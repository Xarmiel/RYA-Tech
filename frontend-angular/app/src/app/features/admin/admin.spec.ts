import { describe, expect, it, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AdminAccess, adminGuard } from './admin-access';
import { AuthService } from '../../core/auth/auth.service';
import { SupabaseStorageService } from '../../core/storage/supabase-storage.service';
import { TECHNICAL_MODEL } from './technical-model';
import { DraftImage, imageUrl, ProductMedia } from './product-media';
import { TechnicalEditor } from './technical-editor';
import { TechnicalEntry, technicalEntryGuard } from './technical-entry';
import { AdminDock } from './admin-dock';
import { AdminService } from './admin.service';

describe('Admin model and gallery',()=>{
  it('normalizes HTTPS and rejects unsafe image schemes',()=>{
    expect(imageUrl('https://example.com/image.png#preview')).toBe('https://example.com/image.png');
    for(const value of ['javascript:alert(1)','http://example.com/a','https://user:pass@example.com/a'])expect(()=>imageUrl(value)).toThrow();
  });
  it('preserves false and null without inventing compatibility',()=>{
    const field=TECHNICAL_MODEL.types['cpu'].fields.find(f=>f.id==='igpu')!;
    expect(TECHNICAL_MODEL.valueOf(field,'false')).toBe(false);
    expect(TECHNICAL_MODEL.valueOf(field,'')).toBeNull();
    expect(TECHNICAL_MODEL.types['peripheral'].defaultUse).toBe('catalog');
  });
  it('recognizes the requested shortcut, not normal typing',()=>{
    const event={ctrlKey:true,shiftKey:true,altKey:false,metaKey:false,repeat:false,code:'Minus',key:'_'};
    expect(TECHNICAL_MODEL.shortcut(event)).toBe(true);
    expect(TECHNICAL_MODEL.shortcut({...event,ctrlKey:false})).toBe(false);
  });
  it('limits combined images to eight, rejects duplicates and maintains ordering',()=>{
    const gallery=TestBed.runInInjectionContext(()=>new ProductMedia());
    for(let i=0;i<8;i++){gallery.url=`https://example.com/${i}.png`;gallery.addUrl();}
    gallery.url='https://example.com/extra.png';gallery.addUrl();expect(gallery.images()).toHaveLength(8);
    gallery.move(1,-1);expect(gallery.snapshot()[0].url).toContain('/1.png');expect(gallery.snapshot()[0].primary).toBe(true);
    gallery.remove(gallery.images()[0]);gallery.url='https://example.com/0.png';gallery.addUrl();expect(gallery.images()).toHaveLength(7);
    gallery.ngOnDestroy();
  });

  it('manages gallery state: setImages, makePrimary, and payload generation', () => {
    const gallery = TestBed.runInInjectionContext(() => new ProductMedia());
    gallery.setImages([
      { url: 'https://example.com/side.png', alt: 'Vista lateral', order: 2, primary: false },
      { url: 'https://example.com/front.png', alt: 'Vista frontal', order: 1, primary: true }
    ]);

    expect(gallery.images()).toHaveLength(2);
    expect(gallery.images()[0].url).toBe('https://example.com/front.png');
    expect(gallery.images()[0].alt).toBe('Vista frontal');

    // Make second image primary
    gallery.makePrimary(1);
    expect(gallery.images()[0].url).toBe('https://example.com/side.png');

    const payload = gallery.getPayload();
    expect(payload).toHaveLength(2);
    expect(payload[0].principal).toBe(true);
    expect(payload[0].posicion).toBe(0);
    expect(payload[1].principal).toBe(false);
    expect(payload[1].posicion).toBe(1);

    gallery.ngOnDestroy();
  });

  it('tracks Supabase upload progress, error and retry states', () => {
    const mockStorage = {
      upload: vi.fn().mockReturnValue(of(
        { progress: 50 },
        { progress: 100, url: 'https://rasudbiaxwyoojxxdutz.supabase.co/storage/v1/object/public/productos/test.png' }
      ))
    };

    TestBed.configureTestingModule({
      providers: [
        ProductMedia,
        { provide: SupabaseStorageService, useValue: mockStorage }
      ]
    });

    const gallery = TestBed.inject(ProductMedia);
    const file = new File(['fake-content'], 'cooler.png', { type: 'image/png' });
    const draftItem: DraftImage = {
      id: 1,
      url: 'blob:fake',
      alt: 'cooler',
      file,
      uploading: true,
      progress: 10
    };

    gallery.images.set([draftItem]);
    gallery.startUpload(draftItem);

    expect(mockStorage.upload).toHaveBeenCalledWith(file);
    expect(draftItem.uploading).toBe(false);
    expect(draftItem.url).toContain('https://rasudbiaxwyoojxxdutz.supabase.co');

    // Test error and retry
    mockStorage.upload.mockReturnValue(of({ progress: 0, error: 'Permiso denegado en Supabase Storage.' }));
    gallery.startUpload(draftItem);
    expect(draftItem.error).toBe('Permiso denegado en Supabase Storage.');

    gallery.ngOnDestroy();
  });

  it('blocks saving with hasPendingOrFailedUploads when upload fails or is blob URL', () => {
    const gallery = TestBed.runInInjectionContext(() => new ProductMedia());
    const draft1: DraftImage = {
      id: 1,
      url: 'blob:fake-preview',
      alt: 'Test',
      uploading: false,
      error: 'Error de conexión'
    };
    gallery.images.set([draft1]);

    expect(gallery.hasPendingOrFailedUploads()).toBe(true);
    // getPayload must exclude blob URLs and failed images
    expect(gallery.getPayload()).toHaveLength(0);

    // When valid HTTPS url without error
    gallery.images.set([{
      id: 2,
      url: 'https://example.com/ok.png',
      alt: 'OK',
      uploading: false
    }]);
    expect(gallery.hasPendingOrFailedUploads()).toBe(false);
    expect(gallery.getPayload()).toHaveLength(1);

    gallery.ngOnDestroy();
  });

  it('cleans up staged uncommitted uploads on cancel', () => {
    const mockStorage = {
      upload: vi.fn().mockReturnValue(of({
        path: 'productos/temp1.png',
        url: 'https://rasudbiaxwyoojxxdutz.supabase.co/storage/v1/object/public/productos/temp1.png'
      })),
      delete: vi.fn().mockReturnValue(of(null))
    };

    TestBed.configureTestingModule({
      providers: [
        ProductMedia,
        { provide: SupabaseStorageService, useValue: mockStorage }
      ]
    });

    const gallery = TestBed.inject(ProductMedia);
    const file = new File(['123'], 'image.png', { type: 'image/png' });
    const draft: DraftImage = { id: 1, url: 'blob:1', alt: 'Test', file };
    gallery.images.set([draft]);
    gallery.startUpload(draft);

    expect(mockStorage.upload).toHaveBeenCalled();

    // Cancel modal / cleanup staged
    gallery.cleanupStagedUploads();
    expect(mockStorage.delete).toHaveBeenCalledWith('productos/temp1.png');

    gallery.ngOnDestroy();
  });
});

describe('Technical editor',()=>{
  beforeEach(()=>TestBed.configureTestingModule({imports:[TechnicalEditor]}));
  it('keeps per-type specifications and exports only the current type',()=>{
    const fixture=TestBed.createComponent(TechnicalEditor);fixture.detectChanges();
    const editor=fixture.componentInstance;
    editor.product={name:'Local test',sku:'TEST',brand:'Test'};editor.values['socket']='AM5';
    editor.type='peripheral';editor.changeType();editor.review();
    const draft=JSON.parse(editor.reviewed());expect(draft.requestedUse).toBe('catalog');expect(draft.technical.socket).toBeUndefined();expect(draft.verificationStatus).toBe('pending');expect(draft.product.price).toBeNull();
    editor.type='cpu';editor.changeType();expect(editor.values['socket']).toBe('AM5');
  });
  it('rejects fractional stock and self-parent categories',()=>{
    const fixture=TestBed.createComponent(TechnicalEditor);fixture.detectChanges();const editor=fixture.componentInstance;
    editor.product={name:'Local test',sku:'TEST',brand:'Test',stock:'1.5'};editor.review();expect(editor.reviewed()).toBe('');expect(editor.error()).toContain('entero');
    editor.product['stock']='1';editor.category='Procesadores';editor.categoryParent='procesadores';editor.review();expect(editor.reviewed()).toBe('');expect(editor.error()).toContain('superior');
  });
  it('propagates isDesignMode to ProductMedia component', () => {
    const fixture = TestBed.createComponent(TechnicalEditor);
    fixture.componentRef.setInput('isDesignMode', true);
    fixture.detectChanges();
    expect(fixture.componentInstance.media.isDesignMode()).toBe(true);
  });
});

describe('Admin access',()=>{
  const user=signal<{id:string}|null>(null);
  beforeEach(()=>{
    user.set(null);TestBed.configureTestingModule({providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting(),{provide:AuthService,useValue:{user,initialize:vi.fn().mockResolvedValue(null)}}]});
  });
  it('redirects anonymous visits to login with returnUrl',async()=>{
    const result=await TestBed.runInInjectionContext(()=>adminGuard({} as never,{url:'/admin/carga-tecnica'} as never));
    expect(TestBed.inject(Router).serializeUrl(result as never)).toContain('/login?returnUrl=');
  });
  it('blocks direct technical editor visits until the shortcut enables entry',()=>{
    const route={paramMap:{get:()=> 'carga-tecnica'}};
    const state={url:'/admin/diseno/carga-tecnica'};
    const blocked=TestBed.runInInjectionContext(()=>technicalEntryGuard(route as never,state as never));
    expect(TestBed.inject(Router).serializeUrl(blocked as never)).toBe('/admin/diseno');
    TestBed.inject(TechnicalEntry).open();
    expect(TestBed.runInInjectionContext(()=>technicalEntryGuard(route as never,state as never))).toBe(true);
    expect(new TechnicalEntry().openedWithShortcut()).toBe(false);
  });
  it('opens the reserved editor from the shortcut without creating a public link',()=>{
    const fixture=TestBed.createComponent(AdminDock);fixture.detectChanges();
    const router=TestBed.inject(Router);const navigate=vi.spyOn(router,'navigateByUrl').mockResolvedValue(true);
    const event=new KeyboardEvent('keydown',{ctrlKey:true,shiftKey:true,code:'Minus',key:'_',cancelable:true});
    fixture.componentInstance.shortcut(event);
    expect(event.defaultPrevented).toBe(true);
    expect(TestBed.inject(TechnicalEntry).openedWithShortcut()).toBe(true);
    expect(navigate).toHaveBeenCalledWith('/admin/carga-tecnica');
    expect(fixture.nativeElement.querySelector('a[href$="/carga-tecnica"]')).toBeNull();
  });
  it('allows only server confirmed access and clears it on logout',async()=>{
    const access=TestBed.inject(AdminAccess);TestBed.tick();user.set({id:'user-a'});TestBed.tick();
    TestBed.inject(HttpTestingController).expectOne('/api/v1/admin/acceso').flush({allowed:true});
    await Promise.resolve();await Promise.resolve();expect(access.allowed()).toBe(true);
    user.set(null);expect(access.allowed()).toBe(false);
  });
});

describe('Admin service and listings', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ]
    });
  });

  it('fetches real catalog items with fallback and formats rows properly', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    let result: any = null;
    service.getItems('catalogo', '', 0, 10).subscribe(res => result = res);

    const req = httpMock.expectOne(r => r.url.includes('/api/v1/admin/productos'));
    req.flush({
      content: [
        { id: 'p1', sku: 'AMD-9900X', nombre: 'AMD Ryzen 9 9900X', marca: 'AMD', categoria: 'Procesadores', precioBase: 2450.00, stockDisponible: 12 }
      ],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1
    });

    expect(result).not.toBeNull();
    expect(result.items.length).toBe(1);
    expect(result.items[0].code).toBe('AMD-9900X');
    expect(result.items[0].title).toBe('AMD Ryzen 9 9900X');
    expect(result.items[0].metric).toBe('S/ 2450.00');
    expect(result.items[0].status).toBe('active');
  });

  it('filters rows by search term and status correctly', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    let result: any = null;
    service.getItems('catalogo', '4070', 0, 10, 'alert').subscribe(res => result = res);

    const req = httpMock.expectOne(r => r.url.includes('/api/v1/admin/productos'));
    req.flush({
      content: [
        { id: 'p2', sku: 'RTX-4070S', nombre: 'GeForce RTX 4070 Super', marca: 'ASUS', categoria: 'Tarjetas', precioBase: 3190.00, stockDisponible: 2 }
      ],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1
    });

    expect(result.items.length).toBe(1);
    expect(result.items[0].status).toBe('alert');
  });

  it('filters catalog items by category correctly in request params', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    let result: any = null;
    service.getItems('catalogo', '', 0, 10, 'active', 'cat-uuid-123').subscribe(res => result = res);

    const req = httpMock.expectOne(r =>
      r.url.includes('/api/v1/admin/productos') &&
      r.params.get('categoriaId') === 'cat-uuid-123' &&
      r.params.get('estado') === 'active'
    );
    req.flush({
      content: [
        { id: 'p3', sku: 'RYZEN-5', nombre: 'AMD Ryzen 5 7600', marca: 'AMD', categoria: 'Procesadores', precioBase: 899.00, stockDisponible: 18 }
      ],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1
    });

    expect(result.items.length).toBe(1);
    expect(result.items[0].code).toBe('RYZEN-5');
    expect(result.items[0].tag).toBe('Procesadores');
  });

  it('saves new items and confirms server operation with feedback message', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);
    let saveResult: any = null;

    service.saveItem('catalogo', {
      nombre: 'Nuevo Cooler AIO 360mm',
      sku: 'COOLER-360',
      marca: 'NZXT',
      categoria: 'Refrigeración',
      precio: 650.00,
      estado: 'active'
    }, false).subscribe(res => saveResult = res);

    const postReq = httpMock.expectOne('/api/v1/admin/productos');
    expect(postReq.request.method).toBe('POST');
    postReq.flush({
      id: 'p-new',
      sku: 'COOLER-360',
      nombre: 'Nuevo Cooler AIO 360mm',
      precioBase: 650.00
    });

    expect(saveResult).not.toBeNull();
    expect(saveResult.success).toBe(true);
    expect(saveResult.message).toContain('creado y guardado con éxito');
    expect(saveResult.row.code).toBe('COOLER-360');
  });

  it('toggles item status between active and inactive with confirmation', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);
    let toggleResult: any = null;

    service.toggleStatus('catalogo', '1').subscribe(res => toggleResult = res);

    const toggleReq = httpMock.expectOne('/api/v1/admin/productos/1/desactivar');
    expect(toggleReq.request.method).toBe('PATCH');
    toggleReq.flush({});

    expect(toggleResult).not.toBeNull();
    expect(toggleResult.success).toBe(true);
    expect(toggleResult.message).toContain('El estado del registro fue cambiado');
  });

  it('propagates error when saveItem is rejected by server (e.g. 403 or 400)', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);
    let errorReceived: any = null;
    let successReceived: any = null;

    service.saveItem('catalogo', {
      nombre: 'Unauthorized product',
      sku: 'NO-AUTH',
      precio: 100.00
    }, false).subscribe({
      next: res => successReceived = res,
      error: err => errorReceived = err
    });

    const postReq = httpMock.expectOne('/api/v1/admin/productos');
    postReq.flush({ message: 'Permiso denegado' }, { status: 403, statusText: 'Forbidden' });

    expect(successReceived).toBeNull();
    expect(errorReceived).not.toBeNull();
    expect(errorReceived.status).toBe(403);
  });

  it('does not alter stock when updating product commercial info', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    service.saveItem('catalogo', {
      id: 'prod-456',
      nombre: 'AMD Ryzen 7 7800X3D',
      sku: 'AMD-7800X3D',
      precio: 1890.00,
      stockInicial: 50
    }, true).subscribe();

    const putReq = httpMock.expectOne('/api/v1/admin/productos/prod-456');
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.stockInicial).toBeUndefined();
    putReq.flush({ id: 'prod-456', sku: 'AMD-7800X3D', nombre: 'AMD Ryzen 7 7800X3D' });
  });

  it('does not invent reference cost when not provided', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    service.saveItem('catalogo', {
      id: 'prod-789',
      nombre: 'Teclado Mecánico',
      sku: 'KB-01',
      precio: 200.00
    }, true).subscribe();

    const putReq = httpMock.expectOne('/api/v1/admin/productos/prod-789');
    expect(putReq.request.body.costoReferencia).toBeUndefined();
    putReq.flush({ id: 'prod-789', sku: 'KB-01' });
  });

  it('manages product lifecycle: publishing, deactivating, saving drafts and deleting', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    // 1. Publicar
    let pubRes: any = null;
    service.publishProduct('prod-123').subscribe(res => pubRes = res);
    const pubReq = httpMock.expectOne('/api/v1/admin/productos/prod-123/publicar');
    expect(pubReq.request.method).toBe('PATCH');
    pubReq.flush({});
    expect(pubRes.success).toBe(true);
    expect(pubRes.message).toContain('publicado');

    // 2. Desactivar
    let deactRes: any = null;
    service.deactivateProduct('prod-123').subscribe(res => deactRes = res);
    const deactReq = httpMock.expectOne('/api/v1/admin/productos/prod-123/desactivar');
    expect(deactReq.request.method).toBe('PATCH');
    deactReq.flush({});
    expect(deactRes.success).toBe(true);
    expect(deactRes.message).toContain('desactivado');

    // 3. Borrador
    let draftRes: any = null;
    service.saveDraftProduct('prod-123').subscribe(res => draftRes = res);
    const draftReq = httpMock.expectOne('/api/v1/admin/productos/prod-123/borrador');
    expect(draftReq.request.method).toBe('PATCH');
    draftReq.flush({});
    expect(draftRes.success).toBe(true);
    expect(draftRes.message).toContain('borrador');

    // 4. Eliminar
    let delRes: any = null;
    service.deleteProduct('prod-123').subscribe(res => delRes = res);
    const delReq = httpMock.expectOne('/api/v1/admin/productos/prod-123');
    expect(delReq.request.method).toBe('DELETE');
    delReq.flush(null);
    expect(delRes.success).toBe(true);
    expect(delRes.message).toContain('eliminado');
  });

  it('retrieves categories and brands metadata with fallback support', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    let metaResult: any = null;
    service.getMetadata().subscribe(res => metaResult = res);

    const metaReq = httpMock.expectOne('/api/v1/admin/productos/metadata');
    metaReq.flush({
      categorias: [{ id: 'c1', nombre: 'Procesadores', slug: 'procesadores' }],
      marcas: [{ id: 'm1', nombre: 'AMD', slug: 'amd' }]
    });

    expect(metaResult).not.toBeNull();
    expect(metaResult.categorias.length).toBe(1);
    expect(metaResult.categorias[0].nombre).toBe('Procesadores');
    expect(metaResult.marcas[0].nombre).toBe('AMD');
  });

  it('creates new categories and brands with server sync and fallback support', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    // 1. Crear categoría
    let createdCat: any = null;
    service.createCategory({ nombre: 'Monitores', acronimo: 'MON' }).subscribe(res => createdCat = res);
    const catReq = httpMock.expectOne('/api/v1/admin/productos/categorias');
    expect(catReq.request.method).toBe('POST');
    catReq.flush({ id: 'cat-mon', nombre: 'Monitores', slug: 'monitores' });

    expect(createdCat).not.toBeNull();
    expect(createdCat.id).toBe('cat-mon');
    expect(createdCat.nombre).toBe('Monitores');

    // 2. Crear marca
    let createdBrand: any = null;
    service.createBrand({ nombre: 'Logitech G', logoUrl: 'https://example.com/logo.png' }).subscribe(res => createdBrand = res);
    const brandReq = httpMock.expectOne('/api/v1/admin/productos/marcas');
    expect(brandReq.request.method).toBe('POST');
    brandReq.flush({ id: 'marca-logi', nombre: 'Logitech G', slug: 'logitech-g' });

    expect(createdBrand).not.toBeNull();
    expect(createdBrand.id).toBe('marca-logi');
    expect(createdBrand.nombre).toBe('Logitech G');
  });

  it('saves product with full gallery images and retrieves product detail', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    // 1. Probar guardado con galería de imágenes
    let saveResult: any = null;
    service.saveItem('catalogo', {
      nombre: 'Case Gamer Lian Li O11 Dynamic',
      sku: 'CASE-O11D',
      precio: 620.00,
      imagenes: [
        { url: 'https://example.com/o11-front.png', textoAlternativo: 'Frente', posicion: 0, principal: true },
        { url: 'https://example.com/o11-side.png', textoAlternativo: 'Lateral', posicion: 1, principal: false }
      ]
    }, false).subscribe(res => saveResult = res);

    const postReq = httpMock.expectOne('/api/v1/admin/productos');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.imagenes).toHaveLength(2);
    expect(postReq.request.body.imagenes[0].principal).toBe(true);
    expect(postReq.request.body.imagenPrincipalUrl).toBe('https://example.com/o11-front.png');
    postReq.flush({ id: 'prod-123', sku: 'CASE-O11D' });

    expect(saveResult.success).toBe(true);

    // 2. Probar obtención de detalle para recuperar galería
    let detailResult: any = null;
    service.getProductDetail('prod-123').subscribe(res => detailResult = res);

    const detailReq = httpMock.expectOne('/api/v1/admin/productos/prod-123');
    expect(detailReq.request.method).toBe('GET');
    detailReq.flush({
      id: 'prod-123',
      nombre: 'Case Gamer Lian Li O11 Dynamic',
      imagenes: [
        { id: 'img-1', url: 'https://example.com/o11-front.png', posicion: 0, principal: true },
        { id: 'img-2', url: 'https://example.com/o11-side.png', posicion: 1, principal: false }
      ]
    });

    expect(detailResult).not.toBeNull();
    expect(detailResult.imagenes).toHaveLength(2);
    expect(detailResult.imagenes[0].url).toBe('https://example.com/o11-front.png');
  });

  it('retrieves stats with real counts and nulls for uncalculated modules', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    let stats: any = null;
    service.getStats().subscribe(res => stats = res);

    const reqProd = httpMock.expectOne('/api/v1/admin/productos?size=1');
    const reqAlert = httpMock.expectOne('/api/v1/admin/productos?estado=alert&size=1');
    const reqCat = httpMock.expectOne('/api/v1/categorias');
    const reqOrders = httpMock.expectOne('/api/v1/admin/operacion/pedidos?estado=PENDIENTE_PAGO');

    reqOrders.flush({total:7});
    reqProd.flush({ totalElements: 42 });
    reqAlert.flush({ totalElements: 5 });
    reqCat.flush([{ id: 'c1' }, { id: 'c2' }]);

    expect(stats.totalProductos).toBe(42);
    expect(stats.stockBajo).toBe(5);
    expect(stats.categoriasActivas).toBe(2);
    expect(stats.pedidosPendientes).toBe(7);
    expect(stats.fichasPorVerificar).toBeNull();
  });

  it('sends parentId when creating category with parent', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    service.createCategory({ nombre: 'Monitores OLED', acronimo: 'OLED', parentId: 'parent-cat-123' }).subscribe();
    const req = httpMock.expectOne('/api/v1/admin/productos/categorias');
    expect(req.request.body.parentId).toBe('parent-cat-123');
    req.flush({ id: 'cat-oled', nombre: 'Monitores OLED', slug: 'monitores-oled' });
  });

  it('does not contaminate localStore when server rejects saveItem', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    service.saveItem('catalogo', { nombre: 'Rejected', sku: 'REJ-1', precio: 100 }, false).subscribe({
      error: () => {}
    });

    const req = httpMock.expectOne('/api/v1/admin/productos');
    req.flush({ message: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    const localList = (service as any).localStore.get('catalogo') || [];
    expect(localList.some((it: any) => it.code === 'REJ-1')).toBe(false);
  });

  it('connects clientes module, retrieves users, and updates roles correctly', async () => {
    const service = TestBed.inject(AdminService);
    const httpMock = TestBed.inject(HttpTestingController);

    expect(service.isModuleConnected('clientes')).toBe(true);

    let result: any = null;
    service.getItems('clientes', 'juan', 0, 10, 'active').subscribe(res => result = res);

    const reqUsers = httpMock.expectOne(req =>
      req.url === '/api/v1/admin/usuarios' &&
      req.params.get('busqueda') === 'juan' &&
      req.params.get('rol') === 'admin'
    );
    expect(reqUsers.request.method).toBe('GET');

    reqUsers.flush({
      content: [
        {
          id: 'user-1',
          email: 'admin@gmail.com',
          nombre: 'Administrador Principal',
          telefono: '+51 987654321',
          rol: 'admin',
          totalPedidos: 3,
          ultimaCompra: '2026-10-25T20:00:00Z',
          creadoEn: '2026-10-01T10:00:00Z'
        }
      ],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1
    });

    expect(result).not.toBeNull();
    expect(result.items).toHaveLength(1);
    expect(result.items[0].code).toBe('ADMIN');
    expect(result.items[0].title).toBe('Administrador Principal');
    expect(result.items[0].subtitle).toBe('+51 987654321');
    expect(result.items[0].status).toBe('active');
    expect(result.items[0].statusText).toBe('Administrador');
    expect(result.items[0].metric).toBe('3 pedidos');

    // Test role update
    let updateSuccess = false;
    service.updateUserRole('user-1', 'user').subscribe(() => updateSuccess = true);

    const reqRole = httpMock.expectOne('/api/v1/admin/usuarios/user-1/rol');
    expect(reqRole.request.method).toBe('PATCH');
    expect(reqRole.request.body).toEqual({ rol: 'user' });
    reqRole.flush({ id: 'user-1', rol: 'user' });

    expect(updateSuccess).toBe(true);
  });
});
