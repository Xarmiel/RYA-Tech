import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';
import { Cart } from './cart.models';
import { NotificationService } from '../notifications/notification.service';

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly http = inject(HttpClient);
  private readonly notifications = inject(NotificationService);
  private readonly sessionStorageKey = 'rya.cart.session.v1';
  private readonly sessionKey = this.getOrCreateSessionKey();

  readonly cart = signal<Cart | null>(null);
  readonly totalQuantity = computed(() => this.cart()?.totalQuantity ?? 0);

  load(): Observable<Cart> {
    return this.http.get<BackendCart>('/api/v1/carrito', { headers: this.requestHeaders() })
      .pipe(map(this.mapCart), tap(cart => this.cart.set(cart)));
  }

  add(productId: string, quantity = 1): Observable<Cart> {
    return this.http.post<BackendCart>('/api/v1/carrito/items', { productoId: productId, cantidad: quantity },
      { headers: this.requestHeaders() }).pipe(map(this.mapCart), tap(cart => {
        this.cart.set(cart); this.notifications.show('Producto añadido al carrito.', 'success');
      }));
  }

  update(itemId: string, quantity: number): Observable<Cart> {
    return this.http.patch<BackendCart>(`/api/v1/carrito/items/${itemId}`, { cantidad: quantity },
      { headers: this.requestHeaders() }).pipe(map(this.mapCart), tap(cart => this.cart.set(cart)));
  }

  remove(itemId: string): Observable<Cart> {
    return this.http.delete<BackendCart>(`/api/v1/carrito/items/${itemId}`, { headers: this.requestHeaders() })
      .pipe(map(this.mapCart), tap(cart => this.cart.set(cart)));
  }

  linkToAccount(): Observable<Cart> {
    return this.http.post<BackendCart>('/api/v1/carrito/vincular', {}, { headers: this.requestHeaders() })
      .pipe(map(this.mapCart), tap(cart => this.cart.set(cart)));
  }

  requestHeaders(): HttpHeaders {
    return new HttpHeaders({ 'X-Cart-Session': this.sessionKey });
  }

  private getOrCreateSessionKey(): string {
    let stored: string | null = null;
    try { stored = localStorage.getItem(this.sessionStorageKey); } catch { /* Navegación privada: mantener sesión en memoria. */ }
    if (stored && stored.length >= 16) return stored;
    const created = globalThis.crypto?.randomUUID?.()
      ?? `${Date.now()}-${Math.random().toString(36).slice(2)}-${Math.random().toString(36).slice(2)}`;
    try { localStorage.setItem(this.sessionStorageKey, created); }
    catch { this.notifications.show('El navegador bloqueó el almacenamiento. El carrito será temporal hasta cerrar esta página.'); }
    return created;
  }

  private readonly mapCart = (response: BackendCart): Cart => ({
    id: response.id,
    currency: 'PEN',
    totalQuantity: response.cantidadTotal,
    subtotal: response.subtotal,
    items: response.items.map(item => ({
      id: item.id,
      productId: item.productoId,
      sku: item.sku,
      name: item.nombre,
      slug: item.slug,
      image: item.imagen,
      quantity: item.cantidad,
      availableStock: item.stockDisponible,
      unitPrice: item.precioUnitario,
      subtotal: item.subtotal,
      priceUpdated: item.precioActualizado,
    })),
  });
}

interface BackendCart {
  id: string | null;
  moneda: string;
  cantidadTotal: number;
  subtotal: number;
  items: Array<{
    id: string;
    productoId: string;
    sku: string;
    nombre: string;
    slug: string;
    imagen: string | null;
    cantidad: number;
    stockDisponible: number;
    precioUnitario: number;
    subtotal: number;
    precioActualizado: boolean;
  }>;
}
