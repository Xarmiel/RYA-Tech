import { Component, DestroyRef, HostListener, OnInit, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom, timeout } from 'rxjs';
import { AdminMetadata, AdminReferenceItem, AdminService } from './admin.service';
import { importKey } from './product-import-model';

@Component({
  selector: 'app-catalog-references', imports: [FormsModule],
  templateUrl: './catalog-references.html', styleUrl: './catalog-references.scss',
})
export class CatalogReferences implements OnInit {
  private readonly api = inject(AdminService);
  private destroyed = false;
  readonly disabled = input(false);
  readonly changed = output<AdminMetadata>();
  readonly busyChange = output<boolean>();
  readonly metadata = signal<AdminMetadata>({categorias:[],marcas:[]});
  readonly busy = signal(false);
  readonly loaded = signal(false);
  readonly uncertain = signal(false);
  readonly error = signal('');
  readonly message = signal('');
  kind: 'marca' | 'categoria' = 'marca'; nombre = ''; acronimo = ''; slug = ''; parentId = ''; query = ''; showDeleted = false;
  constructor() { inject(DestroyRef).onDestroy(()=>{this.destroyed=true;}); }
  ngOnInit() { queueMicrotask(()=>{if(!this.destroyed)void this.load();}); }
  private setBusy(value: boolean) { this.busy.set(value); this.busyChange.emit(value); }
  async load() {
    if (this.busy() || this.disabled()) return;
    this.error.set('');this.setBusy(true);
    try {
      const metadata = await firstValueFrom(this.api.getReferences(this.showDeleted).pipe(timeout(30000)));
      if (this.destroyed) return;
      this.metadata.set(metadata);this.loaded.set(true);this.emitActive();
      if (this.uncertain()) this.message.set('Listado actualizado. Comprueba el resultado de la operación. Los cambios permanecen bloqueados para evitar repetirla; abre de nuevo la sección después de revisar.');
    } catch (err: any) { this.loaded.set(false);this.error.set(err?.error?.message || 'No se pudieron consultar marcas y categorías. Reintenta antes de crear.'); }
    finally { this.setBusy(false); }
  }
  changeKind() { this.error.set('');this.message.set('');this.parentId='';this.acronimo='';this.slug=''; }
  activeCategories() { return this.metadata().categorias.filter(item=>!item.eliminada); }
  private emitActive() {
    this.changed.emit({categorias:this.activeCategories(),marcas:this.metadata().marcas.filter(item=>!item.eliminada)});
  }
  filtered(kind: 'marca' | 'categoria') {
    const list = kind === 'marca' ? this.metadata().marcas : this.metadata().categorias;
    return list.filter(item => importKey(item.nombre).includes(importKey(this.query)));
  }
  async create() {
    if (this.busy() || this.disabled() || !this.loaded() || this.uncertain()) return;
    this.error.set('');this.message.set('');
    const nombre=this.nombre.trim(), max=this.kind==='marca'?130:120;
    if (!nombre || nombre.length>max) { this.error.set(`Nombre obligatorio, máximo ${max} caracteres.`);return; }
    if (this.kind==='categoria' && this.acronimo.trim().length>20) {this.error.set('El acrónimo admite hasta 20 caracteres.');return;}
    if (this.kind==='categoria' && this.slug.trim().length>150) {this.error.set('El slug admite hasta 150 caracteres.');return;}
    const list=this.kind==='marca'?this.metadata().marcas:this.metadata().categorias;
    if (list.some(item=>importKey(item.nombre)===importKey(nombre))) {this.error.set('Ya existe ese nombre. Usa el registro existente o restáuralo si está eliminado.');return;}
    if (this.kind==='categoria' && this.parentId && !this.activeCategories().some(item=>item.id===this.parentId)) {this.error.set('Selecciona una categoría superior existente.');return;}
    this.setBusy(true);
    const kind=this.kind;
    try {
      const result=await firstValueFrom((kind==='marca'
        ? this.api.createBrand({nombre})
        : this.api.createCategory({nombre,acronimo:this.acronimo.trim() || undefined,parentId:this.parentId || undefined,...(this.slug.trim()?{slug:this.slug.trim()}:{})})).pipe(timeout(30000)));
      if (this.destroyed) return;
      this.metadata.update(meta=>kind==='marca'?{...meta,marcas:[...meta.marcas,result]}:{...meta,categorias:[...meta.categorias,result]});
      this.nombre='';this.acronimo='';this.slug='';this.parentId='';
      this.message.set(`${kind==='marca'?'Marca':'Categoría'} «${result.nombre}» guardada en el servidor.`);
      this.emitActive();
    } catch (err: any) {
      const status=err?.status;
      if (!status || status>=500 || [401,403,408,429].includes(status)) {
        this.uncertain.set(true);
        this.error.set('No se confirmó la respuesta del alta. Actualiza el listado y comprueba el nombre antes de volver a intentarlo. No se reintentará automáticamente.');
      } else this.error.set(err?.error?.message || 'El servidor rechazó el alta. Revisa los datos.');
    } finally {this.setBusy(false);}
  }
  async changeDeleted(kind: 'marca' | 'categoria', item: AdminReferenceItem) {
    if(this.busy() || this.disabled() || !this.loaded() || this.uncertain())return;
    const restoring=!!item.eliminada;
    if(!window.confirm(restoring?`¿Restaurar «${item.nombre}»?`:`¿Borrar «${item.nombre}»? Se ocultará y podrás restaurarlo. No se borrarán productos; si tiene productos asociados o subcategorías activas, el servidor lo impedirá.`))return;
    this.setBusy(true);this.error.set('');this.message.set('');
    try {
      await firstValueFrom((restoring?this.api.restoreReference(kind,item.id):this.api.deleteReference(kind,item.id)).pipe(timeout(30000)));
      if(this.destroyed)return;
      const key=kind==='marca'?'marcas':'categorias';
      this.metadata.update(meta=>({...meta,[key]:meta[key].map(row=>row.id===item.id?{...row,eliminada:!restoring}:row).filter(row=>this.showDeleted||!row.eliminada)}));
      if(!restoring && this.parentId===item.id)this.parentId='';
      this.emitActive();this.message.set(`«${item.nombre}» ${restoring?'restaurado':'eliminado de forma recuperable'}.`);
    } catch(err:any) {
      const status=err?.status;
      if(!status||status>=500||[401,403,408,429].includes(status)) {
        this.uncertain.set(true);this.error.set('No se confirmó la respuesta. Actualiza el listado antes de repetir la operación.');
      } else this.error.set(err?.error?.message||'El servidor rechazó el cambio. No se quitó del listado local.');
    } finally {this.setBusy(false);}
  }
  @HostListener('window:beforeunload',['$event']) beforeUnload(event: BeforeUnloadEvent) {
    if(this.busy()){event.preventDefault();event.returnValue='';}
  }
}
