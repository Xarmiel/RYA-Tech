import {CurrencyPipe} from '@angular/common';
import {AfterViewInit,ChangeDetectorRef,Component,DestroyRef,ElementRef,OnDestroy,inject,input,output,signal,HostListener} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {firstValueFrom,timeout} from 'rxjs';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {CheckoutService} from './checkout.service';
import {IniciarPagoResponse,OrdersPaymentData,SavedPaymentCard} from './checkout.models';
@Component({selector:'app-card-checkout',imports:[FormsModule,CurrencyPipe],templateUrl:'./card-checkout.html',styleUrl:'./card-checkout.scss'})
export class CardCheckout implements AfterViewInit,OnDestroy {
 readonly session=input.required<IniciarPagoResponse>();readonly email=input('');readonly disabled=input(false);
 readonly pay=output<OrdersPaymentData>();readonly busyChange=output<boolean>();
 private readonly api=inject(CheckoutService);private readonly cdr=inject(ChangeDetectorRef);private readonly element=inject(ElementRef);private readonly destroyRef=inject(DestroyRef);
 protected readonly cards=signal<SavedPaymentCard[]>([]);protected readonly selected=signal<SavedPaymentCard|null>(null);
 protected readonly loading=signal(true);protected readonly busy=signal(false);protected readonly error=signal('');protected readonly drawer=signal(false);protected readonly uncertain=signal(false);
 protected remember=false;private mp:any;private cardForm:any;private cvv:any;private methods:any[]=[];private destroyed=false;private generation=0;private returnFocus:HTMLElement|null=null;
 ngAfterViewInit():void{void this.refresh();}
 ngOnDestroy():void{this.destroyed=true;this.generation++;this.cleanup();}
 private cleanup():void{try{this.cardForm?.unmount();this.cvv?.unmount();}catch{}this.cardForm=null;this.cvv=null;}
 private sdk():any{if(!this.mp){const ctor=(globalThis as any).MercadoPago;if(!ctor)throw new Error('No se pudo cargar el formulario seguro. Revisa tu conexión.');this.mp=new ctor(this.session().publicKey,{locale:'es-PE'});}return this.mp;}
 private setBusy(value:boolean):void{this.busy.set(value);this.busyChange.emit(value);}
 protected async refresh():Promise<void>{
  if(this.busy()||this.disabled())return;this.loading.set(true);this.error.set('');
  try{const cards=await firstValueFrom(this.api.savedCards().pipe(timeout(20000),takeUntilDestroyed(this.destroyRef)));if(this.destroyed)return;
   this.cards.set(cards);this.uncertain.set(false);this.loading.set(false);this.choose(cards[0]??null);
  }catch{if(!this.destroyed){this.loading.set(false);this.error.set('No pudimos consultar tus tarjetas guardadas. Puedes reintentar o pagar con otra sin guardarla.');}}
 }
 protected choose(card:SavedPaymentCard|null):void{
  if(this.disabled()||this.busy())return;this.generation++;this.cleanup();this.selected.set(card);this.drawer.set(false);this.cdr.detectChanges();
  if(card){try{this.cvv=this.sdk().fields.create('securityCode',{placeholder:'CVV',style:this.fieldStyle()}).mount('rya-saved-cvv');this.cvv.update?.({settings:{mode:'mandatory',length:card.securityCodeLength===4?4:3}});}catch{this.error.set('No se pudo preparar el CVV seguro. Selecciona la tarjeta nuevamente.');}}
 }
 protected openNew():void{
  if(this.disabled()||this.busy()||this.uncertain())return;this.returnFocus=document.activeElement as HTMLElement;this.generation++;this.cleanup();this.selected.set(null);this.remember=false;this.drawer.set(true);this.error.set('');this.cdr.detectChanges();
  const generation=this.generation;
  try{
   const sdk=this.sdk();const fields:any={id:'rya-new-card'};
   for(const [key,id,placeholder] of [['cardNumber','number','Número de tarjeta'],['expirationDate','expiration','MM/AA'],['securityCode','cvv','CVV'],['cardholderName','name','Nombre del titular'],['cardholderEmail','email','Correo electrónico'],['issuer','issuer','Banco emisor'],['installments','installments','Cuotas'],['identificationType','doc-type','Tipo de documento'],['identificationNumber','doc','Número de documento']])fields[key]={id:'rya-card-'+id,placeholder,...(['cardNumber','expirationDate','securityCode'].includes(key)?{style:this.fieldStyle()}: {})};
   this.cardForm=sdk.cardForm({amount:String(this.session().total),iframe:true,form:fields,callbacks:{
    onFormMounted:(error:any)=>{if(!this.destroyed&&generation===this.generation&&error)this.error.set('No se pudo preparar la tarjeta. Cierra y vuelve a abrir el formulario.');},
    onPaymentMethodsReceived:(_error:any,data:any)=>{if(generation===this.generation)this.methods=data?.results??[];},
    onError:()=>{if(!this.destroyed&&generation===this.generation){this.setBusy(false);this.error.set('Revisa los datos de la tarjeta y vuelve a intentarlo.');}},
    onSubmit:(event:Event)=>{event.preventDefault();if(generation===this.generation)void this.submitNew();}
   }});
   this.element.nativeElement.querySelector('.card-drawer h3')?.focus();
  }catch(error:any){this.error.set(error.message||'No se pudo cargar el formulario seguro.');}
 }
 private fieldStyle():any{return {color:getComputedStyle(this.element.nativeElement).getPropertyValue('--text').trim()||'#111827',fontSize:'16px',fontFamily:'Arial'};}
 protected closeDrawer():void{
  if(this.busy()||this.disabled())return;this.generation++;this.cleanup();this.drawer.set(false);this.cdr.detectChanges();this.returnFocus?.focus();
  if(this.cards().length)this.choose(this.cards()[0]);
 }
 @HostListener('document:keydown',['$event']) protected keyboard(event:KeyboardEvent):void{
  if(!this.drawer())return;
  if(event.key==='Escape'){event.preventDefault();this.closeDrawer();}
  if(event.key==='Tab'){
   const focusable=Array.from(this.element.nativeElement.querySelectorAll('.card-drawer button:not(:disabled),.card-drawer input:not(:disabled),.card-drawer select:not(:disabled),.card-drawer iframe')) as HTMLElement[];
   const first=focusable[0],last=focusable[focusable.length-1];
   if(event.shiftKey&&(document.activeElement===first||!focusable.includes(document.activeElement as HTMLElement))){event.preventDefault();last?.focus();}
   else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first?.focus();}
  }
 }
 private async submitNew():Promise<void>{
  if(this.busy()||this.disabled()||this.uncertain())return;
  const form=this.cardForm?.getCardFormData();if(!form?.token){this.error.set('Completa los datos de la tarjeta.');return;}
  const type=this.methods.find(m=>m.id===form.paymentMethodId)?.payment_type_id;
  if(!['credit_card','debit_card'].includes(type)){this.error.set('Selecciona una tarjeta de débito o crédito compatible.');return;}
  this.error.set('');
  if(this.remember){
   this.setBusy(true);
   try{const card=await firstValueFrom(this.api.saveCard(form.token).pipe(timeout(20000),takeUntilDestroyed(this.destroyRef)));if(this.destroyed)return;
    this.setBusy(false);this.cards.update(cards=>[card,...cards.filter(c=>c.id!==card.id)]);this.choose(card);this.error.set('Tarjeta guardada. Ingresa nuevamente el CVV para autorizar este pago.');
   }catch{if(!this.destroyed){this.setBusy(false);this.uncertain.set(true);this.generation++;this.cleanup();this.drawer.set(false);this.error.set('No se confirmó el guardado. Actualiza tus tarjetas antes de volver a guardar.');}}
  }else{
   this.pay.emit({token:form.token,paymentMethodId:form.paymentMethodId,paymentTypeId:type,installments:Number(form.installments)||1,identificationType:form.identificationType,identificationNumber:form.identificationNumber});
  }
 }
 protected async paySaved():Promise<void>{
  const card=this.selected();if(!card||this.busy()||this.disabled()||!this.cvv)return;
  this.setBusy(true);this.error.set('');
  try{const token=await this.sdk().fields.createCardToken({cardId:card.id});if(this.destroyed)return;this.setBusy(false);
   this.pay.emit({savedCardId:card.id,token:token.id,paymentMethodId:card.paymentMethodId,paymentTypeId:card.paymentTypeId,installments:1});
  }catch{if(!this.destroyed){this.setBusy(false);this.error.set('Revisa el CVV de tu tarjeta e inténtalo nuevamente.');}}
 }
}
