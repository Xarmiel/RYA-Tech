import { TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEsPe from '@angular/common/locales/es-PE';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Catalog } from './catalog';
import { CatalogService } from './catalog.service';
import { CartService } from '../../core/cart/cart.service';
import { FavoritesService } from '../../core/favorites/favorites.service';
import { CatalogPage } from './catalog.models';

registerLocaleData(localeEsPe);

describe('Navegación del catálogo', () => {
  const params = new BehaviorSubject(convertToParamMap({}));
  const product = { id: 'cpu-1', sku: 'CPU', slug: 'cpu', name: 'CPU de prueba', brand: 'AMD', category: 'Procesadores', price: 100, currency: 'PEN' as const, availableStock: 0, primaryImage: null };
  const page: CatalogPage = { items: [product], page: 0, size: 12, totalItems: 17, totalPages: 2 };
  const service = { getProducts: vi.fn(), getFilters: vi.fn() };

  beforeEach(() => {
    params.next(convertToParamMap({ categoria: 'procesadores', marca: 'amd', 'filtro.socket_cpu': 'AM5' }));
    service.getProducts.mockReset().mockReturnValue(of(page));
    service.getFilters.mockReset().mockReturnValue(of({ groups: [
      { code: '__hardware', label: 'Hardware', kind: 'section', options: [{ value: 'procesadores', label: 'Procesadores', count: 17 }] },
      { code: '__marca', label: 'Fabricante', kind: 'filter', options: [{ value: 'amd', label: 'AMD', count: 12 }] },
      { code: 'socket_cpu', label: 'Socket', kind: 'filter', options: [{ value: 'AM5', label: 'AM5', count: 4 }] },
    ] }));
    TestBed.configureTestingModule({ imports: [Catalog], providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { queryParamMap: params } },
      { provide: CatalogService, useValue: service },
      { provide: CartService, useValue: {} },
      { provide: FavoritesService, useValue: { has: () => false, toggle: vi.fn() } },
    ] });
  });

  it('sitúa el botón de filtros después del carrusel y antes del contador', () => {
    const fixture = TestBed.createComponent(Catalog); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const carousel = element.querySelector('.category-bubbles-section')!;
    const filters = element.querySelector('.catalog-filter-actions')!;
    const results = element.querySelector('#catalog-results')!;
    expect(carousel.compareDocumentPosition(filters) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(filters.compareDocumentPosition(results) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    expect(element.querySelector('.page-heading .filter-trigger')).toBeNull();
  });

  it('pagina manteniendo categoría, marca y atributos', () => {
    const fixture = TestBed.createComponent(Catalog);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    const next = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')].find(button => button.textContent?.trim() === 'Siguiente')!;
    next.click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: {
      categoria: ['procesadores'], marca: ['amd'], 'filtro.socket_cpu': ['AM5'], pagina: 2,
    } }));
  });

  it('ordena desde la API y consulta la página indicada en la URL', () => {
    params.next(convertToParamMap({ pagina: '2', orden: 'precio-desc' }));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    expect(service.getProducts).toHaveBeenCalledWith('', 1, 12, {}, 'precio', 'desc');
    expect((fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>('#catalog-sort')!.value).toBe('precio-desc');
  });

  it('muestra chips legibles y permite eliminar un atributo sin quitar los demás', () => {
    const fixture = TestBed.createComponent(Catalog);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
    const chip = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('[aria-label="Quitar filtro Socket: AM5"]')!;
    expect(chip).toBeTruthy();
    chip.click();
    expect(navigate).toHaveBeenCalledWith([], expect.objectContaining({ queryParams: { categoria: ['procesadores'], marca: ['amd'] } }));
  });

  it('muestra un estado vacío con una acción para limpiar la búsqueda', () => {
    service.getProducts.mockReturnValue(of({ ...page, items: [], totalItems: 0, totalPages: 0 }));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No encontramos resultados');
    expect(fixture.nativeElement.textContent).toContain('Limpiar búsqueda y filtros');
  });

  it('cancela consultas anteriores al cambiar la búsqueda', () => {
    const previous = new Subject<CatalogPage>();
    service.getProducts.mockReturnValueOnce(previous).mockReturnValue(of(page));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    expect(previous.observed).toBe(true);
    params.next(convertToParamMap({ q: 'nuevo' }));
    expect(previous.observed).toBe(false);
  });

  it('sustituye imágenes rotas por el marcador de su categoría', () => {
    service.getProducts.mockReturnValue(of({ ...page, items: [{ ...product, primaryImage: { url: '/imagen-inexistente.png', alt: null } }] }));
    const fixture = TestBed.createComponent(Catalog);
    fixture.detectChanges();
    const image = (fixture.nativeElement as HTMLElement).querySelector('.product-image img')!;
    image.dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.product-image img')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.image-unavailable')?.textContent).toContain('Procesadores');
  });
});
