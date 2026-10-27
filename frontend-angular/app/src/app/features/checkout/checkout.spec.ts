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
    api={getOptions:()=>of({paymentMethods:[],shippingRates:[]}),iniciarPago:vi.fn(()=>of(init)),procesarCargo:vi.fn(()=>of(pending)),consultarPago:vi.fn(()=>of(pending))};
    unmount=vi.fn();create=vi.fn((_kind,_id,options)=>{options.callbacks.onReady();return Promise.resolve({unmount});});yape=vi.fn(()=>({create:()=>Promise.resolve({id:'yape-token'})}));
    vi.stubGlobal('MercadoPago',vi.fn(function(){return{bricks:()=>({create}),yape};}));
    TestBed.configureTestingModule({imports:[Checkout],providers:[provideRouter([]),{provide:CheckoutService,useValue:api},{provide:CartService,useValue:cart}]});
    fixture=TestBed.createComponent(Checkout);component=fixture.componentInstance;fixture.detectChanges();
    component.customer.email='buyer@example.test';component.paymentMethodId='method-1';component.currentStep.set(4);
    component.validation.set({valid:true,requiresAuthentication:false,total:100,subtotal:100,shipping:0,currency:'PEN',paymentMethod:'Tarjeta',delivery:'Entrega',messages:[]});fixture.detectChanges();
  });
  afterEach(()=>{TestBed.resetTestingModule();vi.unstubAllGlobals();});
  it('uses cardPayment and sends the Orders payment type without a preference or frontend amount',async()=>{
    component.pagarConMercadoPago();await fixture.whenStable();
    expect(create.mock.calls[0][0]).toBe('cardPayment');const settings=create.mock.calls[0][2];expect(settings.initialization).not.toHaveProperty('preferenceId');
    await settings.callbacks.onSubmit({token:'token',payment_method_id:'visa',installments:1,payer:{email:'payer@example.test'}},{paymentTypeId:'credit_card'});
    expect(api.procesarCargo).toHaveBeenCalledWith('pedido-1','payer@example.test',expect.objectContaining({paymentMethodId:'visa',paymentTypeId:'credit_card'}));
    expect(api.procesarCargo.mock.calls[0][2]).not.toHaveProperty('amount');expect(component.pendingPayment()).toBeTruthy();expect(component.paymentSuccess()).toBeNull();
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
    const submit=create.mock.calls[0][2].callbacks.onSubmit;await expect(submit({token:'token',payment_method_id:'visa'},{paymentTypeId:'credit_card'})).rejects.toThrow();
    await expect(submit({token:'token2',payment_method_id:'visa'},{paymentTypeId:'credit_card'})).rejects.toThrow();expect(api.procesarCargo).toHaveBeenCalledTimes(1);expect(component.paymentUncertain()).toBe(true);
    component.checkPayment();expect(api.consultarPago).toHaveBeenCalledWith('pedido-1');
  });
  it('keeps the paid receipt visible after the cart empties and destroys the Brick',async()=>{
    component.pagarConMercadoPago();await fixture.whenStable();api.procesarCargo.mockReturnValue(of({...pending,exitoso:true,pendiente:false,estadoPedido:'PAGADO'}));
    await create.mock.calls[0][2].callbacks.onSubmit({token:'t',payment_method_id:'visa'},{paymentTypeId:'credit_card'});cart.cart.set({items:[],subtotal:0});fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Gracias por tu compra');expect(fixture.nativeElement.textContent).not.toContain('Tu carrito está vacío');expect(unmount).toHaveBeenCalled();
  });
});
