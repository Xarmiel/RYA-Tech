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

describe('Phase one service contracts',()=>{
  beforeEach(()=>TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]}));
  afterEach(()=>TestBed.inject(HttpTestingController).verify());
  it('rejects a blank price without issuing a request',()=>{
    let message='';TestBed.inject(AdminService).saveItem('catalogo',{nombre:'Test',sku:'TEST',precio:null},false).subscribe({error:e=>message=e.message});
    expect(message).toContain('precio');TestBed.inject(HttpTestingController).expectNone('/api/v1/admin/productos');
  });
  it('propagates metadata authorization errors without public fallbacks',()=>{
    let status=0;TestBed.inject(AdminService).getMetadata().subscribe({error:e=>status=e.status});
    const http=TestBed.inject(HttpTestingController);http.expectOne('/api/v1/admin/productos/metadata').flush({}, {status:403,statusText:'Forbidden'});
    expect(status).toBe(403);http.expectNone('/api/v1/categorias');
  });
  it('does not fabricate reserved stock or minimum quantities',()=>{
    let row:any;TestBed.inject(AdminService).getItems('inventario').subscribe(r=>row=r.items[0]);
    TestBed.inject(HttpTestingController).expectOne(r=>r.url==='/api/v1/admin/inventario').flush({content:[{existenciaId:'1',productoNombre:'Test',cantidad:7,reservado:2,disponible:5}],totalElements:1,page:0,size:10,totalPages:1});
    expect(row.detail).toBe('Reservado: 2 un. | Total: 7 un.');expect(row.metric).toBe('Físico: 7');
  });
  it('paginates FAQs in the server rather than only searching the first page',()=>{
    let result:any;TestBed.inject(AdminService).getItems('contenido','BIOS',2,5).subscribe(r=>result=r);
    const req=TestBed.inject(HttpTestingController).expectOne(r=>r.url==='/api/v1/ayuda/preguntas');
    expect(req.request.params.get('q')).toBe('BIOS');expect(req.request.params.get('page')).toBe('2');
    req.flush({content:[{id:'q',pregunta:'BIOS',respuesta:'Consulta',categoria:'Ayuda'}],totalElements:11,page:2,size:5,totalPages:3});
    expect(result.total).toBe(11);expect(result.items).toHaveLength(1);
  });
  it('keeps unsupported connected modules read-only',()=>{
    let error='';TestBed.inject(AdminService).saveItem('pedidos',{},false).subscribe({error:e=>error=e.message});
    expect(error).toContain('no está conectada');
  });
});

describe('Phase one admin workspace',()=>{
  let params:BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let requests:Subject<any>[];
  let service:any;
  let data:{design:boolean};
  beforeEach(()=>{
    vi.useFakeTimers();params=new BehaviorSubject(convertToParamMap({section:'catalogo'}));requests=[];data={design:false};
    service={getStats:vi.fn(()=>of({totalProductos:null,pedidosPendientes:null,stockBajo:null,fichasPorVerificar:null,categoriasActivas:null})),
      getMetadata:vi.fn(()=>of({categorias:[],marcas:[]})),getItems:vi.fn(()=>{const request=new Subject();requests.push(request);return request;}),
      getPreviewItems:vi.fn(()=>of({items:[],total:0,page:0,pageSize:10,totalPages:0})),getPreviewStats:vi.fn(()=>of({totalProductos:0})),
      isModuleConnected:(section:string)=>['catalogo','inventario','contenido'].includes(section),saveItem:vi.fn(()=>of({message:'ok'}))};
    TestBed.configureTestingModule({imports:[AdminPage],providers:[provideRouter([]),
      {provide:ActivatedRoute,useValue:{paramMap:params,snapshot:{paramMap:params.value,data}}},
      {provide:AdminService,useValue:service},{provide:NotificationService,useValue:{show:vi.fn()}},
      {provide:PreferencesService,useValue:{theme:signal('dark'),setTheme:vi.fn()}},{provide:SupabaseStorageService,useValue:{}}]});
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.useRealTimers();vi.restoreAllMocks();});
  it('cancels old list requests and does not display stale responses',()=>{
    const fixture=TestBed.createComponent(AdminPage);fixture.detectChanges();const page=fixture.componentInstance as any;
    expect(requests[0].observed).toBe(true);page.setStatusFilter('draft');expect(requests[0].observed).toBe(false);
    requests[0].next({items:[{id:'old'}]});expect(page.items()).toEqual([]);
    requests[1].next({items:[],total:0,totalPages:0,page:0});expect(page.isLoading()).toBe(false);
  });
  it('clears stale rows on errors and supports retry',()=>{
    const fixture=TestBed.createComponent(AdminPage);fixture.detectChanges();const page=fixture.componentInstance as any;
    requests[0].error({error:{message:'Servidor no disponible'}});expect(page.serverError()).toBe('Servidor no disponible');expect(page.items()).toEqual([]);
    page.loadItems();expect(page.serverError()).toBeNull();expect(requests).toHaveLength(2);
  });
  it('drops debounced searches after changing sections',()=>{
    const fixture=TestBed.createComponent(AdminPage);fixture.detectChanges();const page=fixture.componentInstance as any;
    page.onSearchInput('old');params.next(convertToParamMap({section:'inventario'}));fixture.detectChanges();
    const count=requests.length;vi.advanceTimersByTime(300);expect(requests).toHaveLength(count);
  });
  it('never loads administrative data in design mode',()=>{
    data.design=true;const fixture=TestBed.createComponent(AdminPage);fixture.detectChanges();
    expect(service.getStats).not.toHaveBeenCalled();expect(service.getMetadata).not.toHaveBeenCalled();expect(service.getItems).not.toHaveBeenCalled();
    expect(service.getPreviewItems).toHaveBeenCalled();
  });
  it('uses an accessible modal, validates empty prices and confirms unsaved changes',()=>{
    const fixture=TestBed.createComponent(AdminPage);fixture.detectChanges();const page=fixture.componentInstance as any;
    page.openCreateForm();fixture.detectChanges();vi.runOnlyPendingTimers();fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="dialog"]').getAttribute('aria-labelledby')).toBe('admin-form-title');
    expect(page.formData().stockInicial).toBe(0);
    page.formData.update((value:any)=>({...value,nombre:'Test',sku:'TEST',precio:null}));page.submitForm();
    expect(page.formError()).toContain('precio');expect(service.saveItem).not.toHaveBeenCalled();
    const confirm=vi.spyOn(window,'confirm').mockReturnValue(false);expect(page.closeForm()).toBe(false);expect(confirm).toHaveBeenCalled();
    confirm.mockReturnValue(true);expect(page.closeForm()).toBe(true);
  });
});
