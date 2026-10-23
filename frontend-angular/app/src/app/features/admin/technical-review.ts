import { Component, DestroyRef, OnChanges, SimpleChanges, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { JsonPipe, DatePipe } from '@angular/common';
import { Observable, Subscription, finalize, timeout } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService } from '../../core/auth/auth.service';
import { TechnicalDraftApi, TechnicalDraftDetail, DraftFicha, DraftEvent } from './technical-draft-api';
import { TECHNICAL_MODEL } from './technical-model';

@Component({selector:'app-technical-review',imports:[FormsModule,JsonPipe,DatePipe],templateUrl:'./technical-review.html',styleUrl:'./technical-review.scss'})
export class TechnicalReview implements OnChanges {
  readonly draft=input.required<TechnicalDraftDetail>();readonly disabled=input(false);
  readonly changed=output<TechnicalDraftDetail>();readonly working=output<boolean>();readonly unresolved=output<number>();
  private readonly api=inject(TechnicalDraftApi);private readonly auth=inject(AuthService);private readonly destroy=inject(DestroyRef);
  readonly busy=signal(false);readonly reading=signal(false);readonly error=signal('');readonly notice=signal('');
  readonly ficha=signal<DraftFicha|null>(null);readonly events=signal<DraftEvent[]>([]);readonly snapshot=signal<unknown>(null);
  readonly historyOpened=signal(false);readonly historyPage=signal(0);readonly historyPages=signal(0);
  comment='';private read?:Subscription;
  ngOnChanges(changes:SimpleChanges){if(changes['draft']){this.read?.unsubscribe();this.ficha.set(null);this.events.set([]);this.snapshot.set(null);this.historyOpened.set(false);this.error.set('');if(changes['draft'].previousValue?.id!==this.draft().id)this.notice.set('');this.comment='';}}
  owner(){return this.auth.user()?.id===this.draft().createdBy;}
  available(){return !this.disabled()&&!this.busy()&&!this.reading();}
  compare(){if(!this.available())return;this.error.set('');this.ficha.set(null);this.reading.set(true);
    this.read=this.api.ficha(this.draft().id).pipe(timeout(30000),takeUntilDestroyed(this.destroy),finalize(()=>this.reading.set(false)))
      .subscribe({next:value=>this.ficha.set(value),error:err=>this.error.set(err?.error?.message||'No se pudo consultar la ficha. Reintenta sin modificar el borrador.')});}
  link(unlink=false){if(!this.available()||!this.owner()||this.draft().state!=='DRAFT'||(!unlink&&!this.ficha()))return;
    this.write(this.api.link(this.draft(),unlink),'Vínculo actualizado; la ficha real no se modificó.');}
  submit(){if(!this.available()||!this.owner()||this.draft().state!=='DRAFT')return;
    if(!window.confirm('¿Enviar esta versión guardada a revisión independiente? No se publicará ni modificará la ficha real.'))return;
    this.write(this.api.submit(this.draft()),'Enviado a revisión. Se necesita otro administrador para aprobar o rechazar.');}
  decide(decision:'APPROVED'|'REJECTED'){
    if(!this.available()||this.owner()||this.draft().state!=='IN_REVIEW')return;
    if(!this.comment.trim()){this.error.set('Indica el motivo de tu decisión.');return;}
    if(!window.confirm(decision==='APPROVED'?'¿Aprobar este contenido? No verifica compatibilidad ni aplica datos sobre la ficha real.':'¿Rechazar este contenido con el motivo indicado?'))return;
    this.write(this.api.decide(this.draft(),decision,this.comment.trim()),'Decisión registrada en el historial.');
  }
  reopen(){if(!this.available()||!this.owner()||!['APPROVED','REJECTED'].includes(this.draft().state))return;
    if(!window.confirm('¿Reabrir para corregir? Los cambios deberán pasar por una nueva revisión.'))return;
    this.write(this.api.reopen(this.draft(),this.comment.trim()),'Borrador reabierto para corrección.');}
  private write(request:Observable<TechnicalDraftDetail>,message:string){
    this.busy.set(true);this.working.emit(true);this.error.set('');this.notice.set('');
    request.pipe(timeout(45000),takeUntilDestroyed(this.destroy),finalize(()=>{this.busy.set(false);this.working.emit(false);}))
      .subscribe({next:value=>{this.changed.emit(value);this.notice.set(message);},error:err=>{
        const status=err?.status??0;this.error.set(err?.error?.message||'No se pudo confirmar la operación. Recarga el borrador antes de repetir.');
        if(Array.isArray(err?.error?.issues))this.error.update(message=>message+' '+err.error.issues.join(' '));
        if(status===409||!status||status>=500||[408,429].includes(status))this.unresolved.emit(status);
      }});
  }
  history(page=0){if(!this.available()||page<0)return;this.error.set('');this.events.set([]);this.snapshot.set(null);this.historyOpened.set(false);this.reading.set(true);
    this.read=this.api.history(this.draft().id,page).pipe(timeout(30000),takeUntilDestroyed(this.destroy),finalize(()=>this.reading.set(false)))
      .subscribe({next:value=>{this.events.set(value.content);this.historyPage.set(value.page);this.historyPages.set(value.totalPages);this.historyOpened.set(true);},error:()=>this.error.set('No se pudo cargar el historial. Reintenta.')});}
  version(version:number){if(!this.available())return;this.snapshot.set(null);this.error.set('');this.reading.set(true);
    this.read=this.api.revision(this.draft().id,version).pipe(timeout(30000),takeUntilDestroyed(this.destroy),finalize(()=>this.reading.set(false)))
      .subscribe({next:value=>this.snapshot.set(value),error:()=>this.error.set('No se pudo recuperar esa versión. Reintenta.')});}
  value(value:unknown){return value===null||value===undefined?'Sin dato':typeof value==='boolean'?(value?'Sí':'No'):String(value);}
  fieldLabel(key:string){const product=this.draft().document['product'] as Record<string,unknown>|undefined;return TECHNICAL_MODEL.types[String(product?.['type'])]?.fields.find(field=>field.id===key)?.label??key;}
  sourceLink(value:string|null){try{const url=new URL(value??'');return url.protocol==='https:'&&!url.username&&!url.password?url.href:null;}catch{return null;}}
  status(status:string){return ({SAME:'Coincide',DIFFERENT:'Diferente',NEW:'Dato nuevo',NOT_PROPOSED:'Sin propuesta'} as Record<string,string>)[status]??status;}
}
