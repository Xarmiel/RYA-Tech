import {Component,OnChanges,SimpleChanges,DestroyRef,HostListener,inject,input,output,signal} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {HttpClient} from '@angular/common/http';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import {timeout} from 'rxjs';
import {TechnicalSchema} from './product-technical.models';
export interface CustomTechnicalField {id:string;categoriaId:string;categoria:string;clave:string;nombre:string;tipoDato:'text'|'number'|'boolean';unidad:string|null;ejemplo:string|null;soloEnteros:boolean;version:number;heredado:boolean;}
interface Result {campos:CustomTechnicalField[];esquema:TechnicalSchema|null;}
@Component({selector:'app-technical-field-manager',imports:[FormsModule],templateUrl:'./technical-field-manager.html',styleUrl:'./technical-field-manager.scss'})
export class TechnicalFieldManager implements OnChanges {
 readonly categoryId=input('');readonly disabled=input(false);readonly changed=output<{categoryId:string;schema:TechnicalSchema|null}>();readonly busyChange=output<boolean>();
 private http=inject(HttpClient);private destroy=inject(DestroyRef);private request=0;private baseline='';
 readonly opened=signal(false);readonly busy=signal(false);readonly loaded=signal(false);readonly fields=signal<CustomTechnicalField[]>([]);readonly error=signal('');readonly message=signal('');readonly uncertain=signal(false);
 form=this.empty();editingId='';
 private empty(){return {nombre:'',tipoDato:'text' as 'text'|'number'|'boolean',unidad:'',ejemplo:'',soloEnteros:false,version:null as number|null};}
 ngOnChanges(changes:SimpleChanges){if(changes['categoryId']){this.request++;this.opened.set(false);this.fields.set([]);this.loaded.set(false);this.reset();this.error.set('');this.message.set('');this.uncertain.set(false);}}
 private endpoint(){return '/api/v1/admin/categorias/'+encodeURIComponent(this.categoryId())+'/campos-ficha';}
 toggle(){if(this.disabled()||this.busy()||!this.categoryId())return;if(this.opened()){if(this.canLeave())this.opened.set(false);}else{this.opened.set(true);this.load();}}
 load(){if(this.busy()||!this.categoryId())return;const seq=++this.request;this.setBusy(true);this.error.set('');this.http.get<Result>(this.endpoint()).pipe(timeout(20000),takeUntilDestroyed(this.destroy)).subscribe({next:res=>{if(seq!==this.request)return;this.accept(res);this.uncertain.set(false);this.setBusy(false);},error:e=>{if(seq!==this.request)return;this.loaded.set(false);this.error.set(e?.error?.message??'No pudimos consultar los campos. Comprueba la conexión y que se aplicó la actualización de la BD.');this.setBusy(false);}});}
 private accept(res:Result){this.fields.set(res.campos);this.loaded.set(true);this.changed.emit({categoryId:this.categoryId(),schema:res.esquema});}
 private setBusy(value:boolean){this.busy.set(value);this.busyChange.emit(value);}
 reset(){this.form=this.empty();this.editingId='';this.baseline=JSON.stringify(this.form);}
 edit(field:CustomTechnicalField){if(this.busy()||this.disabled()||field.heredado||!this.canLeave())return;this.form={nombre:field.nombre,tipoDato:field.tipoDato,unidad:field.unidad??'',ejemplo:field.ejemplo??'',soloEnteros:field.soloEnteros,version:field.version};this.editingId=field.id;this.baseline=JSON.stringify(this.form);this.message.set('');this.error.set('');}
 changeType(){if(this.editingId)return;this.form.unidad='';this.form.ejemplo='';this.form.soloEnteros=false;}
 save(){if(this.busy()||this.disabled()||!this.loaded()||this.uncertain())return;this.error.set('');if(!this.form.nombre.trim()){this.error.set('Indica el nombre del campo.');return;}const seq=++this.request;const body={...this.form,nombre:this.form.nombre.trim(),unidad:this.form.unidad.trim()||null,ejemplo:String(this.form.ejemplo??'').trim()||null};this.setBusy(true);
 const req=this.editingId?this.http.put<Result>(this.endpoint()+'/'+this.editingId,body):this.http.post<Result>(this.endpoint(),body);
 req.pipe(timeout(20000),takeUntilDestroyed(this.destroy)).subscribe({next:res=>{if(seq!==this.request)return;this.accept(res);this.reset();this.message.set('Campo guardado. Cada producto podrá tener su propio valor; los ejemplos no se copian como datos.');this.setBusy(false);},error:e=>{if(seq!==this.request)return;this.uncertain.set(!e?.status||e.status>=500);const details=Object.entries(e?.error?.validationErrors??{}).map(([key,value])=>key+': '+value);this.error.set([e?.error?.message??'No se confirmó el guardado. Actualiza el listado antes de reintentar.',...details].join(' · '));this.setBusy(false);}});
 }
 hasPendingChanges(){return this.busy()||(this.opened()&&JSON.stringify(this.form)!==this.baseline);}
 canLeave(){if(this.busy())return false;if(!this.hasPendingChanges())return true;if(!window.confirm('Hay un campo técnico sin guardar. ¿Quieres descartarlo?'))return false;this.reset();return true;}
 @HostListener('window:beforeunload',['$event']) beforeUnload(event:BeforeUnloadEvent){if(this.hasPendingChanges()){event.preventDefault();event.returnValue='';}}
}
