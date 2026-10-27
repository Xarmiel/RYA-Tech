import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { CartService } from '../../core/cart/cart.service';
import { CheckoutCustomer, CheckoutDelivery, CheckoutOptions, CheckoutValidation } from './checkout.models';

@Injectable({ providedIn: 'root' })
export class CheckoutService {
  private readonly http = inject(HttpClient);
  private readonly cart = inject(CartService);

  getOptions(): Observable<CheckoutOptions> {
    return this.http.get<BackendOptions>('/api/v1/checkout/opciones').pipe(map(response => ({
      paymentMethods: response.metodosPago.map(item => ({ id: item.id, code: item.codigo, name: item.nombre })),
      shippingRates: response.tarifasEnvio.map(item => ({ id: item.id, carrier: item.transportista,
        department: item.departamento, province: item.provincia, district: item.distrito,
        cost: item.costo, days: item.plazoDias })),
    })));
  }

  validate(customer: CheckoutCustomer, delivery: CheckoutDelivery, paymentMethodId: string): Observable<CheckoutValidation> {
    return this.http.post<BackendValidation>('/api/v1/checkout/validar', {
      cliente: { tipoDocumento: customer.documentType, numeroDocumento: customer.documentNumber,
        nombre: customer.name, email: customer.email, telefono: customer.phone },
      entrega: { tarifaEnvioId: delivery.shippingRateId, direccion: delivery.address,
        referencia: delivery.reference || null },
      metodoPagoId: paymentMethodId,
    }, { headers: this.cart.requestHeaders() }).pipe(map(response => ({
      valid: response.valido, requiresAuthentication: response.requiereAutenticacion,
      currency: response.moneda, subtotal: response.subtotal, shipping: response.envio,
      total: response.total, paymentMethod: response.metodoPago, delivery: response.entrega,
      messages: response.mensajes, cart: this.cart.cart()!,
    })));
  }

  iniciarPago(customer: CheckoutCustomer, delivery: CheckoutDelivery, paymentMethodId: string): Observable<import('./checkout.models').IniciarPagoResponse> {
    return this.http.post<import('./checkout.models').IniciarPagoResponse>('/api/v1/pagos/iniciar', {
      cliente: { tipoDocumento: customer.documentType, numeroDocumento: customer.documentNumber,
        nombre: customer.name, email: customer.email, telefono: customer.phone },
      entrega: { tarifaEnvioId: delivery.shippingRateId, direccion: delivery.address,
        referencia: delivery.reference || null },
      metodoPagoId: paymentMethodId,
    }, { headers: this.cart.requestHeaders() });
  }

  procesarCargo(
    pedidoId: string,
    email: string,
    paymentData: import('./checkout.models').OrdersPaymentData
  ): Observable<import('./checkout.models').ResultadoPago> {
    return this.http.post<import('./checkout.models').ResultadoPago>('/api/v1/pagos/procesar', {
      pedidoId,
      email,
      token: paymentData.token,
      paymentMethodId: paymentData.paymentMethodId,
      installments: paymentData.installments,
      paymentTypeId: paymentData.paymentTypeId,
      identificationType: paymentData.identificationType,
      identificationNumber: paymentData.identificationNumber,
    }, { headers: this.cart.requestHeaders() });
  }
  consultarPago(pedidoId:string):Observable<import('./checkout.models').ResultadoPago>{
    return this.http.get<import('./checkout.models').ResultadoPago>(`/api/v1/pagos/pedidos/${encodeURIComponent(pedidoId)}`);
  }
}



interface BackendOptions {
  metodosPago: Array<{ id: string; codigo: string; nombre: string }>;
  tarifasEnvio: Array<{ id: string; transportista: string; departamento: string | null; provincia: string | null; distrito: string | null; costo: number; plazoDias: number | null }>;
}
interface BackendValidation {
  valido: boolean; requiereAutenticacion: boolean; moneda: string; subtotal: number; envio: number;
  total: number; metodoPago: string; entrega: string; mensajes: string[];
}
