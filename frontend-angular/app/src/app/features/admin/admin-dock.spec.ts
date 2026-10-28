import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AdminDock } from './admin-dock';
import { AdminAccess } from './admin-access';
import { AdminRow, AdminService } from './admin.service';
import { ProductDetailService } from '../product-detail/product-detail.service';

describe('Contextual admin tools', () => {
  let allowed: ReturnType<typeof signal<boolean>>;
  let events: Subject<NavigationEnd>;
  let api: any;
  let products: any;
  let router: any;
  let dock: AdminDock;
  const row = (id: string, extra: Partial<AdminRow> = {}): AdminRow => ({id,code:id,title:id,status:'active',statusText:'Activo',rawData:{stockDisponible:8},...extra});
  beforeEach(() => {
    allowed=signal(true); events=new Subject();
    api={getItems:vi.fn(()=>of({items:[],totalPages:1})),getProductDetail:vi.fn(()=>of({atributos:[]}))};
    products={getBySlug:vi.fn(()=>of({id:'cpu',sku:'CPU-1',name:'CPU real'}))};
    router={url:'/catalogo',events,navigate:vi.fn(),navigateByUrl:vi.fn()};
    TestBed.configureTestingModule({providers:[{provide:Router,useValue:router},{provide:AdminAccess,useValue:{allowed}},
      {provide:AdminService,useValue:api},{provide:ProductDetailService,useValue:products}]});
    dock=TestBed.runInInjectionContext(()=>new AdminDock());
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.useRealTimers();});
  it('detects admin paths and resolves the actual product before offering product actions',async()=>{
    expect(dock.inAdmin).toBe(false);
    events.next(new NavigationEnd(1,'/admin/catalogo','/admin/catalogo'));expect(dock.inAdmin).toBe(true);
    events.next(new NavigationEnd(2,'/producto/cpu-real','/producto/cpu-real'));
    await dock.loadContext();expect(products.getBySlug).toHaveBeenCalledWith('cpu-real');expect(dock.product()?.id).toBe('cpu');
    dock.action('imagenes','cpu');expect(router.navigate).toHaveBeenCalledWith(['/admin','catalogo'],expect.objectContaining({queryParams:expect.objectContaining({accion:'imagenes',producto:'cpu'})}));
  });
  it('counts every page, distinguishes missing stock from zero and preserves independent pending groups',async()=>{
    api.getItems.mockReturnValueOnce(of({items:[row('no-image'),row('draft',{status:'draft',thumbnail:'https://a.test/a',rawData:{stockDisponible:0}})],totalPages:2}))
      .mockReturnValueOnce(of({items:[row('missing-stock',{thumbnail:'https://a.test/b',rawData:{}})],totalPages:2}));
    await dock.refresh();expect(api.getItems).toHaveBeenCalledTimes(2);
    expect(dock.filtered('images').map(r=>r.id)).toEqual(['no-image']);
    expect(dock.filtered('drafts').map(r=>r.id)).toEqual(['draft']);
    expect(dock.filtered('stock').map(r=>r.id)).toEqual(['draft']);
  });
  it('does not display partial counts when a later page fails',async()=>{
    api.getItems.mockReturnValueOnce(of({items:[row('partial')],totalPages:2})).mockReturnValueOnce(throwError(()=>new Error('offline')));
    await dock.refresh();expect(dock.pending()).toBeNull();expect(dock.error()).toContain('Reintenta');expect(dock.busy()).toBe(false);
  });
  it('checks technical pending markers only when requested',async()=>{
    dock.pending.set([row('pending'),row('legacy'),row('unknown')]);
    api.getProductDetail.mockImplementation((id:string)=>of(id==='pending'?{atributos:[{codigo:'CATALOG_TECH_STATUS',valor:'Pendiente de revisión'}]}:id==='legacy'?{fichaTecnica:{origen:'ANTERIOR'}}:{}));
    expect(dock.technicalRows()).toBeNull();await dock.selectTechnical();
    expect(dock.filtered('technical').map(r=>r.id)).toEqual(['pending','legacy']);
    await dock.selectTechnical();expect(api.getProductDetail).toHaveBeenCalledTimes(3);
  });
  it('does not query data without administrative permission and clears cached data on logout',async()=>{
    dock.pending.set([row('private')]);dock.open.set(true);allowed.set(false);TestBed.tick();
    await dock.refresh();expect(api.getItems).not.toHaveBeenCalled();expect(dock.pending()).toBeNull();expect(dock.open()).toBe(false);
  });
  it('moves only after holding, stays onscreen and does not minimize after dragging',()=>{
    vi.useFakeTimers();
    const target={setPointerCapture:vi.fn(),hasPointerCapture:()=>true,releasePointerCapture:vi.fn()};
    const event=(x:number,y:number)=>({button:0,pointerId:1,clientX:x,clientY:y,currentTarget:target} as unknown as PointerEvent);
    dock.start(event(100,100));dock.move(event(200,200));expect(dock.right()).toBe(24);
    vi.advanceTimersByTime(250);dock.move(event(-3000,-3000));dock.end(event(-3000,-3000));dock.minimize();
    expect(dock.minimized()).toBe(false);expect(dock.right()).toBeLessThan(window.innerWidth);expect(dock.bottom()).toBeLessThan(window.innerHeight);
    dock.minimize();expect(dock.minimized()).toBe(true);dock.resetPosition();expect(dock.right()).toBe(24);
  });
});
