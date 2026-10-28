import {Component,DestroyRef,OnInit,inject,signal,output} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {DialogFocus} from '../../shared/dialog-focus/dialog-focus';
import {NotificationService} from '../../core/notifications/notification.service';
export interface ShippingTariff {id?:string;nombreServicio:string;modalidad:'DOMICILIO'|'RECOJO';departamento:string;provincia:string;distrito:string;costo:number;plazoDias:number;direccionRecojo:string;horarioRecojo:string;activo:boolean;}
interface Dispatch {id:string;codigo:string;nombre_completo:string;modalidad_entrega:string;direccion_envio:string;referencia_envio:string;entrega_snapshot:string;estado_entrega:string;transportista_seguimiento:string;codigo_seguimiento:string;updated_at:string;}
@Component({selector:'app-shipping-admin',imports:[CommonModule,FormsModule,DialogFocus],templateUrl:'./shipping-admin.html',styleUrl:'./shipping-admin.scss'})
export class ShippingAdmin implements OnInit {
  private http=inject(HttpClient);private destroy=inject(DestroyRef);private notifications=inject(NotificationService);
  readonly busyChange=output<boolean>();readonly tariffs=signal<ShippingTariff[]>([]);readonly orders=signal<Dispatch[]>([]);
  readonly loading=signal(false);readonly error=signal('');readonly saving=signal(false);readonly editing=signal(false);
  readonly dispatchSaving=signal('');readonly dispatchError=signal('');readonly dispatchLoading=signal(false);readonly ordersError=signal('');
  page=0;total=0;form=this.empty();private baseline='';private trackingBaseline='';tracking:Dispatch|null=null;
  private empty():ShippingTariff{return {nombreServicio:'',modalidad:'DOMICILIO',departamento:'',provincia:'',distrito:'',costo:0,plazoDias:1,direccionRecojo:'',horarioRecojo:'',activo:true};}
  ngOnInit(){this.load();this.loadOrders();}
  load(){this.loading.set(true);this.error.set('');this.http.get<ShippingTariff[]>('/api/v1/admin/envios').pipe(takeUntilDestroyed(this.destroy)).subscribe({next:rows=>{this.tariffs.set(rows);this.loading.set(false);},error:e=>{this.error.set(e?.error?.message??'No pudimos cargar las tarifas.');this.loading.set(false);}});}
  loadOrders(){this.dispatchLoading.set(true);this.ordersError.set('');this.http.get<{items:Dispatch[];total:number}>('/api/v1/admin/envios/pedidos',{params:{page:this.page}}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:res=>{this.orders.set(res.items);this.total=res.total;this.dispatchLoading.set(false);},error:e=>{this.ordersError.set(e?.error?.message??'No pudimos cargar los pedidos.');this.dispatchLoading.set(false);}});}
  edit(row?:ShippingTariff){if(!this.canLeave())return;this.tracking=null;this.form=row?{...this.empty(),...row}:this.empty();this.baseline=JSON.stringify(this.form);this.editing.set(true);this.error.set('');}
  cancel(){if(this.canLeave()){this.editing.set(false);this.form=this.empty();}}
  save(){if(this.saving())return;this.saving.set(true);this.busyChange.emit(true);this.error.set('');const data={...this.form,costo:this.form.modalidad==='RECOJO'?0:this.form.costo};
    const request=data.id?this.http.put<ShippingTariff>('/api/v1/admin/envios/'+data.id,data):this.http.post<ShippingTariff>('/api/v1/admin/envios',data);
    request.pipe(takeUntilDestroyed(this.destroy)).subscribe({next:()=>{this.saving.set(false);this.busyChange.emit(false);this.editing.set(false);this.notifications.show('Tarifa guardada.','success');this.load();},error:e=>{this.saving.set(false);this.busyChange.emit(false);this.error.set(e?.error?.message??'No se confirmó el guardado. Actualiza las tarifas antes de volver a crear una.');}});
  }
  openTracking(order:Dispatch){if(!this.canLeave())return;this.editing.set(false);this.tracking={...order,estado_entrega:order.estado_entrega||'PENDIENTE',transportista_seguimiento:order.transportista_seguimiento||'',codigo_seguimiento:order.codigo_seguimiento||''};this.trackingBaseline=JSON.stringify(this.tracking);this.dispatchError.set('');}
  closeTracking(){if(this.canLeave())this.tracking=null;}
  states(order:Dispatch){return order.modalidad_entrega==='RECOJO'?['PENDIENTE','PREPARANDO','LISTO_RECOJO','RETIRADO']:['PENDIENTE','PREPARANDO','ENVIADO','ENTREGADO'];}
  track(){const order=this.tracking;if(!order||this.dispatchSaving())return;this.dispatchSaving.set(order.id);this.busyChange.emit(true);this.dispatchError.set('');
    this.http.put('/api/v1/admin/envios/pedidos/'+order.id+'/seguimiento',{estado:order.estado_entrega,transportista:order.transportista_seguimiento,codigo:order.codigo_seguimiento,version:order.updated_at}).pipe(takeUntilDestroyed(this.destroy)).subscribe({next:()=>{this.dispatchSaving.set('');this.busyChange.emit(false);this.tracking=null;this.loadOrders();this.notifications.show('Seguimiento guardado.','success');},error:e=>{this.dispatchSaving.set('');this.busyChange.emit(false);this.dispatchError.set(e?.error?.message??'No se pudo guardar el seguimiento.');}});
  }
  navigate(delta:number){if(!this.canLeave())return;this.page+=delta;this.loadOrders();}
  hasPendingChanges(){return this.saving()||!!this.dispatchSaving()||(this.editing()&&JSON.stringify(this.form)!==this.baseline)||!!(this.tracking&&JSON.stringify(this.tracking)!==this.trackingBaseline);}
  canLeave(){if(this.saving()||this.dispatchSaving())return false;return !this.hasPendingChanges()||window.confirm('Hay cambios de envío sin guardar. ¿Quieres descartarlos?');}
}
