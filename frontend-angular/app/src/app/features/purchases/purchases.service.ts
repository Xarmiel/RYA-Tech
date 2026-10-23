import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface OrderItem {
  id: string; productoId: string | null; nombre: string; sku: string | null; cantidad: number;
  precioUnitario: number; descuentoUnitario: number; subtotal: number; slug: string | null; imagen: string | null;
}
export interface Order {
  id: string; codigo: string; estado: string; moneda: string; subtotal: number; descuento: number;
  envio: number; total: number; fecha: string; metodoPago: string | null; productos: OrderItem[];
}
export interface OrderPage { content: Order[]; page: number; size: number; totalElements: number; totalPages: number; }

@Injectable({ providedIn: 'root' })
export class PurchasesService {
  private readonly http = inject(HttpClient);
  list(page = 0): Observable<OrderPage> {
    return this.http.get<OrderPage>('/api/v1/pedidos', { params: new HttpParams().set('page', page).set('size', 12) });
  }
  detail(id: string): Observable<Order> { return this.http.get<Order>(`/api/v1/pedidos/${encodeURIComponent(id)}`); }
}
