import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProductDetailComponent } from './product-detail';
import { ProductDetailService } from './product-detail.service';
import { CatalogService } from '../catalog/catalog.service';
import { CartService } from '../../core/cart/cart.service';
import { FavoritesService } from '../../core/favorites/favorites.service';

describe('Recuperación del detalle de producto', () => {
  const params = new BehaviorSubject(convertToParamMap({ slug: 'cpu' }));
  const getBySlug = vi.fn();
  beforeEach(() => {
    params.next(convertToParamMap({ slug: 'cpu' }));
    getBySlug.mockReset().mockReturnValue(throwError(() => ({ status: 503 })));
    TestBed.configureTestingModule({ imports: [ProductDetailComponent], providers: [provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: params } },
      { provide: ProductDetailService, useValue: { getBySlug } },
      { provide: CatalogService, useValue: {} }, { provide: CartService, useValue: {} },
      { provide: FavoritesService, useValue: { has: () => false } },
    ] });
  });
  it('distingue una caída temporal de un producto inexistente y permite reintentar', () => {
    const fixture = TestBed.createComponent(ProductDetailComponent); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No pudimos cargar el producto');
    getBySlug.mockReturnValue(throwError(() => ({ status: 404 })));
    fixture.nativeElement.querySelector('app-request-error button').click(); fixture.detectChanges();
    expect(getBySlug).toHaveBeenCalledTimes(2);
    expect(fixture.nativeElement.textContent).toContain('No encontramos este producto');
  });
  it('sigue atendiendo cambios de ruta después de un error y muestra skeletons', () => {
    const fixture = TestBed.createComponent(ProductDetailComponent); fixture.detectChanges();
    getBySlug.mockReturnValue(new Subject()); params.next(convertToParamMap({ slug: 'otro' })); fixture.detectChanges();
    expect(getBySlug).toHaveBeenLastCalledWith('otro');
    expect(fixture.nativeElement.querySelector('app-loading-state [role=status]')).toBeTruthy();
  });
});
