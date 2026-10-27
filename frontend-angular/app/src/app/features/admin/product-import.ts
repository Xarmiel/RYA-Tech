import { Component, DestroyRef, ElementRef, HostListener, inject, output, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, timeout } from 'rxjs';
import { AdminMetadata, AdminRow, AdminService } from './admin.service';
import { ImportProduct, findDuplicate, importKey, parseProducts } from './product-import-model';
import { CatalogReferences } from './catalog-references';
import { technicalIssues } from './product-technical-validation';
import { ImportSnapshot, ResolutionField, ResolutionMode, plannedValue, resolutionChanges, resolutionData } from './import-resolution-model';

type State = 'ready' | 'duplicate' | 'saved' | 'error' | 'uncertain';
interface Row { product: ImportProduct; categoriaId: string; marcaId: string; selected: boolean; state: State; message: string; targetId?:string;
  duplicateId?:string; resolution?:ResolutionMode; comparison?:ImportSnapshot; fields?:ResolutionField[] }

@Component({
  selector: 'app-product-import', imports: [FormsModule, CatalogReferences],
  templateUrl: './product-import.html', styleUrl: './product-import.scss',
})
export class ProductImport {
  private readonly api = inject(AdminService);
  private destroyed = false;
  readonly busyChange = output<boolean>();
  readonly saved = output<void>();
  readonly open = signal(false);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly rows = signal<Row[]>([]);
  readonly metadata = signal<AdminMetadata>({categorias: [], marcas: []});
  readonly uncertain = signal(false);
  readonly referencesOpen = signal(false);
  readonly referencesBusy = signal(false);
  readonly issueDialog = viewChild<ElementRef<HTMLDialogElement>>('issueDialog');
  readonly report = signal<{name:string; sku:string; issues:string[]}[]>([]);
  private reportTrigger: HTMLElement | null = null;
  readonly bulkMode = signal<ResolutionMode>('conservar');
  hasDuplicates() {return this.rows().some(row=>!!row.duplicateId&&row.state!=='saved');}
  reviewDuplicates() {this.closeIssues();document.getElementById('import-duplicates')?.focus();}
  changes(row:Row) { return row.comparison ? resolutionChanges(row.comparison,row.product,row.resolution||'conservar') : []; }
  resolutionIssues(row:Row):string[] {
    if (!row.comparison || !row.resolution || row.resolution==='conservar') return ['Compara el producto y elige cómo resolver el duplicado.'];
    const issues:string[]=[];
    if(row.resolution==='actualizar'&&!row.fields?.length)issues.push('Selecciona al menos un campo en la comparación Actual → Propuesto.');
    const ficha=row.product.fichaTecnica;
    if(ficha && (row.resolution==='completar'||row.fields?.includes('fichaTecnica'))){
      const schema=this.metadata().categorias.find(c=>c.slug===row.comparison!.actual.categoriaSlug)?.fichaEsquema;
      issues.push(...technicalIssues(schema,ficha));
    }
    if(row.resolution==='completar'){
      if((plannedValue(row.comparison,row.product,'completar','imagenes') as unknown[]).length>8)issues.push('La galería combinada supera ocho imágenes. Usa actualizar campos seleccionados para revisar la galería que deseas conservar.');
    }
    if(!this.changes(row).some(change=>change.changed&&(row.resolution==='completar'||row.fields?.includes(change.key))))issues.push('No hay cambios pendientes para los campos elegidos. Conserva el actual o selecciona otros campos.');
    return issues;
  }
  async chooseResolution(row:Row, mode:ResolutionMode) {
    if(this.busy()||this.referencesBusy()||this.uncertain())return;
    this.setBusy(true);this.confirmed=false;
    try { await this.prepareResolution(row,mode); }
    finally {this.rows.update(rows=>[...rows]);this.setBusy(false);}
  }
  private async prepareResolution(row:Row,mode:ResolutionMode) {
    row.resolution=mode;row.selected=false;row.fields=[];
    if(mode==='conservar'){row.state='duplicate';row.message='Se conserva el producto actual; esta fila se omite.';return;}
    if(!row.duplicateId){row.message='No hay una coincidencia de SKU exacto. Revisa el producto manualmente; no se combinará por parecido de nombre.';return;}
    try {
      const snapshot=await firstValueFrom(this.api.getImportComparison(row.duplicateId).pipe(timeout(30000)));
      if(snapshot.actual.sku!==row.product.sku)throw new Error('El SKU cambió. Vuelve a validar el lote.');
      row.comparison=snapshot;row.targetId=row.duplicateId;row.state='ready';row.selected=true;
      row.message=mode==='completar'?'Se completan solo datos faltantes; se agregan imágenes sin cambiar la principal.':'Se reemplazan únicamente los campos marcados en la comparación.';
    } catch(err:any){row.state='duplicate';row.message=err?.error?.message||err?.message||'No se pudo cargar la comparación. Reintenta antes de guardar.';}
  }
  async applyResolutionToAll() {
    if(this.busy()||this.referencesBusy()||this.uncertain())return;
    this.confirmed=false;this.setBusy(true);
    try {for(const row of this.rows())if(row.product.accion==='crear'&&row.duplicateId&&row.state!=='saved'){if(this.destroyed)break;await this.prepareResolution(row,this.bulkMode());}}
    finally {this.rows.update(rows=>[...rows]);this.setBusy(false);}
  }
  toggleResolutionField(row:Row,key:ResolutionField,checked:boolean) {
    row.fields=checked?[...new Set([...(row.fields||[]),key])]:(row.fields||[]).filter(field=>field!==key);this.confirmed=false;
  }
  requirements(row: Row): string[] {
    const issues = [...row.product.errors];
    if (['duplicate', 'error', 'uncertain'].includes(row.state) && row.message) issues.push(row.message);
    if (row.state === 'saved') return [];
    if(row.duplicateId){
      if(row.resolution && row.resolution!=='conservar')issues.push(...this.resolutionIssues(row));
      return [...new Set(issues)];
    }
    if (row.product.accion !== 'agregar-imagenes') {
      if (!this.metadata().categorias.some(item => item.id === row.categoriaId)) {
        issues.push(`Falta seleccionar una categoría registrada${row.product.categoria ? ': ' + row.product.categoria : ''}. Créala en «Marcas y categorías» o selecciona una existente.`);
      } else issues.push(...this.technicalErrors(row));
      if (row.product.accion === 'crear' && !this.metadata().marcas.some(item => item.id === row.marcaId)) {
        issues.push(`Falta seleccionar una marca registrada${row.product.marca ? ': ' + row.product.marca : ''}. Créala en «Marcas y categorías» o selecciona una existente.`);
      }
    }
    return [...new Set(issues)];
  }
  showIssues(selectedOnly = false, showIfClear = false) {
    this.report.set(this.rows().filter(row => !selectedOnly || row.selected)
      .map(row => ({name: row.product.nombre || 'Sin nombre', sku: row.product.sku, issues: this.requirements(row)}))
      .filter(row => row.issues.length));
    const general: string[] = [];
    if (this.error()) general.push(this.error());
    if (selectedOnly && !this.rows().some(row => row.selected)) general.push('Selecciona al menos un producto pendiente. Los productos guardados o bloqueados no se vuelven a enviar.');
    if (selectedOnly && !this.confirmed) general.push('Marca la confirmación de revisión antes de aplicar las acciones.');
    if (general.length) this.report.update(items => [{name:'Revisión del lote',sku:'',issues:general}, ...items]);
    if (!this.report().length && showIfClear) this.report.set([{name:'Revisión del lote',sku:'',issues:['No se detectaron requisitos pendientes en la revisión actual. Confirma las acciones seleccionadas para continuar. El servidor volverá a comprobar los datos antes de guardarlos.']}]);
    if (!this.report().length) return;
    const dialog = this.issueDialog()?.nativeElement;
    if (dialog && !dialog.open) {
      this.reportTrigger = document.activeElement instanceof HTMLElement ? document.activeElement : null;
      if (typeof dialog.showModal === 'function') dialog.showModal(); else dialog.setAttribute('open', '');
    }
  }
  closeIssues() {
    const dialog = this.issueDialog()?.nativeElement;
    if (dialog) { if (typeof dialog.close === 'function') dialog.close(); else dialog.removeAttribute('open'); }
    this.reportTrigger?.focus();
  }
  resolveReferences() { this.closeIssues(); this.referencesOpen.set(true); }
  source = ''; format: 'json' | 'csv' = 'json'; confirmed = false;
  constructor() { inject(DestroyRef).onDestroy(() => { this.destroyed = true; }); }
  private setBusy(value: boolean) { this.busy.set(value); this.busyChange.emit(value || this.referencesBusy()); }
  invalidate() { if (!this.busy() && !this.uncertain()) { this.rows.set([]); this.confirmed = false; } }
  referencesChanged(metadata: AdminMetadata) {
    this.metadata.set(metadata);
    this.rows.update(rows=>rows.map(row=>{
      const resolve=(items: typeof metadata.categorias,name:string)=>{
        const matches=items.filter(item=>importKey(item.nombre)===importKey(name)||importKey(item.slug)===importKey(name));
        return matches.length===1?matches[0].id:'';
      };
      return {...row,categoriaId:row.categoriaId||resolve(metadata.categorias,row.product.categoria),marcaId:row.marcaId||resolve(metadata.marcas,row.product.marca)};
    }));
    this.confirmed=false;
  }
  setReferencesBusy(value: boolean) {this.referencesBusy.set(value);this.busyChange.emit(this.busy()||value);}
  async file(event: Event) {
    if (this.busy() || this.referencesBusy() || this.uncertain()) return;
    const input = event.target as HTMLInputElement, file = input.files?.[0];
    input.value = ''; if (!file) return;
    this.invalidate(); this.error.set('');
    if (file.size > 2 * 1024 * 1024) { this.error.set('El archivo supera 2 MB.'); this.showIssues(); return; }
    this.setBusy(true);
    try { this.source = await file.text(); this.format = file.name.toLowerCase().endsWith('.csv') ? 'csv' : 'json'; }
    catch { this.error.set('No se pudo leer el archivo.'); }
    finally { this.setBusy(false); if (this.error()) this.showIssues(); }
  }
  private async identities(): Promise<AdminRow[]> {
    const all: AdminRow[] = [];
    for (let page = 0; page < 200; page++) {
      const result = await firstValueFrom(this.api.getItems('catalogo', '', page, 50, '').pipe(timeout(30000)));
      all.push(...result.items);
      if (page + 1 >= result.totalPages) return all;
    }
    throw new Error('Catálogo demasiado grande para comprobarlo completo. No se habilitó el guardado.');
  }
  async preview() {
    if (this.busy() || this.referencesBusy() || this.uncertain()) return;
    this.rows.set([]); this.error.set(''); this.confirmed = false; this.setBusy(true);
    try {
      const products = parseProducts(this.source, this.format);
      const [metadata, existing] = await Promise.all([
        firstValueFrom(this.api.getMetadata().pipe(timeout(30000))), this.identities(),
      ]);
      if (this.destroyed) return;
      this.metadata.set(metadata);
      const resolve = (items: typeof metadata.categorias, name: string) => {
        const matches = items.filter(item => importKey(item.nombre) === importKey(name) || importKey(item.slug) === importKey(name));
        return matches.length === 1 ? matches[0].id : '';
      };
      this.rows.set(products.map(product => {
        const duplicate = findDuplicate(product, existing);
        const exact=existing.filter(item=>item.code===product.sku);
        if(product.accion==='actualizar-ficha'||product.accion==='agregar-imagenes'){
          const target=exact.length===1?exact[0]:undefined;
          return {product,categoriaId:resolve(metadata.categorias,product.categoria),marcaId:resolve(metadata.marcas,product.marca),targetId:target?.id,
            selected:!!target&&!product.errors.length,state:target?'ready':'error',message:target?(product.accion==='agregar-imagenes'?'Agregar solo imágenes, conservando las existentes y la principal. No cambia ficha, precio, stock ni publicación.':'Actualizar solo ficha; no cambia precio, stock, imágenes ni publicación.'):'No hay un único SKU exacto vigente. No se creará ni se actualizará por parecido de nombre.'};
        }
        return {product, categoriaId: resolve(metadata.categorias, product.categoria), marcaId: resolve(metadata.marcas, product.marca),
          duplicateId:duplicate&&exact.length===1?exact[0].id:undefined,resolution:duplicate?'conservar' as const:undefined,
          selected: !duplicate && !product.errors.length, state: duplicate ? 'duplicate' : 'ready',
          message: duplicate ? `Ya existe: ${duplicate.title} (${duplicate.code}). No se sobrescribirá.` : ''};
      }));
    } catch (err: any) { this.error.set(err?.error?.message || err?.message || 'No se pudo comprobar el catálogo. Reintenta la vista previa.'); }
    finally { this.setBusy(false); this.showIssues(); }
  }
  ready(row: Row): boolean {
    if(row.duplicateId)return row.state==='ready'&&!row.product.errors.length&&!!row.comparison&&row.resolution!=='conservar'&&!this.resolutionIssues(row).length;
    if(row.product.accion==='agregar-imagenes')return row.state==='ready'&&!row.product.errors.length&&!!row.targetId;
    return row.state === 'ready' && !row.product.errors.length
      && (row.product.accion!=='actualizar-ficha'||!!row.targetId)
      && !this.technicalErrors(row).length
      && this.metadata().categorias.some(item => item.id === row.categoriaId)
      && (row.product.accion==='actualizar-ficha'||this.metadata().marcas.some(item => item.id === row.marcaId));
  }
  technicalErrors(row:Row):string[]{
    return row.product.fichaTecnica?technicalIssues(this.metadata().categorias.find(category=>category.id===row.categoriaId)?.fichaEsquema,row.product.fichaTecnica):[];
  }
  selectedCount(): number { return this.rows().filter(row => row.selected && this.ready(row)).length; }
  canSave(): boolean {
    const selected = this.rows().filter(row => row.selected);
    return !this.busy() && !this.referencesBusy() && !this.uncertain() && this.confirmed && !!selected.length && selected.every(row => this.ready(row));
  }
  async save() {
    if (this.busy() || this.referencesBusy() || this.uncertain()) return;
    if (!this.canSave()) { this.showIssues(true); return; }
    this.setBusy(true); this.error.set(''); let saved = 0;
    try {
      // Recheck immediately before writing: a preview can become stale.
      const [existing, metadata] = await Promise.all([
        this.identities(), firstValueFrom(this.api.getMetadata().pipe(timeout(30000))),
      ]);
      this.metadata.set(metadata);
      if (this.rows().some(row => row.selected && row.state === 'ready' && !this.ready(row))) {
        this.error.set('Una categoría o marca seleccionada dejó de estar disponible. Revisa las selecciones antes de guardar.'); return;
      }
      for (const row of this.rows()) {
        if (this.destroyed || !row.selected || !this.ready(row)) continue;
        const duplicate = findDuplicate(row.product, existing);
        if(row.duplicateId||row.product.accion==='actualizar-ficha'||row.product.accion==='agregar-imagenes'){
          const exact=existing.filter(item=>item.code===row.product.sku);
          if(exact.length!==1||exact[0].id!==row.targetId){row.state='error';row.message='El SKU o identificador cambió desde la revisión. No se actualizó.';this.rows.update(rows=>[...rows]);continue;}
        }else if (duplicate) { row.state = 'duplicate'; row.message = `Ya existe: ${duplicate.title}. No se guardó.`; this.rows.update(rows => [...rows]); continue; }
        const p = row.product;
        try {
          const result = await firstValueFrom((row.duplicateId
            ? this.api.resolveImport(row.targetId!,{sku:p.sku,revision:row.comparison!.revision,modo:row.resolution,
                campos:row.resolution==='completar'?[]:row.fields,datos:resolutionData(p)})
            :p.accion==='actualizar-ficha'
            ?(p.camposFicha?this.api.updateProductTechnical(row.targetId!,p.sku,p.fichaTecnica!,p.camposFicha):this.api.updateProductTechnical(row.targetId!,p.sku,p.fichaTecnica!))
            :p.accion==='agregar-imagenes'?this.api.addProductImages(row.targetId!,p.sku,p.imagenes)
            :this.api.saveItem('catalogo', {
            nombre: p.nombre, sku: p.sku, descripcion: p.descripcion,
            slug: p.slug || undefined,
            precio: p.precio, stockInicial: p.stockInicial,
            costoReferencia: p.costoReferencia,
            categoriaId: row.categoriaId, marcaId: row.marcaId,
            estado: p.estado || 'draft',
            requiereSerie: p.requiereSerie ?? false,
            requiereEnvio: p.requiereEnvio ?? true,
            ...(p.fichaTecnica?{fichaTecnica:p.fichaTecnica}:{}),
            imagenes: p.imagenes.map((url, posicion) => ({url, textoAlternativo: `${p.nombre} — imagen ${posicion + 1}`, posicion, principal: posicion === 0})),
          }, false)).pipe(timeout(45000)));
          row.state = 'saved'; row.message = row.duplicateId?'Cambios confirmados. Identificador, stock, publicación y relaciones conservados.':p.accion==='actualizar-ficha'?'Ficha comercial actualizada; resto del producto conservado.':p.accion==='agregar-imagenes'?'Galería confirmada; enlaces repetidos omitidos y resto del producto conservado.':'Guardado como borrador en el servidor.'; row.selected = false;
          if(p.accion==='crear'&&!row.duplicateId&&'row' in result)existing.push(result.row); saved++;
        } catch (err: any) {
          const status = err?.status;
          if (!status || status >= 500 || [401, 403, 408, 429].includes(status)) {
            row.state = 'uncertain'; this.uncertain.set(true);
            const reason = status === 401 ? 'La sesión venció. Vuelve a iniciar sesión.'
              : status === 403 ? 'No tienes permiso para esta operación. Comprueba el acceso de administrador.'
              : status === 429 ? 'Se alcanzó el límite de solicitudes. Espera antes de volver a consultar.'
              : status >= 500 ? 'El servidor no pudo completar la respuesta.' : 'Se perdió la conexión o se agotó el tiempo de espera.';
            row.message = `${reason} Respuesta no confirmada. Revisa este SKU en el catálogo antes de intentar otra importación.`;
            this.error.set('Lote detenido. Puede existir un guardado aunque no haya llegado su respuesta. No se reintentará automáticamente.');
          } else {
            const fields = err?.error?.validationErrors;
            const details = fields && typeof fields === 'object' ? Object.entries(fields)
              .filter(([, value]) => typeof value === 'string').map(([field, value]) => `${field}: ${value}`).join(' · ') : '';
            row.state = 'error'; row.message = [err?.error?.message || 'El servidor rechazó este producto. Corrige el archivo y genera otra vista previa.', details].filter(Boolean).join(' ');
            this.error.set('Lote detenido por un rechazo del servidor. Los borradores anteriores se conservan.');
          }
          this.rows.update(rows => [...rows]); break;
        }
        this.rows.update(rows => [...rows]);
      }
    } catch (err: any) { this.error.set(err?.error?.message || err?.message || 'No se pudo volver a verificar el catálogo.'); }
    finally { this.setBusy(false); if (saved) this.saved.emit(); this.showIssues(); }
  }
  @HostListener('window:beforeunload', ['$event']) beforeUnload(event: BeforeUnloadEvent) {
    if (this.busy() || this.referencesBusy()) { event.preventDefault(); event.returnValue = ''; }
  }
}
