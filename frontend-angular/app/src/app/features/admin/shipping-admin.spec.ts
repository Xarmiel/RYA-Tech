import {TestBed} from '@angular/core/testing';
import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting,HttpTestingController} from '@angular/common/http/testing';
import {registerLocaleData} from '@angular/common';
import locale from '@angular/common/locales/es-PE';
import {describe,it,beforeEach,afterEach,expect} from 'vitest';
import {ShippingAdmin} from './shipping-admin';
registerLocaleData(locale);
describe('Shipping administration',()=>{
 let fixture:any,component:any,http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({imports:[ShippingAdmin],providers:[provideHttpClient(),provideHttpClientTesting()]});fixture=TestBed.createComponent(ShippingAdmin);component=fixture.componentInstance;http=TestBed.inject(HttpTestingController);fixture.detectChanges();http.expectOne('/api/v1/admin/envios').flush([]);http.expectOne('/api/v1/admin/envios/pedidos?page=0').flush({items:[],total:0});fixture.detectChanges();});
 afterEach(()=>{http.verify();TestBed.resetTestingModule();});
 it('saves a pickup point using the backend and refreshes confirmed tariffs',()=>{
   component.edit();component.form={...component.form,nombreServicio:'Tienda',modalidad:'RECOJO',direccionRecojo:'Av. Prueba 123',horarioRecojo:'9 a 18',costo:50};component.save();
   const req=http.expectOne('/api/v1/admin/envios');expect(req.request.method).toBe('POST');expect(req.request.body.costo).toBe(0);expect(component.tariffs()).toEqual([]);
   req.flush({...req.request.body,id:'point'});http.expectOne('/api/v1/admin/envios').flush([{...req.request.body,id:'point'}]);expect(component.tariffs()[0].id).toBe('point');expect(component.editing()).toBe(false);
 });
 it('retains the form and reports a failed save without inventing a saved record',()=>{
   component.edit();component.form.nombreServicio='Pendiente';component.save();http.expectOne('/api/v1/admin/envios').flush({message:'Falta dirección'},{status:400,statusText:'Bad Request'});
   expect(component.error()).toBe('Falta dirección');expect(component.form.nombreServicio).toBe('Pendiente');expect(component.editing()).toBe(true);expect(component.tariffs()).toEqual([]);
 });
 it('sends the original version for tracking and preserves edits on conflict',()=>{
   component.openTracking({id:'order',estado_entrega:'PREPARANDO',modalidad_entrega:'DOMICILIO',updated_at:'2026-10-27T12:00:00Z'});component.tracking.estado_entrega='ENVIADO';component.tracking.transportista_seguimiento='Courier';component.tracking.codigo_seguimiento='ABC';component.track();
   const req=http.expectOne('/api/v1/admin/envios/pedidos/order/seguimiento');expect(req.request.body.version).toBe('2026-10-27T12:00:00Z');expect(req.request.body.codigo).toBe('ABC');req.flush({message:'El pedido cambió'},{status:409,statusText:'Conflict'});expect(component.dispatchError()).toBe('El pedido cambió');expect(component.tracking.codigo_seguimiento).toBe('ABC');
 });
});
