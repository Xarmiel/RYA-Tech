import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminPage } from './admin-page';
import { AdminService } from './admin.service';
import { NotificationService } from '../../core/notifications/notification.service';
import { PreferencesService } from '../../core/preferences/preferences.service';
import { SupabaseStorageService } from '../../core/storage/supabase-storage.service';

describe('Phase two product and reference forms', () => {
  let service: any;
  let data: {design:boolean};
  let reference: Subject<any>;
  let action: Subject<any>;
  const category = {id:'cat',nombre:'Procesadores',slug:'procesadores'};
  const brand = {id:'brand',nombre:'AMD',slug:'amd'};
  beforeEach(() => {
    vi.useFakeTimers(); data={design:false}; reference=new Subject(); action=new Subject();
    const params = new BehaviorSubject(convertToParamMap({section:'catalogo'}));
    service={getStats:vi.fn(()=>of({totalProductos:0})),getMetadata:vi.fn(()=>of({categorias:[category],marcas:[brand]})),
      getItems:vi.fn(()=>of({items:[],total:0,page:0,totalPages:0})),isModuleConnected:()=>true,
      getPreviewStats:vi.fn(()=>of({totalProductos:0})),getPreviewItems:vi.fn(()=>of({items:[],total:0,page:0,totalPages:0})),
      createCategory:vi.fn(()=>reference),createBrand:vi.fn(()=>reference),saveItem:vi.fn(()=>of({message:'ok'})),
      publishProduct:vi.fn(()=>action),deactivateProduct:vi.fn(()=>action),getProductDetail:vi.fn(()=>reference)};
    TestBed.configureTestingModule({imports:[AdminPage],providers:[provideRouter([]),
      {provide:ActivatedRoute,useValue:{paramMap:params,queryParamMap:of(convertToParamMap({})),snapshot:{paramMap:params.value,data}}},
      {provide:AdminService,useValue:service},{provide:NotificationService,useValue:{show:vi.fn()}},
      {provide:PreferencesService,useValue:{theme:signal('dark'),setTheme:vi.fn()}},{provide:SupabaseStorageService,useValue:{}}]});
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.useRealTimers();vi.restoreAllMocks();});
  function open() {
    const fixture=TestBed.createComponent(AdminPage); fixture.detectChanges();
    const page=fixture.componentInstance as any; page.openCreateForm();fixture.detectChanges();
    vi.runOnlyPendingTimers();fixture.detectChanges();return {page,fixture};
  }
  it('requires an explicit category and starts unpublished without an arbitrary brand',()=>{
    const {page,fixture}=open(); expect(page.formData()).toMatchObject({categoriaId:'',marcaId:'',estado:'draft'});
    expect(fixture.nativeElement.querySelector('[role="dialog"]').closest('.admin-workspace')).not.toBeNull();
    page.formData.update((d:any)=>({...d,nombre:'CPU',sku:'CPU-1',precio:20}));page.submitForm();
    expect(page.formError()).toContain('categoría');expect(service.saveItem).not.toHaveBeenCalled();
  });
  it('preserves a manually edited slug when the product name changes',()=>{
    const {page}=open();page.onNombreChange('Procesador rápido');expect(page.formData().slug).toBe('procesador-rapido');
    page.onSlugChange('mi-url');page.onNombreChange('Otro nombre');expect(page.formData().slug).toBe('mi-url');
  });
  it('validates the technical child before saving the complete product',()=>{
    const {page,fixture}=open();
    page.categories.set([{...category,fichaEsquema:{tipo:'cpu',nombre:'Procesador',campos:[
      {key:'tdp_w',label:'TDP',type:'number',unit:'W',integer:false,positive:false}
    ]}}]);
    page.formData.update((d:any)=>({...d,nombre:'CPU',sku:'CPU-1',precio:20,categoriaId:'cat',
      fichaTecnica:{tipo:'cpu',valores:{tdp_w:-1}}}));fixture.detectChanges();
    page.submitForm();expect(service.saveItem).not.toHaveBeenCalled();
    expect(page.formError()).toContain('ficha técnica');
    page.setTechnical({tipo:'cpu',fuenteUrl:'https://example.com/specs',valores:{tdp_w:65}});fixture.detectChanges();
    page.submitForm();expect(service.saveItem).toHaveBeenCalledWith('catalogo',expect.objectContaining({
      fichaTecnica:{tipo:'cpu',fuenteUrl:'https://example.com/specs',valores:{tdp_w:65}}
    }),false);
  });
  it('rejects duplicate category names and invalid parent IDs before a request',()=>{
    const {page}=open();page.newCategoryName.set('  Procesadores ');page.submitNewCategory();
    expect(page.categoryError()).toContain('ya existe');expect(service.createCategory).not.toHaveBeenCalled();
    page.newCategoryName.set('Pantallas');page.newCategoryParentId.set('missing');page.submitNewCategory();
    expect(page.categoryError()).toContain('superior');expect(service.createCategory).not.toHaveBeenCalled();
  });
  it('blocks repeated category submissions and closing until the server responds',()=>{
    const {page}=open();page.newCategoryName.set('Monitores');page.newCategoryParentId.set('cat');
    page.submitNewCategory();page.submitNewCategory();expect(service.createCategory).toHaveBeenCalledTimes(1);
    expect(service.createCategory).toHaveBeenCalledWith(expect.objectContaining({parentId:'cat',slug:'monitores'}));
    expect(page.closeForm()).toBe(false);expect(page.canLeave()).toBe(false);
    reference.next({id:'new',nombre:'Monitores',slug:'monitores'});reference.complete();
    expect(page.savingCategory()).toBe(false);expect(page.formData().categoriaId).toBe('new');
  });
  it('preserves failed category input and supports retry',()=>{
    const {page}=open();page.newCategoryName.set('Monitores');page.submitNewCategory();
    reference.error({error:{message:'Conflicto de categoría'}});expect(page.savingCategory()).toBe(false);
    expect(page.categoryError()).toBe('Conflicto de categoría');expect(page.newCategoryName()).toBe('Monitores');
    service.createCategory.mockReturnValue(of({id:'new',nombre:'Monitores',slug:'monitores'}));page.submitNewCategory();
    expect(page.formData().categoriaId).toBe('new');
  });
  it('rejects duplicate brands and insecure logo URLs',()=>{
    const {page}=open();page.newBrandName.set('amd');page.submitNewBrand();expect(page.brandError()).toContain('ya existe');
    page.newBrandName.set('Nueva');page.newBrandLogoUrl.set('http://example.com/logo.png');page.submitNewBrand();
    expect(page.brandError()).toContain('HTTPS');expect(service.createBrand).not.toHaveBeenCalled();
  });
  it('blocks repeated brand submissions and preserves errors for retry',()=>{
    const {page}=open();page.newBrandName.set('Nueva');page.submitNewBrand();page.submitNewBrand();
    expect(service.createBrand).toHaveBeenCalledTimes(1);expect(page.savingBrand()).toBe(true);
    reference.error({error:{message:'No disponible'}});expect(page.savingBrand()).toBe(false);expect(page.brandError()).toBe('No disponible');
  });
  it('creates design categories locally without backend calls',()=>{
    data.design=true;const {page}=open();page.newCategoryName.set('Vídeo y edición');page.submitNewCategory();
    expect(service.createCategory).not.toHaveBeenCalled();expect(page.categories()[0].slug).toBe('video-y-edicion');
    expect(page.formData().categoriaId).toMatch(/^local-cat-/);
  });
  it('preserves shipping and serial flags from the full administrative detail',()=>{
    const {page,fixture}=open();page.openEditForm({id:'1',title:'Software',code:'SW',rawData:{},status:'draft'});fixture.detectChanges();
    vi.runOnlyPendingTimers(); reference.next({nombre:'Software',sku:'SW',precioBase:10,categoria:'Procesadores',marca:'AMD',requiereSerie:true,requiereEnvio:false});
    expect(page.formData()).toMatchObject({requiereSerie:true,requiereEnvio:false,categoriaId:'cat',marcaId:'brand'});
  });
  it('restores design form values when reopening a locally saved product',()=>{
    data.design=true;const {page}=open();page.openEditForm({id:'1',title:'Software',code:'SW',status:'draft',
      rawData:{precio:50,stockInicial:0,requiereSerie:true,requiereEnvio:false}});
    expect(page.formData()).toMatchObject({precio:50,stockInicial:0,requiereSerie:true,requiereEnvio:false});
  });
  it('locks product actions during requests, releases on error and does not mutate the row optimistically',()=>{
    const {page}=open();const row={id:'1',title:'CPU',status:'draft'};vi.spyOn(window,'confirm').mockReturnValue(true);
    page.publishProduct(row);page.publishProduct(row);expect(service.publishProduct).toHaveBeenCalledTimes(1);
    expect(page.pendingRows().has('1')).toBe(true);expect(row.status).toBe('draft');
    action.error({error:{message:'Falló'}});expect(page.pendingRows().size).toBe(0);
  });
  it('shows deactivate rather than publish for an active product with a stock alert',()=>{
    const {page,fixture}=open();page.showFormModal.set(false);fixture.detectChanges();
    page.items.set([{id:'1',title:'CPU',code:'SKU',status:'alert',statusText:'Sin stock'}]);fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[aria-label="Desactivar producto"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[aria-label="Publicar producto"]')).toBeNull();
  });
});

describe('Phase two product payload safety',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]}));
  afterEach(()=>TestBed.inject(HttpTestingController).verify());
  it('uses an image-only endpoint without sending commercial fields or specifications',()=>{
    TestBed.inject(AdminService).addProductImages('1','CPU-1',['https://example.com/cpu.webp']).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne('/api/v1/admin/productos/1/imagenes');
    expect(request.request.method).toBe('PATCH');expect(request.request.body).toEqual({sku:'CPU-1',imagenes:[{url:'https://example.com/cpu.webp'}]});request.flush({id:'1'});
  });
  it('sends technical-only updates without price, stock, publication or image fields',()=>{
    const ficha={tipo:'cpu',valores:{igpu:false}};TestBed.inject(AdminService).updateProductTechnical('1','CPU-1',ficha).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne('/api/v1/admin/productos/1/ficha-tecnica');
    expect(request.request.method).toBe('PATCH');expect(request.request.body).toEqual({sku:'CPU-1',fichaTecnica:ficha});request.flush({id:'1'});
  });
  it('does not silently create a new product when an edit has no ID',()=>{
    let message='';TestBed.inject(AdminService).saveItem('catalogo',{precio:10},true).subscribe({error:e=>message=e.message});
    expect(message).toContain('identificador');TestBed.inject(HttpTestingController).expectNone('/api/v1/admin/productos');
  });
  it('sends technical numbers and false without coercion and preserves omission on ordinary edits',()=>{
    const service=TestBed.inject(AdminService),http=TestBed.inject(HttpTestingController);
    const fichaTecnica={tipo:'cpu',fuenteUrl:'https://example.com/specs',valores:{tdp_w:65,igpu:false}};
    service.saveItem('catalogo',{id:'1',nombre:'CPU',sku:'CPU',precio:20,fichaTecnica},true).subscribe();
    const technical=http.expectOne('/api/v1/admin/productos/1');
    expect(technical.request.body.fichaTecnica).toEqual(fichaTecnica);technical.flush({id:'1'});
    service.saveItem('catalogo',{id:'1',nombre:'CPU',sku:'CPU',precio:21},true).subscribe();
    const commercial=http.expectOne('/api/v1/admin/productos/1');
    expect(Object.hasOwn(commercial.request.body,'fichaTecnica')).toBe(false);commercial.flush({id:'1'});
  });
  it('roundtrips shipping and serial flags without leaking real saves into design data',()=>{
    const service=TestBed.inject(AdminService);service.saveItem('catalogo',{id:'1',nombre:'Licencia',sku:'SW',precio:10,requiereSerie:true,requiereEnvio:false},true).subscribe();
    const request=TestBed.inject(HttpTestingController).expectOne('/api/v1/admin/productos/1');
    expect(request.request.body).toMatchObject({requiereSerie:true,requiereEnvio:false});
    expect(request.request.body.stockInicial).toBeUndefined();
    request.flush({id:'1',nombre:'Licencia',sku:'SW',precioBase:10});
    let count=-1;service.getPreviewItems('catalogo').subscribe(result=>count=result.total);expect(count).toBe(0);
  });
  it('preserves zero stock and does not invent a brand in preview rows',()=>{
    let row:any;TestBed.inject(AdminService).saveItemLocally('catalogo',{nombre:'Test',sku:'TEST',precio:0,stockInicial:0},false).subscribe(result=>row=result.row);
    expect(row.detail).toBe('0 unidades');expect(row.subtitle).toBe('');
  });
});
