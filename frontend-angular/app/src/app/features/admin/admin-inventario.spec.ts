import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError, Subject } from 'rxjs';
import { AdminService, AjusteStockPayload, InventarioItem, MovimientoInventario } from './admin.service';
import { AdminPage } from './admin-page';
import { ActivatedRoute } from '@angular/router';
import { NotificationService } from '../../core/notifications/notification.service';
import { PreferencesService } from '../../core/preferences/preferences.service';
import { ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('Admin Inventario Service', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
  });

  it('getInventoryReal consulta /api/v1/admin/inventario con parámetros y mapea stock real', () => {
    const service = TestBed.inject(AdminService);
    const http = TestBed.inject(HttpTestingController);

    let result: any;
    service.getInventoryReal('Ryzen', 0, 10, 'bajo_stock').subscribe(res => (result = res));

    const req = http.expectOne(r => r.url === '/api/v1/admin/inventario');
    expect(req.request.params.get('busqueda')).toBe('Ryzen');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('10');
    expect(req.request.params.get('alerta')).toBe('bajo_stock');

    const mockItem: InventarioItem = {
      version: 0,
      existenciaId: 'ex-01',
      productoId: 'prod-01',
      productoNombre: 'AMD Ryzen 7 7800X3D',
      productoSku: 'AMD-7800X3D',
      almacenId: 'alm-01',
      almacenNombre: 'Almacén Principal',
      almacenCodigo: 'ALM-01',
      cantidad: 10,
      reservado: 2,
      disponible: 8,
      estadoAlerta: 'DISPONIBLE',
      imagenPrincipal: 'https://images.ryatech.com/cpu.webp',
    };

    req.flush({
      content: [mockItem],
      totalElements: 1,
      page: 0,
      size: 10,
      totalPages: 1,
    });

    expect(result.items).toHaveLength(1);
    const row = result.items[0];
    expect(row.title).toBe('AMD Ryzen 7 7800X3D');
    expect(row.code).toBe('AMD-7800X3D');
    expect(row.subtitle).toBe('8 un. disponibles');
    expect(row.tag).toBe('Almacén Principal');
    expect(row.detail).toBe('Reservado: 2 un. | Total: 10 un.');
    expect(row.metric).toBe('Físico: 10');
    expect(row.status).toBe('active');
  });

  it('getInventoryReal clasifica como alert si disponible es 0 o menor igual a 3', () => {
    const service = TestBed.inject(AdminService);
    const http = TestBed.inject(HttpTestingController);

    let result: any;
    service.getInventoryReal().subscribe(res => (result = res));

    const req = http.expectOne(r => r.url === '/api/v1/admin/inventario');
    req.flush({
      content: [
        { existenciaId: '1', productoNombre: 'Agotado', cantidad: 5, reservado: 5, disponible: 0 },
        { existenciaId: '2', productoNombre: 'Bajo', cantidad: 3, reservado: 0, disponible: 3 },
      ],
      totalElements: 2,
      page: 0,
      size: 10,
      totalPages: 1,
    });

    expect(result.items[0].status).toBe('alert');
    expect(result.items[0].statusText).toBe('Agotado');
    expect(result.items[1].status).toBe('alert');
    expect(result.items[1].statusText).toBe('Bajo stock');
  });

  it('getAlmacenes consulta el listado de almacenes activos', () => {
    const service = TestBed.inject(AdminService);
    const http = TestBed.inject(HttpTestingController);

    let almacenes: any[] = [];
    service.getAlmacenes().subscribe(res => (almacenes = res));

    const req = http.expectOne('/api/v1/admin/inventario/almacenes');
    req.flush([{ id: 'alm-01', nombre: 'Almacén Central', codigo: 'ALM-01', activo: true }]);

    expect(almacenes).toHaveLength(1);
    expect(almacenes[0].nombre).toBe('Almacén Central');
  });

  it('ajustarStock valida campos obligatorios antes de enviar la petición', () => {
    const service = TestBed.inject(AdminService);
    const http = TestBed.inject(HttpTestingController);

    // Sin nota
    let errorMsg = '';
    service.ajustarStock({
      operacionId: 'op-1', versionEsperada: 0,
      productoId: 'p-1',
      almacenId: 'a-1',
      tipo: 'ENTRADA',
      cantidad: 5,
      nota: '   ',
    }).subscribe({
      error: err => (errorMsg = err.message),
    });

    expect(errorMsg).toContain('motivo o justificación del ajuste es obligatorio');
    http.expectNone('/api/v1/admin/inventario/ajuste');
  });

  it('ajustarStock envía la petición POST con éxito', () => {
    const service = TestBed.inject(AdminService);
    const http = TestBed.inject(HttpTestingController);

    const payload: AjusteStockPayload = {
      operacionId: 'op-1', versionEsperada: 0,
      productoId: 'p-1',
      almacenId: 'a-1',
      tipo: 'ENTRADA',
      cantidad: 5,
      nota: 'Factura F001-123',
    };

    let response: any;
    service.ajustarStock(payload).subscribe(res => (response = res));

    const req = http.expectOne('/api/v1/admin/inventario/ajuste');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);

    req.flush({
      existenciaId: 'ex-1',
      productoId: 'p-1',
      cantidad: 15,
      reservado: 0,
      disponible: 15,
    });

    expect(response.disponible).toBe(15);
  });

  it('getKardex consulta el historial de movimientos de la existencia', () => {
    const service = TestBed.inject(AdminService);
    const http = TestBed.inject(HttpTestingController);

    let kardex: any[] = [];
    service.getKardex('ex-123').subscribe(res => (kardex = res.content));

    const req = http.expectOne(r => r.url === '/api/v1/admin/inventario/ex-123/kardex');
    req.flush({content: [
      { id: 'm-1', tipo: 'ENTRADA', cantidad: 10, nota: 'Lote inicial' },
      { id: 'm-2', tipo: 'SALIDA', cantidad: 2, nota: 'Merma' },
    ], totalPages: 1});

    expect(kardex).toHaveLength(2);
    expect(kardex[0].tipo).toBe('ENTRADA');
    expect(kardex[1].tipo).toBe('SALIDA');
  });
});

describe('Admin Inventario Page UI & Modals', () => {
  let fixture: ComponentFixture<AdminPage>;
  let component: AdminPage;
  let adminServiceMock: any;
  let notificationsMock: any;

  beforeEach(async () => {
    adminServiceMock = {
      isModuleConnected: vi.fn(() => true),
      getStats: vi.fn(() => of({ totalProductos: 10, pedidosPendientes: 2, stockBajo: 1, fichasPorVerificar: null, categoriasActivas: 5 })),
      getMetadata: vi.fn(() => of({ categorias: [], marcas: [] })),
      getAlmacenes: vi.fn(() => of([
        { id: 'alm-1', nombre: 'Almacén Principal', codigo: 'ALM-01', activo: true },
        { id: 'alm-2', nombre: 'Almacén Secundario', codigo: 'ALM-02', activo: true },
      ])),
      getInventoryReal: vi.fn(() => of({
        items: [
          {
            id: 'ex-1',
            code: 'SKU-CPU-01',
            title: 'Intel Core i7-14700K',
            subtitle: '10 un. disponibles',
            tag: 'Almacén Principal',
            detail: 'Reservado: 2 un. | Total: 12 un.',
            metric: 'Físico: 12',
            status: 'active',
            statusText: 'Disponible',
            rawData: {
              existenciaId: 'ex-1',
              productoId: 'prod-cpu-1',
              productoNombre: 'Intel Core i7-14700K',
              productoSku: 'SKU-CPU-01',
              almacenId: 'alm-1',
              almacenNombre: 'Almacén Principal',
              cantidad: 12,
              reservado: 2,
              disponible: 10,
            },
          },
        ],
        total: 1,
        page: 0,
        pageSize: 10,
        totalPages: 1,
      })),
      consultarStock: vi.fn(() => of({existenciaId: 'ex-1', productoId:'prod-cpu-1', almacenId:'alm-1', version:0, cantidad:12, reservado:2, disponible:10})),
      getItems: vi.fn(() => of({ items: [], total: 0, page: 0, pageSize: 10, totalPages: 0 })),
      getPreviewItems: vi.fn(() => of({ items: [], total: 0, page: 0, pageSize: 10, totalPages: 0 })),
      ajustarStock: vi.fn(() => of({
        existenciaId: 'ex-1',
        disponible: 15,
        cantidad: 17,
      })),
      getKardex: vi.fn(() => of({content: [
        { id: 'k-1', tipo: 'ENTRADA', cantidad: 5, nota: 'Recepción lote', registradoAt: new Date().toISOString() },
      ], totalPages:1})),
    };

    notificationsMock = {
      show: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => 'inventario' }, data: {} },
            paramMap: of({ get: (k: string) => (k === 'section' ? 'inventario' : null) }),
            queryParamMap: of({get: () => null}),
          },
        },
        { provide: AdminService, useValue: adminServiceMock },
        { provide: NotificationService, useValue: notificationsMock },
        PreferencesService,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('carga existencias reales de inventario al inicializar en sección inventario', () => {
    expect(adminServiceMock.getInventoryReal).toHaveBeenCalled();
    const rows = (component as any).items();
    expect(rows).toHaveLength(1);
    expect(rows[0].title).toBe('Intel Core i7-14700K');
  });

  it('abre el modal de ajuste de stock con el producto seleccionado de la fila', () => {
    const row = (component as any).items()[0];
    (component as any).openStockModal(row);
    fixture.detectChanges();

    expect((component as any).showStockModal()).toBe(true);
    expect((component as any).selectedStockRow()).toBe(row);
    expect((component as any).stockForm().productoId).toBe('prod-cpu-1');
    expect((component as any).stockForm().almacenId).toBe('alm-1');
  });

  it('calcula la proyección de stock para ENTRADA, SALIDA y AJUSTE', () => {
    const row = (component as any).items()[0];
    (component as any).openStockModal(row);

    // ENTRADA +5 (disponible 10 -> 15, total 12 -> 17)
    (component as any).stockForm.update((f: any) => ({ ...f, tipo: 'ENTRADA', cantidad: 5 }));
    expect((component as any).getStockProjectionText()).toBe('15 un. disponibles (17 un. totales)');

    // SALIDA 3 (disponible 10 -> 7, total 12 -> 9)
    (component as any).stockForm.update((f: any) => ({ ...f, tipo: 'SALIDA', cantidad: 3 }));
    expect((component as any).getStockProjectionText()).toBe('7 un. disponibles (9 un. totales)');

    // SALIDA que excede disponible (11 > 10)
    (component as any).stockForm.update((f: any) => ({ ...f, tipo: 'SALIDA', cantidad: 11 }));
    expect((component as any).getStockProjectionText()).toContain('Inválido: excede unidades disponibles');

    // AJUSTE físico a 20 (reservado 2 -> disponible 18, total 20)
    (component as any).stockForm.update((f: any) => ({ ...f, tipo: 'AJUSTE', cantidad: 20 }));
    expect((component as any).getStockProjectionText()).toBe('18 un. disponibles (20 un. totales)');

    // AJUSTE físico menor a reservado (1 < 2)
    (component as any).stockForm.update((f: any) => ({ ...f, tipo: 'AJUSTE', cantidad: 1 }));
    expect((component as any).getStockProjectionText()).toContain('Inválido: menor a unidades reservadas');
  });

  it('envía ajuste de stock al servidor y recarga el listado', () => {
    const row = (component as any).items()[0];
    (component as any).openStockModal(row);

    (component as any).stockForm.set({
      productoId: 'prod-cpu-1',
      almacenId: 'alm-1',
      tipo: 'ENTRADA',
      cantidad: 5,
      nota: 'Ingreso lote verificado',
    });

    (component as any).submitStockAdjustment();
    fixture.detectChanges();

    expect(adminServiceMock.ajustarStock).toHaveBeenCalledWith(expect.objectContaining({
      operacionId: expect.any(String), versionEsperada: 0, existenciaId:'ex-1',
      productoId: 'prod-cpu-1',
      almacenId: 'alm-1',
      tipo: 'ENTRADA',
      cantidad: 5,
      nota: 'Ingreso lote verificado',
    }));

    expect((component as any).showStockModal()).toBe(false);
    expect(notificationsMock.show).toHaveBeenCalledWith(expect.stringContaining('Movimiento registrado'));
  });

  it('abre el modal de Kardex y muestra los movimientos del servidor', () => {
    const row = (component as any).items()[0];
    (component as any).openKardexModal(row);
    fixture.detectChanges();

    expect((component as any).showKardexModal()).toBe(true);
    expect(adminServiceMock.getKardex).toHaveBeenCalledWith('ex-1', 0);
    expect((component as any).kardexItems()).toHaveLength(1);
    expect((component as any).kardexItems()[0].tipo).toBe('ENTRADA');
  });

  it('no consulta ni guarda inventario real en modo diseño', () => {
    Object.defineProperty(component, 'design', {get: () => true});
    adminServiceMock.getAlmacenes.mockClear();
    const page = component as any;
    page.openStockModal(); page.loadAlmacenes(); page.refreshStock();
    page.submitStockAdjustment(); page.openKardexModal(page.items()[0]);
    expect(adminServiceMock.getAlmacenes).not.toHaveBeenCalled();
    expect(adminServiceMock.consultarStock).not.toHaveBeenCalled();
    expect(adminServiceMock.ajustarStock).not.toHaveBeenCalled();
    expect(adminServiceMock.getKardex).not.toHaveBeenCalled();
  });

  it('espera la existencia del almacén nuevo sin reutilizar las cantidades anteriores', () => {
    const page=component as any;
    page.openStockModal(page.items()[0]);
    const pending = new Subject<any>();
    adminServiceMock.consultarStock.mockReturnValue(pending);
    page.onStockAlmacenChange('alm-2');
    expect(adminServiceMock.consultarStock).toHaveBeenLastCalledWith('prod-cpu-1','alm-2',undefined);
    expect(page.stockCurrent()).toBeNull();
    expect(page.getStockProjectionText()).toContain('Consultando');
    expect(page.isStockFormValid()).toBe(false);
    pending.next({existenciaId:'ex-2',version:3,cantidad:30,reservado:5,disponible:25});pending.complete();
    page.stockForm.update((f:any)=>({...f,cantidad:2,nota:'Test'}));
    expect(page.getStockProjectionText()).toBe('27 un. disponibles (32 un. totales)');
  });

  it('busca productos fuera de las filas de existencias y admite su primera entrada', () => {
    const page=component as any;
    adminServiceMock.getItems.mockReturnValue(of({items:[{id:'new-prod',title:'Nuevo sin stock'}],page:0,totalPages:2}));
    page.openStockModal();
    expect(page.stockForm().productoId).toBe('');
    expect(adminServiceMock.getItems).toHaveBeenCalledWith('catalogo','',0,20,'');
    expect(page.stockProducts()[0].id).toBe('new-prod');
    adminServiceMock.consultarStock.mockReturnValue(of({existenciaId:null,version:-1,cantidad:0,reservado:0,disponible:0}));
    page.onStockProductChange('new-prod');
    page.stockForm.update((f:any)=>({...f,cantidad:4,nota:'Ingreso inicial'}));
    page.submitStockAdjustment();
    expect(adminServiceMock.ajustarStock).toHaveBeenCalledWith(expect.objectContaining({productoId:'new-prod',versionEsperada:-1,existenciaId:undefined}));
  });

  it('conserva la misma operación al reintentar una respuesta incierta', () => {
    const page=component as any;
    page.openStockModal(page.items()[0]);
    page.stockForm.update((f:any)=>({...f,cantidad:2,nota:'Recepción'}));
    adminServiceMock.ajustarStock.mockReturnValueOnce(throwError(()=>({status:0})));
    page.submitStockAdjustment();
    const first=adminServiceMock.ajustarStock.mock.calls[0][0];
    expect(page.stockUncertain()).toBe(true);
    expect(page.canLeave()).toBe(false);
    page.refreshStock();
    page.submitStockAdjustment();
    expect(adminServiceMock.ajustarStock.mock.calls[1][0]).toEqual(first);
    expect(page.stockUncertain()).toBe(false);
  });

  it('un conflicto obliga a consultar de nuevo y muestra errores de almacenes', () => {
    const page=component as any;
    adminServiceMock.getAlmacenes.mockReturnValue(throwError(()=>({status:500})));
    page.almacenes.set([]);
    page.openStockModal(page.items()[0]);
    expect(page.almacenesError()).toContain('Reintenta');
    page.stockForm.update((f:any)=>({...f,cantidad:2,nota:'Recepción'}));
    adminServiceMock.ajustarStock.mockReturnValue(throwError(()=>({status:409,error:{message:'El stock cambió'}})));
    page.submitStockAdjustment();
    expect(page.stockCurrent()).toBeNull();
    expect(page.isStockFormValid()).toBe(false);
  });

  it('recupera la identidad de una operación pendiente después de recargar la pestaña', () => {
    const page=component as any;
    const id='10000000-1000-4000-8000-000000000001';
    const userSpy=vi.spyOn(page.auth,'user').mockReturnValue({id});
    const key=`rya.inventory.pending.${id}`;
    sessionStorage.removeItem(key);
    try {
      page.openStockModal(page.items()[0]);
      page.stockForm.update((f:any)=>({...f,cantidad:2,nota:'Recepción persistida'}));
      adminServiceMock.ajustarStock.mockReturnValueOnce(throwError(()=>({status:0})));
      page.submitStockAdjustment();
      const first=adminServiceMock.ajustarStock.mock.calls[0][0];
      expect(sessionStorage.getItem(key)).toContain(first.operacionId);
      page.stockUncertain.set(false);page.stockAttempt=null;
      page.openStockModal();
      expect(page.stockUncertain()).toBe(true);
      page.submitStockAdjustment();
      expect(adminServiceMock.ajustarStock.mock.calls[1][0]).toEqual(first);
      expect(sessionStorage.getItem(key)).toBeNull();
    } finally { sessionStorage.removeItem(key);userSpy.mockRestore(); }
  });

  it('cancela el historial anterior al abrir otra fila', () => {
    const page=component as any;
    const old = new Subject<any>();
    adminServiceMock.getKardex.mockReturnValueOnce(old);
    page.openKardexModal(page.items()[0]);
    page.openKardexModal({...page.items()[0],id:'ex-2',rawData:{existenciaId:'ex-2'}});
    old.next({content:[{id:'old'}],totalPages:1});
    expect(page.kardexItems()[0].id).toBe('k-1');
  });
});
