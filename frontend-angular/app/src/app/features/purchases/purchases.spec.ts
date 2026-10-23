import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Purchases } from './purchases';
import { Order, PurchasesService } from './purchases.service';

registerLocaleData(localeEsPe);
describe('Historial de compras', () => {
  const params = new BehaviorSubject(convertToParamMap({}));
  const query = new BehaviorSubject(convertToParamMap({}));
  const api = { list: vi.fn(), detail: vi.fn() };
  const order: Order = { id: 'pedido-1', codigo: 'RYA-001', estado: 'PAGADO', moneda: 'PEN',
    subtotal: 390, descuento: 10, envio: 15, total: 395, fecha: '2026-10-20T12:00:00-05:00', metodoPago: 'Transferencia',
    productos: [{ id: 'item-1', productoId: null, nombre: 'CPU comprado', sku: 'SKU-HISTORICO', cantidad: 2,
      precioUnitario: 200, descuentoUnitario: 5, subtotal: 390, slug: null, imagen: null }] };
  beforeEach(() => {
    params.next(convertToParamMap({})); query.next(convertToParamMap({}));
    api.list.mockReset().mockReturnValue(of({ content: [order], page: 0, size: 12, totalElements: 13, totalPages: 2 }));
    api.detail.mockReset().mockReturnValue(of(order));
    TestBed.configureTestingModule({ imports: [Purchases], providers: [provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: params, queryParamMap: query } },
      { provide: PurchasesService, useValue: api },
    ] });
  });

  it('muestra código, estado, fecha, total y productos del pedido', () => {
    const fixture = TestBed.createComponent(Purchases); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.order-card')?.textContent).toContain('RYA-001');
    expect(element.querySelector('.order-status')?.textContent).toBe('Pagado');
    expect(element.querySelector('time')?.getAttribute('datetime')).toBe(order.fecha);
    expect(element.querySelector('.order-preview')?.textContent).toContain('CPU comprado');
    expect(element.querySelector('footer strong')?.textContent).toContain('395.00');
    expect(element.querySelector('[aria-label="Ver pedido RYA-001"]')?.getAttribute('href')).toBe('/compras/pedido-1');
  });

  it('pagina usando la URL y conserva la página al abrir un pedido', () => {
    query.next(convertToParamMap({ pagina: '2' }));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Purchases); fixture.detectChanges();
    expect(api.list).toHaveBeenCalledWith(1);
    expect((fixture.nativeElement as HTMLElement).querySelector('[aria-label="Ver pedido RYA-001"]')?.getAttribute('href')).toBe('/compras/pedido-1?pagina=2');
    [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find(item => item.textContent?.trim() === 'Anterior')!.click();
    expect(navigate).toHaveBeenCalledWith(['/compras'], { queryParams: {} });
  });

  it('muestra el detalle histórico aunque el producto ya no exista', () => {
    params.next(convertToParamMap({ id: order.id }));
    const fixture = TestBed.createComponent(Purchases); fixture.detectChanges();
    expect(api.detail).toHaveBeenCalledWith(order.id);
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.order-item')?.textContent).toContain('SKU-HISTORICO');
    expect(element.querySelector('.order-item')?.textContent).toContain('200.00');
    expect(element.querySelector('.order-item')?.textContent).toContain('ya no está publicado');
    expect(element.querySelector('.item-info a')).toBeNull();
    expect(element.querySelector('.grand-total')?.textContent).toContain('395.00');
    expect(element.querySelector('.payment')?.textContent).toContain('Transferencia');
  });

  it('distingue la cuenta sin compras de un fallo de conexión', () => {
    api.list.mockReturnValue(of({ content: [], page: 0, size: 12, totalElements: 0, totalPages: 0 }));
    const fixture = TestBed.createComponent(Purchases); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aún no tienes compras');
    api.list.mockReturnValue(throwError(() => ({ status: 503 })));
    query.next(convertToParamMap({ pagina: '2' })); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar tus compras');
    expect(fixture.nativeElement.textContent).not.toContain('Aún no tienes compras');
  });

  it('trata pedidos no encontrados y sesiones vencidas sin exponer datos anteriores', () => {
    params.next(convertToParamMap({ id: order.id }));
    api.detail.mockReturnValue(throwError(() => ({ status: 404 })));
    const fixture = TestBed.createComponent(Purchases); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Pedido no encontrado');
    api.detail.mockReturnValue(throwError(() => ({ status: 401 })));
    params.next(convertToParamMap({ id: 'otro' })); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Vuelve a iniciar sesión');
    expect((fixture.nativeElement as HTMLElement).querySelector('.order-detail')).toBeNull();
  });
});
