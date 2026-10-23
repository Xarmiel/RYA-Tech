import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { CartItem } from '../../core/cart/cart.models';
import { CartService } from '../../core/cart/cart.service';
import { CheckoutCustomer, CheckoutDelivery, CheckoutOptions, CheckoutValidation, ShippingRate } from './checkout.models';
import { CheckoutService } from './checkout.service';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { RequestError } from '../../shared/request-error/request-error';

@Component({
  selector: 'app-checkout',
  imports: [CurrencyPipe, FormsModule, RouterLink, LoadingState, RequestError],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class Checkout implements OnInit {
  protected readonly cart = inject(CartService);
  private readonly checkout = inject(CheckoutService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly currentStep = signal(1);
  protected readonly options = signal<CheckoutOptions>({ paymentMethods: [], shippingRates: [] });
  protected readonly validation = signal<CheckoutValidation | null>(null);
  protected readonly updating = signal<Set<string>>(new Set());
  protected readonly validating = signal(false);
  protected readonly message = signal('');
  protected readonly errors = signal<Record<string, string>>({});
  protected paymentMethodId = '';
  protected customer: CheckoutCustomer = { documentType: 'DNI', documentNumber: '', name: '', email: '', phone: '' };
  protected delivery: CheckoutDelivery = { shippingRateId: '', address: '', reference: '' };

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.state.set('loading'); this.message.set('');
    forkJoin({ cart: this.cart.load(), options: this.checkout.getOptions() })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: result => { this.options.set(result.options); this.state.set('ready'); },
        error: error => { this.state.set('error'); this.message.set(error?.error?.message ?? 'No se pudo preparar el checkout.'); },
      });
  }

  protected goToCustomer(): void { this.currentStep.set(1); this.scrollTop(); }
  protected goToDelivery(): void {
    if (!this.validateCustomer()) return;
    this.currentStep.set(2); this.scrollTop();
  }
  protected goToPayment(): void {
    const next: Record<string, string> = {};
    if (!this.delivery.shippingRateId) next['shippingRate'] = 'Selecciona una tarifa de envío disponible.';
    if (this.delivery.address.trim().length < 5) next['address'] = 'Ingresa una dirección completa.';
    this.errors.set(next);
    if (Object.keys(next).length) return;
    this.currentStep.set(3); this.scrollTop();
  }
  protected validateOrder(): void {
    if (!this.paymentMethodId) { this.errors.set({ payment: 'Selecciona un método de pago.' }); return; }
    this.validating.set(true); this.message.set(''); this.errors.set({});
    this.checkout.validate(this.customer, this.delivery, this.paymentMethodId)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: result => { this.validation.set(result); this.validating.set(false); this.currentStep.set(4); this.scrollTop(); },
        error: error => { this.validating.set(false); this.message.set(error?.error?.message ?? 'No se pudo validar la compra.'); },
      });
  }

  protected selectRate(rate: ShippingRate): void { this.delivery.shippingRateId = rate.id; this.errors.set({}); }
  protected rateLabel(rate: ShippingRate): string {
    return [rate.department, rate.province, rate.district].filter(Boolean).join(', ') || 'Cobertura general';
  }
  protected changeQuantity(item: CartItem, quantity: number): void {
    if (quantity < 1 || quantity > item.availableStock || this.updating().has(item.id)) return;
    this.run(item.id, this.cart.update(item.id, quantity));
  }
  protected remove(item: CartItem): void {
    if (!this.updating().has(item.id)) this.run(item.id, this.cart.remove(item.id));
  }

  private validateCustomer(): boolean {
    const next: Record<string, string> = {};
    const rules = { DNI: /^\d{8}$/, RUC: /^\d{11}$/, CE: /^[a-zA-Z0-9]{6,12}$/ };
    if (!rules[this.customer.documentType].test(this.customer.documentNumber)) next['document'] = `Documento ${this.customer.documentType} inválido.`;
    if (this.customer.name.trim().length < 3) next['name'] = 'Ingresa tu nombre completo o razón social.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.customer.email)) next['email'] = 'Ingresa un correo electrónico válido.';
    if (!/^\d{9}$/.test(this.customer.phone)) next['phone'] = 'El teléfono debe tener 9 dígitos.';
    this.errors.set(next);
    return !Object.keys(next).length;
  }
  private run(itemId: string, request: ReturnType<CartService['remove']>): void {
    this.updating.update(current => new Set(current).add(itemId)); this.message.set('');
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.updating.update(current => { const next = new Set(current); next.delete(itemId); return next; });
        if (this.validation()) {
          this.validation.set(null);
          this.currentStep.set(3);
          this.message.set('El carrito cambió. Revisa y valida nuevamente la compra.');
        }
      },
      error: error => {
        this.updating.update(current => { const next = new Set(current); next.delete(itemId); return next; });
        this.message.set(error?.error?.message ?? 'No se pudo actualizar el carrito.');
      },
    });
  }
  private scrollTop(): void { globalThis.scrollTo?.({ top: 0, behavior: 'smooth' }); }
}
