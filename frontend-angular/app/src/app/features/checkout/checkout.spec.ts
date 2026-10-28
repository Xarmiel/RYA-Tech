import {TestBed} from '@angular/core/testing';
import {provideRouter} from '@angular/router';
import {signal} from '@angular/core';
import {registerLocaleData} from '@angular/common';
import locale from '@angular/common/locales/es-PE';
import {of,throwError} from 'rxjs';
import {beforeEach,afterEach,it,describe,expect,vi} from 'vitest';
import {Checkout} from './checkout';
import {CheckoutService} from './checkout.service';
import {CartService} from '../../core/cart/cart.service';

registerLocaleData(locale);
describe('Checkout Orders',()=>{
  let api:any,cart:any,component:any,fixture:any,create:any,unmount:any,yape:any;
  const init={pedidoId:'pedido-1',numeroPedido:'RYA-1',metodoPagoCodigo:'MP_CARDS',total:100,moneda:'PEN',publicKey:'public-test'};
  const pending={exitoso:false,pendiente:true,pedidoId:'pedido-1',numeroPedido:'RYA-1',estadoPedido:'PENDIENTE_PAGO',estadoPago:'action_required',mensaje:'Pendiente',monto:100,moneda:'PEN',mpOrderId:'ORD1'};
  beforeEach(()=>{
    vi.stubGlobal('scrollTo',vi.fn());
    cart={cart:signal({items:[{id:'item',productId:'p',slug:'p',name:'Producto',sku:'SKU',quantity:1,availableStock:10,subtotal:100}],subtotal:100}),totalQuantity:()=>1,load:vi.fn(()=>of({}))};
    api={savedCards:()=>of([]),getOptions:()=>of({paymentMethods:[],shippingRates:[]}),iniciarPago:vi.fn(()=>of(init)),procesarCargo:vi.fn(()=>of(pending)),consultarPago:vi.fn(()=>of(pending))};
    unmount=vi.fn();create=vi.fn((_kind,_id,options)=>{options.callbacks.onReady();return Promise.resolve({unmount});});yape=vi.fn(()=>({create:()=>Promise.resolve({id:'yape-token'})}));
    vi.stubGlobal('MercadoPago',vi.fn(function(){return{bricks:()=>({create}),yape};}));
    TestBed.configureTestingModule({imports:[Checkout],providers:[provideRouter([]),{provide:CheckoutService,useValue:api},{provide:CartService,useValue:cart}]});
    fixture=TestBed.createComponent(Checkout);component=fixture.componentInstance;fixture.detectChanges();
    component.customer.email='buyer@example.test';component.paymentMethodId='method-1';component.currentStep.set(4);
    component.validation.set({valid:true,requiresAuthentication:false,total:100,subtotal:100,shipping:0,currency:'PEN',paymentMethod:'Tarjeta',delivery:'Entrega',messages:[]});fixture.detectChanges();
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.unstubAllGlobals();});
  it('sanitizes document input and updates its length when the document type changes',async()=>{
    component.currentStep.set(1);fixture.detectChanges();await fixture.whenStable();
    const input:HTMLInputElement=fixture.nativeElement.querySelector('[name="documentNumber"]');
    const select:HTMLSelectElement=fixture.nativeElement.querySelector('[name="documentType"]');
    input.value='12ab34567890';input.dispatchEvent(new Event('input',{bubbles:true}));fixture.detectChanges();
    expect(component.customer.documentNumber).toBe('12345678');expect(input.value).toBe('12345678');expect(input.maxLength).toBe(8);expect(input.inputMode).toBe('numeric');
    select.value='RUC';select.dispatchEvent(new Event('change',{bubbles:true}));fixture.detectChanges();
    input.value='201234567890x';input.dispatchEvent(new Event('input',{bubbles:true}));fixture.detectChanges();
    expect(component.customer.documentNumber).toBe('20123456789');expect(input.value).toBe('20123456789');expect(input.maxLength).toBe(11);
    select.value='DNI';select.dispatchEvent(new Event('change',{bubbles:true}));fixture.detectChanges();await fixture.whenStable();
    expect(component.customer.documentNumber).toBe('20123456');expect(input.value).toBe('20123456');
    select.value='CE';select.dispatchEvent(new Event('change',{bubbles:true}));fixture.detectChanges();
    input.value='AB-123456789012';input.dispatchEvent(new Event('input',{bubbles:true}));fixture.detectChanges();
    expect(component.customer.documentNumber).toBe('AB1234567890');expect(input.maxLength).toBe(12);expect(input.inputMode).toBe('text');
  });
  it('filters delivery rates by location, clears selection and allows pickup without a home address',()=>{
    const home={id:'home',carrier:'Courier',department:'Lima',province:'Lima',district:'San Miguel',cost:12,days:1,mode:'DOMICILIO'};
    const wide={...home,id:'wide',province:null,district:null};
    const pickup={...home,id:'pickup',mode:'RECOJO',pickupAddress:'Av. Tienda 123',pickupHours:'9 a 18',cost:0};
    component.options.set({paymentMethods:[],shippingRates:[home,wide,pickup]});
    component.delivery={shippingRateId:'',address:'',reference:'',department:'LIMA',province:'Lima',district:'San Miguel'};
    expect(component.matchingRates().map((r:any)=>r.id)).toEqual(['home','wide']);
    component.selectRate(home);component.delivery.district='Otro';component.resetDelivery();
    expect(component.delivery.shippingRateId).toBe('');expect(component.matchingRates().map((r:any)=>r.id)).toEqual(['wide']);
    component.delivery.department='Cusco';component.goToPayment();expect(component.errors()['shippingRate']).toBeTruthy();
    component.resetDelivery('RECOJO');expect(component.matchingRates().map((r:any)=>r.id)).toEqual(['pickup']);
    component.selectRate(pickup);expect(component.delivery.address).toBe('Av. Tienda 123');component.goToPayment();expect(component.currentStep()).toBe(3);
  });
  it('sends only the token and Orders metadata and retains pending status',async()=>{
    component.pagarConMercadoPago();await fixture.whenStable();
    await component.submitOrder({token:'token',paymentMethodId:'visa',paymentTypeId:'debit_card'});
    expect(api.procesarCargo).toHaveBeenCalledWith('pedido-1','buyer@example.test',expect.objectContaining({paymentMethodId:'visa',paymentTypeId:'debit_card'}));
    expect(api.procesarCargo.mock.calls[0][2]).not.toHaveProperty('amount');expect(component.pendingPayment()).toBeTruthy();expect(component.paymentSuccess()).toBeNull();
  });
  it('opens the secure card phase after validation without charging',()=>{
    api.validate=vi.fn(()=>of(component.validation()));component.options.set({paymentMethods:[{id:'method-1',code:'MP_CARDS',name:'Tarjeta'}],shippingRates:[]});
    component.validateOrder();expect(component.currentStep()).toBe(4);expect(api.iniciarPago).toHaveBeenCalledTimes(1);expect(api.procesarCargo).not.toHaveBeenCalled();
  });
  it('creates a PagoEfectivo code and displays pending instructions rather than a success receipt',()=>{
    api.iniciarPago.mockReturnValue(of({...init,metodoPagoCodigo:'MP_PAGOEFECTIVO'}));
    api.procesarCargo.mockReturnValue(of({...pending,referenciaPago:'123456',instruccionesUrl:'https://www.mercadopago.com.pe/payments/1/ticket'}));
    component.pagarConMercadoPago();fixture.detectChanges();expect(api.procesarCargo).toHaveBeenCalledWith('pedido-1','buyer@example.test',{paymentMethodId:'pagoefectivo_atm',paymentTypeId:'atm'});
    expect(fixture.nativeElement.textContent).toContain('123456');expect(fixture.nativeElement.textContent).not.toContain('¡PAGO COMPLETADO!');expect(create).not.toHaveBeenCalled();
  });
  it('tokenizes Yape in the SDK and clears the OTP without sending it to the backend',async()=>{
    api.iniciarPago.mockReturnValue(of({...init,metodoPagoCodigo:'MP_YAPE'}));component.pagarConMercadoPago();component.yapePhone='999999999';component.yapeOtp='123456';
    await component.payYape();expect(yape).toHaveBeenCalledWith({otp:'123456',phoneNumber:'999999999'});expect(component.yapeOtp).toBe('');
    expect(api.procesarCargo.mock.calls[0][2]).toEqual({token:'yape-token',paymentMethodId:'yape',paymentTypeId:'debit_card'});
  });
  it('locks an uncertain response until its status is checked, without automatically submitting again',async()=>{
    component.pagarConMercadoPago();await fixture.whenStable();api.procesarCargo.mockReturnValue(throwError(()=>({status:0})));
    const submit=(token:string)=>component.submitOrder({token,paymentMethodId:'visa',paymentTypeId:'credit_card'});await expect(submit('token')).rejects.toThrow();
    await expect(submit('token2')).rejects.toThrow();expect(api.procesarCargo).toHaveBeenCalledTimes(1);expect(component.paymentUncertain()).toBe(true);
    fixture.detectChanges();expect(fixture.nativeElement.textContent).toContain('No pudimos confirmar si el pago se inició');
    api.consultarPago.mockReturnValue(of({...pending,pendiente:false,estadoPago:'not_started',mensaje:'Todavía no se ha enviado un pago.'}));
    component.checkPayment();expect(api.consultarPago).toHaveBeenCalledWith('pedido-1');expect(component.paymentUncertain()).toBe(false);expect(component.pendingPayment()).toBeNull();expect(component.message()).toContain('Todavía no se ha enviado');
  });
  it('keeps the paid receipt visible after the cart empties',async()=>{
    component.pagarConMercadoPago();await fixture.whenStable();api.procesarCargo.mockReturnValue(of({...pending,exitoso:true,pendiente:false,estadoPedido:'PAGADO'}));
    await component.submitOrder({token:'t',paymentMethodId:'visa',paymentTypeId:'credit_card'});cart.cart.set({items:[],subtotal:0});fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Gracias por tu compra');expect(fixture.nativeElement.textContent).not.toContain('Tu carrito está vacío');
  });
});
