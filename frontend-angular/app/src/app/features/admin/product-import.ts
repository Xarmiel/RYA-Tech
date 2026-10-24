import { Component, DestroyRef, HostListener, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, timeout } from 'rxjs';
import { AdminMetadata, AdminRow, AdminService } from './admin.service';
import { ImportProduct, findDuplicate, importKey, parseProducts } from './product-import-model';
import { CatalogReferences } from './catalog-references';
import { technicalIssues } from './product-technical-validation';

type State = 'ready' | 'duplicate' | 'saved' | 'error' | 'uncertain';
interface Row { product: ImportProduct; categoriaId: string; marcaId: string; selected: boolean; state: State; message: string; targetId?:string }

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
    if (file.size > 2 * 1024 * 1024) { this.error.set('El archivo supera 2 MB.'); return; }
    this.setBusy(true);
    try { this.source = await file.text(); this.format = file.name.toLowerCase().endsWith('.csv') ? 'csv' : 'json'; }
    catch { this.error.set('No se pudo leer el archivo.'); }
    finally { this.setBusy(false); }
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
          selected: !duplicate && !product.errors.length, state: duplicate ? 'duplicate' : 'ready',
          message: duplicate ? `Ya existe: ${duplicate.title} (${duplicate.code}). No se sobrescribirá.` : ''};
      }));
    } catch (err: any) { this.error.set(err?.error?.message || err?.message || 'No se pudo comprobar el catálogo. Reintenta la vista previa.'); }
    finally { this.setBusy(false); }
  }
  ready(row: Row): boolean {
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
    const selected = this.rows().filter(row => row.selected && row.state === 'ready');
    return !this.busy() && !this.referencesBusy() && !this.uncertain() && this.confirmed && !!selected.length && selected.every(row => this.ready(row));
  }
  async save() {
    if (!this.canSave()) return;
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
        if(row.product.accion==='actualizar-ficha'||row.product.accion==='agregar-imagenes'){
          const exact=existing.filter(item=>item.code===row.product.sku);
          if(exact.length!==1||exact[0].id!==row.targetId){row.state='error';row.message='El SKU o identificador cambió desde la revisión. No se actualizó.';this.rows.update(rows=>[...rows]);continue;}
        }else if (duplicate) { row.state = 'duplicate'; row.message = `Ya existe: ${duplicate.title}. No se guardó.`; this.rows.update(rows => [...rows]); continue; }
        const p = row.product;
        try {
          const result = await firstValueFrom((p.accion==='actualizar-ficha'
            ?this.api.updateProductTechnical(row.targetId!,p.sku,p.fichaTecnica!)
            :p.accion==='agregar-imagenes'?this.api.addProductImages(row.targetId!,p.sku,p.imagenes)
            :this.api.saveItem('catalogo', {
            nombre: p.nombre, sku: p.sku, descripcion: p.descripcion,
            precio: p.precio, stockInicial: p.stockInicial, categoriaId: row.categoriaId, marcaId: row.marcaId,
            estado: 'draft', requiereEnvio: true,
            ...(p.fichaTecnica?{fichaTecnica:p.fichaTecnica}:{}),
            imagenes: p.imagenes.map((url, posicion) => ({url, textoAlternativo: `${p.nombre} — imagen ${posicion + 1}`, posicion, principal: posicion === 0})),
          }, false)).pipe(timeout(45000)));
          row.state = 'saved'; row.message = p.accion==='actualizar-ficha'?'Ficha comercial actualizada; resto del producto conservado.':p.accion==='agregar-imagenes'?'Galería confirmada; enlaces repetidos omitidos y resto del producto conservado.':'Guardado como borrador en el servidor.'; row.selected = false;
          if(p.accion==='crear')existing.push(result.row); saved++;
        } catch (err: any) {
          const status = err?.status;
          if (!status || status >= 500 || [401, 403, 408, 429].includes(status)) {
            row.state = 'uncertain'; this.uncertain.set(true);
            row.message = 'Respuesta no confirmada. Revisa este SKU en el catálogo antes de intentar otra importación.';
            this.error.set('Lote detenido. Puede existir un guardado aunque no haya llegado su respuesta. No se reintentará automáticamente.');
          } else {
            row.state = 'error'; row.message = err?.error?.message || 'El servidor rechazó este producto. Corrige el archivo y genera otra vista previa.';
            this.error.set('Lote detenido por un rechazo del servidor. Los borradores anteriores se conservan.');
          }
          this.rows.update(rows => [...rows]); break;
        }
        this.rows.update(rows => [...rows]);
      }
    } catch (err: any) { this.error.set(err?.error?.message || err?.message || 'No se pudo volver a verificar el catálogo.'); }
    finally { this.setBusy(false); if (saved) this.saved.emit(); }
  }
  @HostListener('window:beforeunload', ['$event']) beforeUnload(event: BeforeUnloadEvent) {
    if (this.busy() || this.referencesBusy()) { event.preventDefault(); event.returnValue = ''; }
  }
}
