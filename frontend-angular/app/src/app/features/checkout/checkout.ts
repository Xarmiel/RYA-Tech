import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin, timeout } from 'rxjs';
import { CartItem } from '../../core/cart/cart.models';
import { CartService } from '../../core/cart/cart.service';
import { CheckoutCustomer, CheckoutDelivery, CheckoutOptions, CheckoutValidation, ShippingRate } from './checkout.models';
import { CheckoutService } from './checkout.service';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { CardCheckout } from './card-checkout';
import { RequestError } from '../../shared/request-error/request-error';

@Component({
  selector: 'app-checkout',
  imports: [CurrencyPipe, FormsModule, RouterLink, LoadingState, RequestError, CardCheckout],
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
  protected readonly processingPayment = signal(false);
  protected readonly cardPreparing = signal(false);
  protected readonly paymentSuccess = signal<import('./checkout.models').ResultadoPago | null>(null);
  protected readonly message = signal('');
  protected readonly errors = signal<Record<string, string>>({});
  protected paymentMethodId = '';
  protected customer: CheckoutCustomer = { documentType: 'DNI', documentNumber: '', name: '', email: '', phone: '' };
  protected deliveryMode: 'DOMICILIO'|'RECOJO' = 'DOMICILIO';
  protected delivery: CheckoutDelivery = { shippingRateId: '', address: '', reference: '' };
  private activePedidoId = '';

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

  protected readonly paymentSession = signal<import('./checkout.models').IniciarPagoResponse|null>(null);
  protected readonly pendingPayment = signal<import('./checkout.models').ResultadoPago|null>(null);
  protected readonly paymentUncertain = signal(false);
  protected yapePhone = '';
  protected yapeOtp = '';
  private destroyed=false;

  constructor(){
    this.destroyRef.onDestroy(()=>{this.destroyed=true;this.yapeOtp='';});
  }
  protected changePayment():void{
    if(this.processingPayment()||this.cardPreparing()||this.pendingPayment()||this.paymentUncertain())return;
    this.paymentSession.set(null);this.currentStep.set(3);
  }
  protected pagarConMercadoPago():void{
    if(!this.paymentMethodId||!this.validation()||this.processingPayment()||this.paymentSession())return;
    this.processingPayment.set(true);this.message.set('');
    this.checkout.iniciarPago(this.customer,this.delivery,this.paymentMethodId).pipe(timeout(30000),takeUntilDestroyed(this.destroyRef)).subscribe({
      next:response=>{
        this.activePedidoId=response.pedidoId;this.paymentSession.set(response);
        if(response.metodoPagoCodigo==='MP_CARDS')this.processingPayment.set(false);
        else if(response.metodoPagoCodigo==='MP_PAGOEFECTIVO'){
          this.processingPayment.set(false);
          void this.submitOrder({paymentMethodId:'pagoefectivo_atm',paymentTypeId:'atm'}).catch(()=>undefined);
        }else this.processingPayment.set(false);
      },
      error:error=>{this.processingPayment.set(false);this.message.set(error?.error?.message??'No se pudo iniciar la pasarela.');}
    });
  }
  private sdk(publicKey:string):any{
    const constructor=(globalThis as unknown as {MercadoPago?:any}).MercadoPago;
    if(!constructor)throw new Error('El formulario de Mercado Pago no está disponible. Revisa tu conexión.');
    return new constructor(publicKey,{locale:'es-PE'});
  }
  protected payCard(data:import('./checkout.models').OrdersPaymentData):void{
    void this.submitOrder(data).catch(()=>undefined);
  }
  protected async payYape():Promise<void>{
    if(this.processingPayment()||this.pendingPayment()||this.paymentUncertain())return;
    const session=this.paymentSession();if(!session)return;
    if(!/^\d{9}$/.test(this.yapePhone)||!/^\d{6}$/.test(this.yapeOtp)){this.message.set('Ingresa el celular de Yape (9 dígitos) y su código de aprobación (6 dígitos).');return;}
    this.processingPayment.set(true);this.message.set('');
    try{
      const token=await this.sdk(session.publicKey).yape({otp:this.yapeOtp,phoneNumber:this.yapePhone}).create();
      this.yapeOtp='';if(this.destroyed)return;
      this.processingPayment.set(false);await this.submitOrder({token:token.id,paymentMethodId:'yape',paymentTypeId:'debit_card'});
    }catch{
      this.yapeOtp='';this.processingPayment.set(false);
      if(!this.message())this.message.set('No se pudo completar Yape. Revisa los datos y consulta el estado antes de volver a pagar.');
    }
  }
  private submitOrder(data:import('./checkout.models').OrdersPaymentData,email=this.customer.email):Promise<void>{
    if(this.processingPayment()||this.pendingPayment()||this.paymentUncertain()||!this.activePedidoId)return Promise.reject(new Error('Hay un pago en curso.'));
    this.processingPayment.set(true);this.message.set('');
    return new Promise((resolve,reject)=>{
      this.checkout.procesarCargo(this.activePedidoId,email,data).pipe(timeout(25000),takeUntilDestroyed(this.destroyRef)).subscribe({
        next:result=>{this.processingPayment.set(false);this.acceptPaymentResult(result);resolve();},
        error:error=>{
          this.processingPayment.set(false);
          if(!error?.status||error.status>=500||[401,403,408,409,429].includes(error.status))this.paymentUncertain.set(true);
          this.message.set(error?.error?.message||'No recibimos la confirmación. Consulta el estado antes de volver a pagar.');reject(new Error('No se confirmó el pago.'));
        }
      });
    });
  }
  private acceptPaymentResult(result:import('./checkout.models').ResultadoPago):void{
    this.paymentUncertain.set(false);
    if(result.exitoso){
      this.pendingPayment.set(null);this.paymentSuccess.set(result);
      this.cart.load().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({error:()=>undefined});this.scrollTop();
    }else{
      this.pendingPayment.set(result.pendiente?result:null);this.message.set(result.mensaje||'El pago no se completó.');
    }
  }
  protected checkPayment():void{
    if(!this.activePedidoId||this.processingPayment())return;
    this.processingPayment.set(true);
    this.checkout.consultarPago(this.activePedidoId).pipe(timeout(20000),takeUntilDestroyed(this.destroyRef)).subscribe({
      next:result=>{this.processingPayment.set(false);this.acceptPaymentResult(result);},
      error:error=>{this.processingPayment.set(false);this.message.set(error?.error?.message||'No se pudo consultar el estado. No vuelvas a pagar hasta confirmarlo.');}
    });
  }

  protected goToCustomer(): void { this.currentStep.set(1); this.scrollTop(); }
  protected goToDelivery(): void {
    if (!this.validateCustomer()) return;
    this.currentStep.set(2); this.scrollTop();
  }
  protected goToPayment(): void {
    const next: Record<string, string> = {};
    const selected = this.matchingRates().find(rate => rate.id === this.delivery.shippingRateId);
    if (!selected) next['shippingRate'] = 'Selecciona una opción disponible para tu ubicación.';
    if (this.deliveryMode === 'DOMICILIO') {
      if (![this.delivery.department, this.delivery.province, this.delivery.district].every(v => v?.trim())) next['location'] = 'Completa departamento, provincia y distrito.';
      if (this.delivery.address.trim().length < 5) next['address'] = 'Ingresa una dirección completa.';
    }
    this.errors.set(next);
    if (Object.keys(next).length) return;
    this.currentStep.set(3); this.scrollTop();
  }
  protected validateOrder(): void {
    if (!this.paymentMethodId) { this.errors.set({ payment: 'Selecciona un método de pago.' }); return; }
    this.validating.set(true); this.message.set(''); this.errors.set({});
    this.checkout.validate(this.customer, this.delivery, this.paymentMethodId)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: result => {
          this.validation.set(result); this.validating.set(false); this.currentStep.set(4); this.scrollTop();
          const code=this.options().paymentMethods.find(method=>method.id===this.paymentMethodId)?.code;
          if(!result.requiresAuthentication&&['MP_CARDS','MP_YAPE'].includes(code??''))this.pagarConMercadoPago();
        },
        error: error => { this.validating.set(false); this.message.set(error?.error?.message ?? 'No se pudo validar la compra.'); },
      });
  }

  protected selectRate(rate: ShippingRate): void {
    this.delivery.shippingRateId = rate.id;
    if (this.deliveryMode === 'RECOJO') this.delivery.address = rate.pickupAddress ?? '';
    this.errors.set({}); this.validation.set(null);
  }
  protected resetDelivery(mode = this.deliveryMode): void {
    const changed = this.deliveryMode !== mode;
    this.deliveryMode = mode; this.delivery.shippingRateId = '';
    if (changed) { this.delivery.address = ''; this.delivery.reference = ''; }
    this.validation.set(null); this.errors.set({});
  }
  private normalized(value?: string|null): string { return (value ?? '').trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase(); }
  protected locationValues(field: 'department'|'province'|'district'): string[] {
    return [...new Set(this.options().shippingRates.filter(rate => (rate.mode ?? 'DOMICILIO') === 'DOMICILIO')
      .filter(rate => field === 'department' || !rate.department || this.normalized(rate.department) === this.normalized(this.delivery.department))
      .filter(rate => field !== 'district' || !rate.province || this.normalized(rate.province) === this.normalized(this.delivery.province))
      .map(rate => rate[field]).filter((v): v is string => !!v))].sort();
  }
  protected matchingRates(): ShippingRate[] {
    return this.options().shippingRates.filter(rate => {
      if ((rate.mode ?? 'DOMICILIO') !== this.deliveryMode) return false;
      if (this.deliveryMode === 'RECOJO') return true;
      if (![this.delivery.department, this.delivery.province, this.delivery.district].every(v => v?.trim())) return false;
      return (['department','province','district'] as const).every(field => !rate[field] || this.normalized(rate[field]) === this.normalized(this.delivery[field]));
    });
  }
  protected rateLabel(rate: ShippingRate): string {
    return [rate.department, rate.province, rate.district].filter(Boolean).join(', ') || 'Cobertura general';
  }
  protected changeQuantity(item: CartItem, quantity: number): void {
    if (this.paymentSession() || quantity < 1 || quantity > item.availableStock || this.updating().has(item.id)) return;
    this.run(item.id, this.cart.update(item.id, quantity));
  }
  protected remove(item: CartItem): void {
    if (!this.paymentSession() && !this.updating().has(item.id)) this.run(item.id, this.cart.remove(item.id));
  }

  protected get documentMaxLength(): number {
    return { DNI: 8, RUC: 11, CE: 12 }[this.customer.documentType];
  }

  protected normalizeDocument(value: string): void {
    const unsupported = this.customer.documentType === 'CE' ? /[^a-zA-Z0-9]/g : /[^0-9]/g;
    this.customer.documentNumber = value.replace(unsupported, '').slice(0, this.documentMaxLength);
    this.errors.update(({ document, ...remaining }) => remaining);
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
