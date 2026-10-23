import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface Offer {
  productoId: string; nombre: string; slug: string; marca: string | null; categoria: string;
  imagen: string | null; stockDisponible: number; precioAnterior: number; precioActual: number;
  descuentoPorcentaje: number; ahorro: number; promocion: string;
  inicioAt: string | null; finAt: string | null;
}
export interface OfferFilters { q: string; categoria: string; marca: string; enStock: boolean; descuentoMin: string; orden: string; }
export interface OfferPage {
  content: Offer[]; page: number; size: number; totalElements: number; totalPages: number;
  categorias: Array<{ slug: string; nombre: string }>; marcas: Array<{ slug: string; nombre: string }>;
}

@Injectable({ providedIn: 'root' })
export class OffersService {
  private readonly http = inject(HttpClient);
  list(filters: OfferFilters, page: number): Observable<OfferPage> {
    let params = new HttpParams().set('page', page).set('size', 12).set('orden', filters.orden);
    if (filters.q.trim()) params = params.set('q', filters.q.trim());
    if (filters.categoria) params = params.set('categoria', filters.categoria);
    if (filters.marca) params = params.set('marca', filters.marca);
    if (filters.enStock) params = params.set('enStock', true);
    if (filters.descuentoMin) params = params.set('descuentoMin', filters.descuentoMin);
    return this.http.get<OfferPage>('/api/v1/ofertas', { params });
  }
}
