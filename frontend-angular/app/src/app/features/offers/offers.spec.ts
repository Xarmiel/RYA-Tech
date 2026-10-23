import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Offers } from './offers';
import { OfferPage, OffersService } from './offers.service';
import { FavoritesService } from '../../core/favorites/favorites.service';

registerLocaleData(localeEsPe);
describe('Página de ofertas', () => {
  const params = new BehaviorSubject(convertToParamMap({}));
  const api = { list: vi.fn() };
  const response: OfferPage = {
    content: [{ productoId: 'cpu', nombre: 'CPU en oferta', slug: 'cpu', marca: 'AMD', categoria: 'Procesadores', imagen: null,
      stockDisponible: 2, precioAnterior: 100, precioActual: 80, descuentoPorcentaje: 20, ahorro: 20,
      promocion: 'Campaña vigente', inicioAt: null, finAt: '2026-12-31T23:59:00-05:00' }],
    page: 0, size: 12, totalElements: 1, totalPages: 1, categorias: [], marcas: [],
  };
  beforeEach(() => {
    params.next(convertToParamMap({}));
    api.list.mockReset().mockReturnValue(of(response));
    TestBed.configureTestingModule({ imports: [Offers], providers: [provideRouter([]),
      { provide: ActivatedRoute, useValue: { queryParamMap: params } },
      { provide: OffersService, useValue: api },
      { provide: FavoritesService, useValue: { has: () => false, toggle: vi.fn() } },
    ] });
  });

  it('muestra precio anterior, precio actual, descuento, vigencia y enlace al producto', async () => {
    const fixture = TestBed.createComponent(Offers);
    fixture.detectChanges(); await fixture.whenStable();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('del')?.textContent).toContain('100.00');
    expect(element.querySelector('.prices strong')?.textContent).toContain('80.00');
    expect(element.querySelector('.discount-badge')?.textContent).toContain('20');
    expect(element.querySelector('.validity')?.textContent).toContain('2026');
    expect(element.querySelector('h2 a')?.getAttribute('href')).toBe('/producto/cpu');
  });

  it('distingue una lista vacía de un error de la API', () => {
    api.list.mockReturnValue(of({ ...response, content: [], totalElements: 0, totalPages: 0 }));
    const fixture = TestBed.createComponent(Offers); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No hay ofertas vigentes por ahora');
    api.list.mockReturnValue(throwError(() => new Error('offline')));
    params.next(convertToParamMap({ q: 'CPU' })); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar las ofertas');
    expect(fixture.nativeElement.textContent).not.toContain('No hay ofertas vigentes');
  });

  it('recupera los filtros y la página desde la URL', () => {
    params.next(convertToParamMap({ categoria: 'procesadores', enStock: 'true', descuentoMin: '20', pagina: '2', orden: 'fin' }));
    api.list.mockReturnValue(of({ ...response, totalPages: 3 }));
    const fixture = TestBed.createComponent(Offers); fixture.detectChanges();
    expect(api.list).toHaveBeenCalledWith(expect.objectContaining({ categoria: 'procesadores', enStock: true, descuentoMin: '20', orden: 'fin' }), 1);
  });

  it('permite limpiar filtros cuando no hay coincidencias', () => {
    params.next(convertToParamMap({ q: 'sin coincidencias' }));
    api.list.mockReturnValue(of({ ...response, content: [], totalElements: 0, totalPages: 0 }));
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(Offers); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No encontramos ofertas con estos filtros');
    const button = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find(item => item.textContent?.trim() === 'Limpiar filtros')!;
    button.click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: {} }));
  });
});
