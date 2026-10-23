import { Cart } from '../../core/cart/cart.models';

export interface CheckoutCustomer {
  documentType: 'DNI' | 'RUC' | 'CE';
  documentNumber: string;
  name: string;
  email: string;
  phone: string;
}
export interface CheckoutDelivery { shippingRateId: string; address: string; reference: string; }
export interface PaymentMethod { id: string; code: string; name: string; }
export interface ShippingRate {
  id: string; carrier: string; department: string | null; province: string | null;
  district: string | null; cost: number; days: number | null;
}
export interface CheckoutOptions { paymentMethods: PaymentMethod[]; shippingRates: ShippingRate[]; }
export interface CheckoutValidation {
  valid: boolean; requiresAuthentication: boolean; currency: string; subtotal: number; shipping: number;
  total: number; paymentMethod: string; delivery: string; messages: string[]; cart: Cart;
}
