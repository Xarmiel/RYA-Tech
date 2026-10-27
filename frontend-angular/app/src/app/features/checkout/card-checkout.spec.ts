import {TestBed} from '@angular/core/testing';
import {registerLocaleData} from '@angular/common';
import locale from '@angular/common/locales/es-PE';
import {of,throwError} from 'rxjs';
import {beforeEach,afterEach,it,describe,expect,vi} from 'vitest';
import {CardCheckout} from './card-checkout';
import {CheckoutService} from './checkout.service';
registerLocaleData(locale);
describe('Secure card checkout',()=>{
 let fixture:any,component:any,api:any,settings:any,form:any,cvv:any,sdk:any,pay:any;
 const card={id:'saved-1',lastFour:'1234',brand:'Visa',paymentMethodId:'visa',paymentTypeId:'debit_card',expirationMonth:12,expirationYear:2035,securityCodeLength:3};
 beforeEach(async()=>{
  api={savedCards:vi.fn(()=>of([card])),saveCard:vi.fn(()=>of(card))};cvv={unmount:vi.fn()};
  form={unmount:vi.fn(),getCardFormData:()=>({token:'one-time-token',paymentMethodId:'visa',installments:'1',identificationType:'DNI',identificationNumber:'12345678'})};
  sdk={cardForm:vi.fn((s:any)=>{settings=s;return form;}),fields:{create:vi.fn(()=>({mount:()=>cvv})),createCardToken:vi.fn(()=>Promise.resolve({id:'fresh-token'}))}};
  vi.stubGlobal('MercadoPago',vi.fn(function(){return sdk;}));
  TestBed.configureTestingModule({imports:[CardCheckout],providers:[{provide:CheckoutService,useValue:api}]});
  fixture=TestBed.createComponent(CardCheckout);component=fixture.componentInstance;
  fixture.componentRef.setInput('session',{publicKey:'public',total:100});fixture.componentRef.setInput('email','buyer@test.test');pay=vi.fn();component.pay.subscribe(pay);
  fixture.detectChanges();await fixture.whenStable();fixture.detectChanges();
 });
 afterEach(()=>{TestBed.resetTestingModule();vi.unstubAllGlobals();});
 it('displays only masked numbers and obtains a fresh CVV token for every payment',async()=>{
  expect(fixture.nativeElement.textContent).toContain('XXXX-XXXX-XXXX-1234');await component.paySaved();
  expect(sdk.fields.createCardToken).toHaveBeenCalledWith({cardId:'saved-1'});expect(pay).toHaveBeenCalledWith(expect.objectContaining({savedCardId:'saved-1',token:'fresh-token',paymentTypeId:'debit_card'}));expect(api.saveCard).not.toHaveBeenCalled();
 });
 it('uses iframe fields for new cards and never saves without consent',async()=>{
  component.openNew();expect(settings.iframe).toBe(true);expect(fixture.nativeElement.querySelector('#rya-card-number').tagName).toBe('DIV');
  settings.callbacks.onPaymentMethodsReceived(null,{results:[{id:'visa',payment_type_id:'debit_card'}]});await component.submitNew();
  expect(api.saveCard).not.toHaveBeenCalled();expect(pay).toHaveBeenCalledWith(expect.objectContaining({token:'one-time-token'}));
 });
 it('saves only with consent then requires another CVV token before charging',async()=>{
  component.openNew();settings.callbacks.onPaymentMethodsReceived(null,{results:[{id:'visa',payment_type_id:'debit_card'}]});component.remember=true;
  await component.submitNew();expect(api.saveCard).toHaveBeenCalledWith('one-time-token');expect(pay).not.toHaveBeenCalled();expect(component.drawer()).toBe(false);expect(form.unmount).toHaveBeenCalled();
  await component.paySaved();expect(pay).toHaveBeenCalledWith(expect.objectContaining({token:'fresh-token'}));
 });
 it('blocks repetition after an uncertain save until cards are refreshed',async()=>{
  api.saveCard.mockReturnValue(throwError(()=>({status:0})));component.openNew();settings.callbacks.onPaymentMethodsReceived(null,{results:[{id:'visa',payment_type_id:'debit_card'}]});component.remember=true;
  await component.submitNew();expect(component.uncertain()).toBe(true);component.openNew();expect(component.drawer()).toBe(false);expect(pay).not.toHaveBeenCalled();
  await component.refresh();expect(component.uncertain()).toBe(false);expect(component.selected()?.id).toBe(card.id);
 });
 it('disables card changes and submission while a charge is running',async()=>{
  fixture.componentRef.setInput('disabled',true);fixture.detectChanges();component.openNew();await component.paySaved();expect(pay).not.toHaveBeenCalled();expect(component.drawer()).toBe(false);
 });
});
