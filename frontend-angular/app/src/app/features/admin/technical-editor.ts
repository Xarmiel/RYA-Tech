import { Component, DestroyRef, ElementRef, HostListener, Injector, ViewChild, afterNextRender, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TECHNICAL_MODEL, TechnicalField } from './technical-model';
import { ProductMedia, imageUrl } from './product-media';
import { TechnicalDraftApi, TechnicalDraftDetail, TechnicalDraftSummary } from './technical-draft-api';
import { AuthService } from '../../core/auth/auth.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription, finalize, timeout } from 'rxjs';
import { TechnicalReview } from './technical-review';

interface TypeDraft { values:Record<string,string>; records:Record<string,Record<string,string>[]>; use:string; source:string; date:string; notes:string; }
const RECORD_REQUIRED:Record<string,string[]> = {
  memory:['type','speed_mhz','source_url'], benchmarks:['application','score','unit','environment','source_url'],
  bios:['revision','version','date','source_url'], cpu_board:['cpu_sku','board_sku','revision','minimum_bios','source_url'], connectors:['connector','quantity'],
};

@Component({selector:'app-technical-editor',imports:[FormsModule,ProductMedia,TechnicalReview],templateUrl:'./technical-editor.html',styleUrl:'./technical-editor.scss'})
export class TechnicalEditor {
  @ViewChild(ProductMedia) media!:ProductMedia;
  readonly isDesignMode=input(false);
  readonly types=Object.entries(TECHNICAL_MODEL.types).map(([key,value])=>({key,value}));
  readonly groups=TECHNICAL_MODEL.groups;
  type='cpu';use='pending';category='';categorySlug='';categoryParent='';section='hardware';source='';date='';notes='';
  product:Record<string,string>={};values:Record<string,string>={};records:Record<string,Record<string,string>[]>={};
  private drafts:Record<string,TypeDraft>={};private previous='cpu';private destroyed=false;
  readonly importing=signal(false);
  private readonly element:ElementRef<HTMLElement>=inject(ElementRef);
  private readonly injector=inject(Injector);
  private readonly destroyRef=inject(DestroyRef);
  constructor(){this.destroyRef.onDestroy(()=>this.destroyed=true);}
  readonly serverDraft=signal<TechnicalDraftDetail|null>(null);
  readonly saving=signal(false);readonly loadingDraft=signal(false);readonly listing=signal(false);
  readonly serverError=signal('');readonly serverNotice=signal('');readonly serverRows=signal<TechnicalDraftSummary[]>([]);
  readonly serverPage=signal(0);readonly serverPages=signal(0);readonly serverTotal=signal(0);readonly listOpened=signal(false);
  readonly uncertain=signal(false);readonly conflict=signal(false);
  readonly reviewBusy=signal(false);catalogProductId:string|null=null;
  serverQuery='';serverState='';private listSub?:Subscription;private baseline:string|null=null;
  private get api(){return this.injector.get(TechnicalDraftApi);}
  busy():boolean{return this.importing()||this.saving()||this.loadingDraft()||this.reviewBusy();}
  reviewChanged(value:TechnicalDraftDetail):void {
    this.serverDraft.set(value);this.catalogProductId=typeof value.document['catalogProductId']==='string'?value.document['catalogProductId']:null;
    this.baseline=this.fingerprint();
    this.serverRows.update(rows=>rows.map(row=>row.id===value.id?{...row,version:value.version,state:value.state,updatedAt:value.updatedAt}:row));
  }
  reviewUnresolved(status:number):void {if(status===409)this.conflict.set(true);else this.uncertain.set(true);}
  canEdit():boolean {
    const saved=this.serverDraft();
    return !saved||(saved.state==='DRAFT'&&saved.createdBy===this.injector.get(AuthService).user()?.id);
  }
  stateLabel(state:string):string {return ({DRAFT:'Borrador',IN_REVIEW:'En revisión',APPROVED:'Contenido aprobado',REJECTED:'Rechazado'} as Record<string,string>)[state]??'Estado desconocido';}
  private fingerprint():string{return JSON.stringify({catalogProductId:this.catalogProductId,type:this.type,use:this.use,product:this.product,values:this.values,records:this.records,
    category:this.category,slug:this.categorySlug,parent:this.categoryParent,section:this.section,source:this.source,date:this.date,notes:this.notes,media:this.media?.snapshot()??[]});}
  private hasOtherDrafts():boolean{return Object.entries(this.drafts).some(([type,d])=>type!==this.type&&
    (Object.values(d.values).some(v=>String(v??'').trim())||Object.values(d.records).some(rows=>rows.some(row=>Object.values(row).some(v=>String(v??'').trim())))||[d.source,d.date,d.notes].some(v=>!!v.trim())));}
  reviewed=signal('');error=signal('');issues=signal<string[]>([]);warnings=signal<string[]>([]);recoveryNotice=signal('');
  readonly commercialFields=[{key:'name',label:'Nombre',type:'text',required:true},{key:'sku',label:'SKU',type:'text',required:true},{key:'mpn',label:'MPN del fabricante',type:'text',required:false},{key:'brand',label:'Marca',type:'text',required:true},{key:'price',label:'Precio (S/)',type:'number',required:false},{key:'stock',label:'Stock',type:'number',required:false}];
  get definition(){return TECHNICAL_MODEL.types[this.type] ?? TECHNICAL_MODEL.types['cpu'];}
  get catalogOnly(){return this.definition.defaultUse==='catalog';}
  invalidate():void {this.reviewed.set('');this.error.set('');this.issues.set([]);this.warnings.set([]);}
  private focusResult(selector:string):void {afterNextRender(()=>{if(!this.destroyed)this.element.nativeElement.querySelector<HTMLElement>(selector)?.focus();},{injector:this.injector});}
  changeType():void {
    if(!Object.hasOwn(TECHNICAL_MODEL.types,this.type)){this.type=this.previous;return;}
    this.drafts[this.previous]={values:this.values,records:this.records,use:this.use,source:this.source,date:this.date,notes:this.notes};
    const draft=this.drafts[this.type];this.values=draft?.values??{};this.records=draft?.records??{};
    this.use=draft?.use??this.definition.defaultUse??'pending';this.source=draft?.source??'';this.date=draft?.date??'';this.notes=draft?.notes??'';
    this.previous=this.type;this.invalidate();
  }
  addRow(group:string):void {
    if(!this.definition.groups.includes(group))return;
    if((this.records[group]?.length??0)>=20){this.error.set('El límite es de 20 registros por grupo.');return;}
    (this.records[group]??=[]).push({});this.invalidate();
  }
  removeRow(group:string,index:number):void {this.records[group]?.splice(index,1);this.invalidate();}
  isDirty():boolean {
    if(this.busy()||this.uncertain())return true;
    if(this.baseline!==null)return this.baseline!==this.fingerprint()||this.hasOtherDrafts();
    const filled=(value:unknown):boolean=>typeof value==='string'?!!value.trim():Array.isArray(value)?value.some(filled):!!value&&typeof value==='object'?Object.values(value).some(filled):false;
    return this.importing()||filled(this.product)||filled(this.values)||filled(this.records)||Object.values(this.drafts).some(d=>filled(d.values)||filled(d.records)||[d.source,d.date,d.notes].some(v=>!!v.trim()))||
      [this.category,this.categorySlug,this.categoryParent,this.source,this.date,this.notes].some(value=>!!value.trim())||!!this.media?.images().length||!!this.media?.hasPendingUploads();
  }
  canLeave():boolean {if(this.busy())return false;return !this.isDirty()||window.confirm(this.uncertain()
    ?'El servidor pudo haber guardado el borrador, pero no se confirmó la respuesta. Revisa el listado antes de volver a crearlo. ¿Salir sin confirmar el resultado?'
    :'Hay cambios técnicos locales sin guardar. ¿Quieres salir y descartarlos? Puedes cancelar y descargar el JSON antes.');}
  @HostListener('window:beforeunload',['$event']) beforeUnload(event:BeforeUnloadEvent):void {if(this.isDirty()){event.preventDefault();event.returnValue='';}}
  private validDate(raw:string):boolean {
    if(!/^\d{4}-\d{2}-\d{2}$/.test(raw))return false;
    const date=new Date(raw+'T00:00:00Z'),today=new Date();
    const localToday=`${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`;
    return Number.isFinite(date.getTime())&&date.toISOString().slice(0,10)===raw&&raw<=localToday;
  }
  private parseField(field:TechnicalField,raw:unknown,label:string,issues:string[]):unknown {
    const value=TECHNICAL_MODEL.valueOf(field,raw);
    if(typeof value==='number'&&(!Number.isFinite(value)||value<0||(field.integer&&(!Number.isInteger(value)||value<1))))issues.push(`${label}: ${field.integer?'usa un entero mayor que cero':'usa un número no negativo'}.`);
    if(field.type==='boolean'&&raw!=null&&raw!==''&&value===null)issues.push(`${label}: selecciona Sí, No o Sin verificar.`);
    if(typeof value==='string'&&value.length>2000)issues.push(`${label}: máximo 2000 caracteres.`);
    if(field.type==='url'&&value){try{return imageUrl(String(value));}catch{issues.push(`${label}: usa una URL HTTPS sin credenciales.`);}}
    if(field.type==='date'&&value&&!this.validDate(String(value)))issues.push(`${label}: fecha inválida o futura.`);
    return value;
  }
  review(strict=true):void {
    this.invalidate();const issues:string[]=[],warnings:string[]=[];
    if(!Object.hasOwn(TECHNICAL_MODEL.types,this.type))issues.push('Selecciona un tipo de producto válido.');
    if(!['catalog','pending','evaluate'].includes(this.use))issues.push('Selecciona un uso válido.');
    if(this.catalogOnly&&this.use==='evaluate')issues.push('Este tipo se gestiona como catálogo; no se evalúa como componente de PC.');
    for(const field of this.commercialFields)if(field.required&&!String(this.product[field.key]??'').trim())issues.push(`Completa ${field.label.toLowerCase()}.`);
    for(const [key,max] of [['name',220],['sku',80],['mpn',120],['brand',130]] as const)if((this.product[key]??'').trim().length>max)issues.push(`${key}: máximo ${max} caracteres.`);
    const number=(key:string)=>this.product[key]==null||!String(this.product[key]).trim()?null:Number(this.product[key]);
    const price=number('price'),stock=number('stock');
    if(price!==null&&(!Number.isFinite(price)||price<0))issues.push('El precio debe ser un número no negativo.');
    if(stock!==null&&(!Number.isInteger(stock)||stock<0))issues.push('El stock debe ser un entero no negativo.');
    const slug=this.categorySlug.trim()||this.category.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-+|-+$/g,'');
    if((slug&&!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug))||(this.categoryParent&&!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(this.categoryParent)))issues.push('Los slugs deben usar minúsculas, números y guiones.');
    if(this.categoryParent&&this.categoryParent===slug)issues.push('Una categoría no puede ser su propia categoría superior.');
    if((this.categorySlug||this.categoryParent)&&!this.category.trim())issues.push('Indica el nombre de la categoría propuesta.');
    if(this.category.length>120)issues.push('Categoría: máximo 120 caracteres.');
    if(!['hardware','software'].includes(this.section))issues.push('Sección de catálogo inválida.');
    if(this.source){try{imageUrl(this.source);}catch{issues.push('La fuente debe usar HTTPS sin credenciales.');}}
    if(this.date&&!this.validDate(this.date))issues.push('La fecha de consulta es inválida o futura.');
    if(this.use==='evaluate'&&(!this.source.trim()||!this.date))(strict?issues:warnings).push('La revisión técnica necesita fuente oficial y fecha.');
    if(this.notes.length>5000)issues.push('Observaciones: máximo 5000 caracteres.');
    const technical:Record<string,unknown>={};
    for(const field of this.definition.fields){const value=this.parseField(field,this.values[field.id],field.label,issues);technical[field.id]=value;
      if(field.required&&value===null)(strict&&this.use==='evaluate'?issues:warnings).push(`Falta: ${field.label}.`);}
    const numeric=(key:string)=>typeof technical[key]==='number'?technical[key] as number:null;
    for(const [low,high,label] of [['base_mhz','boost_mhz','La frecuencia máxima no puede ser menor que la base.'],['base_w','max_w','La potencia máxima no puede ser menor que la base.'],['cores','threads','Los hilos no pueden ser menos que los núcleos.']] as const){
      const a=numeric(low),b=numeric(high);if(a!==null&&b!==null&&b<a)issues.push(label);
    }
    const relatedRecords:Record<string,unknown>={};
    for(const group of this.definition.groups){const rows=this.records[group]??[];if(rows.length>20)issues.push(`${this.groups[group].name}: máximo 20 registros.`);
      relatedRecords[group]=rows.filter(row=>Object.values(row).some(value=>value!=null&&String(value).trim()!=='')).map((row,index)=>{
        const result:Record<string,unknown>={};
        for(const field of this.groups[group].fields){const label=`${this.groups[group].name}, registro ${index+1}: ${field.label}`;const value=this.parseField(field,row[field.id],label,issues);result[field.id]=value;
          if((RECORD_REQUIRED[group]??[]).includes(field.id)&&value===null)(strict&&this.use==='evaluate'?issues:warnings).push(`Falta ${label}.`);}
        if(group==='cpu_board'){
          if(result['cpu_sku']&&result['cpu_sku']===result['board_sku'])issues.push('CPU y placa deben ser productos distintos.');
          warnings.push(`Relación CPU–placa ${index+1}: identificadores y revisión sin vincular a registros reales.`);
        }
        return {...result,resolutionStatus:'pending'};
      });
    }
    if(this.media?.hasPendingUploads())issues.push('Espera a que termine la validación o subida de imágenes.');
    if(this.media?.images().length>8||this.media?.images().some(image=>image.error||image.previewError||image.checkingPreview))issues.push('Corrige las imágenes con error o retíralas antes de revisar.');
    if(this.media?.snapshot().some(image=>image.file&&image.url===null))warnings.push('Los archivos locales no están incluidos en el JSON. Debes volver a adjuntarlos al recuperar el borrador.');
    warnings.push('La fuente y los datos requieren verificación humana. Guardar el borrador no publica el producto ni confirma compatibilidad.');
    this.issues.set(issues);this.warnings.set(warnings);
    if(issues.length){this.error.set(issues.join(' '));this.focusResult('.validation-summary');return;}
    this.reviewed.set(JSON.stringify({schema:'rya.technical-draft.v1',verificationStatus:'pending',requestedUse:this.use,...(this.catalogProductId?{catalogProductId:this.catalogProductId}:{}),
      product:{...Object.fromEntries(Object.entries(this.product).map(([key,value])=>[key,typeof value==='string'?value.trim():value])),price,stock,type:this.type,
        categoryProposal:this.category.trim()?{name:this.category.trim(),slug,section:this.section,parentSlug:this.categoryParent||null}:null},
      technical,relatedRecords,evidence:{url:this.source?imageUrl(this.source):null,date:this.date||null,notes:this.notes},media:this.media?.snapshot()??[]},null,2));
    this.focusResult('.review-summary');
  }
  restoreDraft(raw:string):boolean {
    this.invalidate();
    try{
      if(raw.length>1024*1024)throw new Error('El JSON supera 1 MB.');
      const data=JSON.parse(raw),product=data?.product;
      if(data?.schema!=='rya.technical-draft.v1'||!product||typeof product!=='object'||Array.isArray(product)||!Object.hasOwn(TECHNICAL_MODEL.types,product.type))throw new Error('Esquema o tipo de borrador no admitido.');
      if(data.catalogProductId!=null&&(typeof data.catalogProductId!=='string'||!/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/.test(data.catalogProductId)))throw new Error('Identificador de producto vinculado inválido.');
      for(const key of ['technical','relatedRecords','evidence'])if(data[key]!=null&&(typeof data[key]!=='object'||Array.isArray(data[key])))throw new Error('Estructura de borrador no válida.');
      if(!['catalog','pending','evaluate'].includes(data.requestedUse))throw new Error('Uso solicitado no válido.');
      const definition=TECHNICAL_MODEL.types[product.type];
      const text=(value:unknown,max=2000):string=>{if(value==null)return '';if(!['string','number','boolean'].includes(typeof value)||String(value).length>max)throw new Error('El JSON contiene campos no válidos o demasiado largos.');return String(value);};
      const values=Object.fromEntries(definition.fields.map(field=>[field.id,text(data.technical?.[field.id])]));
      const records:Record<string,Record<string,string>[]>={};
      for(const group of definition.groups){const rows=data.relatedRecords?.[group]??[];
        if(!Array.isArray(rows)||rows.length>20)throw new Error('Registros técnicos inválidos o más de 20 por grupo.');
        records[group]=rows.map((row:any)=>{if(!row||typeof row!=='object'||Array.isArray(row))throw new Error('Registro técnico inválido.');return Object.fromEntries(this.groups[group].fields.map(field=>[field.id,text(row[field.id])]));});
      }
      const images=data.media??[];if(!Array.isArray(images)||images.length>8)throw new Error('El borrador admite hasta ocho imágenes.');
      let missingFiles=0;
      const gallery:Array<{url:string;alt:string;primary:boolean;order:number}>=images.flatMap((image:any)=>{
        if(!image||typeof image!=='object')throw new Error('Imagen inválida.');
        if(!image.url&&image.file){missingFiles++;return [];}
        return [{url:imageUrl(text(image.url)),alt:text(image.alt,300),primary:image.primary===true,order:Number(image.order)||0}];
      });
      if(new Set(gallery.map(image=>image.url)).size!==gallery.length)throw new Error('El borrador contiene imágenes duplicadas.');
      const commercial=Object.fromEntries(this.commercialFields.map(field=>[field.key,text(product[field.key],220)]));
      const proposal=product.categoryProposal;
      const category=text(proposal?.name,120),categorySlug=text(proposal?.slug),categoryParent=text(proposal?.parentSlug),section=text(proposal?.section)||'hardware';
      if(!['hardware','software'].includes(section))throw new Error('Sección de catálogo inválida.');
      const source=text(data.evidence?.url),date=text(data.evidence?.date),notes=text(data.evidence?.notes,5000);
      if(source)imageUrl(source);
      this.catalogProductId=data.catalogProductId??null;
      this.type=product.type;this.previous=this.type;this.product=commercial;this.values=values;this.records=records;this.drafts={};
      this.use=this.catalogOnly?'catalog':data.requestedUse;this.category=category;this.categorySlug=categorySlug;this.categoryParent=categoryParent;this.section=section;
      this.source=source;this.date=date;this.notes=notes;this.media.setImages(gallery);
      this.serverDraft.set(null);this.baseline=null;this.conflict.set(false);this.uncertain.set(false);this.serverNotice.set('');
      this.recoveryNotice.set(`Borrador recuperado localmente, pendiente de verificación.${missingFiles?` ${missingFiles} archivo(s) local(es) no se pudieron recuperar; vuelve a adjuntarlos.`:''}`);
      return true;
    }catch(error){this.error.set(error instanceof Error?error.message:'No se pudo recuperar el borrador.');return false;}
  }
  async importFile(event:Event):Promise<void>{
    const input=event.target as HTMLInputElement,file=input.files?.[0];input.value='';if(!file||this.busy())return;
    if(this.uncertain()){this.serverError.set('Revisa el listado del servidor antes de reemplazar un borrador con guardado incierto.');return;}
    if(file.size>1024*1024){this.error.set('El JSON supera 1 MB.');return;}
    if(this.isDirty()&&!window.confirm('¿Reemplazar el borrador local por el archivo seleccionado?'))return;
    this.importing.set(true);try{const raw=await file.text();if(!this.destroyed)this.restoreDraft(raw);}catch{if(!this.destroyed)this.error.set('No se pudo leer el archivo JSON.');}finally{this.importing.set(false);}
  }
  download():void{this.review(false);if(!this.reviewed())return;const url=URL.createObjectURL(new Blob([this.reviewed()],{type:'application/json'}));const link=document.createElement('a');link.href=url;link.download='rya-borrador-tecnico.json';link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}

  listServer(page=0):void {
    if(this.isDesignMode()||this.busy())return;
    this.listSub?.unsubscribe();this.listOpened.set(true);this.listing.set(true);this.serverError.set('');
    this.serverRows.set([]);this.serverTotal.set(0);this.serverPages.set(0);this.serverPage.set(page);
    this.listSub=this.api.list(page,this.serverQuery,this.serverState).pipe(timeout(30000),takeUntilDestroyed(this.destroyRef),finalize(()=>this.listing.set(false)))
      .subscribe({next:res=>{this.serverRows.set(res.content);this.serverPage.set(res.page);this.serverPages.set(res.totalPages);this.serverTotal.set(res.totalElements);},
        error:err=>{this.serverRows.set([]);this.serverError.set(this.remoteError(err));}});
  }

  loadServer(id:string):void {
    if(this.isDesignMode()||this.busy()||!this.canLeave())return;
    this.loadingDraft.set(true);this.serverError.set('');
    this.api.get(id).pipe(timeout(30000),takeUntilDestroyed(this.destroyRef),finalize(()=>this.loadingDraft.set(false))).subscribe({
      next:res=>{
        if(!this.restoreDraft(JSON.stringify(res.document))){this.serverError.set('El documento del servidor no pudo cargarse. Se conservaron los datos locales.');return;}
        this.serverDraft.set(res);this.baseline=this.fingerprint();
        this.recoveryNotice.set('Borrador recuperado del servidor. Sus datos siguen pendientes de verificación técnica.');
        this.serverNotice.set(`Recuperado · versión ${res.version} · ${this.stateLabel(res.state)}. No es un producto publicado.`);
      },error:err=>this.serverError.set(this.remoteError(err))});
  }

  saveServer():void {
    if(this.isDesignMode()||this.busy()||!this.canEdit()||this.uncertain()||this.conflict())return;
    this.serverError.set('');this.serverNotice.set('');
    if(this.media?.snapshot().some(image=>!image.url||!image.url.startsWith('https://'))){
      this.serverError.set('Los archivos locales todavía no se suben desde este editor técnico. Usa enlaces HTTPS o retira los archivos antes de guardar; no se descartarán silenciosamente.');return;
    }
    this.review(false);if(!this.reviewed())return;
    const document=JSON.parse(this.reviewed()) as Record<string,unknown>,draft=this.serverDraft();
    if(new TextEncoder().encode(JSON.stringify(draft?{version:draft.version,document}:{document})).length>131072){this.serverError.set('El borrador supera el límite de 128 KiB del servidor.');return;}
    this.saving.set(true);
    (draft?this.api.update(draft,document):this.api.create(document)).pipe(timeout(45000),takeUntilDestroyed(this.destroyRef),finalize(()=>this.saving.set(false))).subscribe({
      next:res=>{this.serverDraft.set(res);this.baseline=this.fingerprint();this.reviewed.set('');
        const product=res.document['product'] as Record<string,unknown>|undefined;
        this.serverRows.update(rows=>rows.map(row=>row.id===res.id?{...row,version:res.version,state:res.state,updatedAt:res.updatedAt,
          name:String(product?.['name']??row.name),sku:String(product?.['sku']??row.sku),type:String(product?.['type']??row.type)}:row));
        this.serverNotice.set(`Guardado en el servidor · versión ${res.version}. No se publicó ni se verificó compatibilidad.${this.hasOtherDrafts()?' Los datos de otros tipos permanecen solo en este formulario.':''}`);},
      error:err=>{this.serverError.set(this.remoteError(err));
        if(err?.status===409)this.conflict.set(true);
        if(!err?.status||err.status>=500||[408,429].includes(err.status))this.uncertain.set(true);
        const issues=err?.error?.issues;if(Array.isArray(issues))this.issues.set(issues.filter((v:unknown)=>typeof v==='string'));
      }});
  }

  newDraft():void {
    if(this.busy())return;
    if(this.uncertain()&&!window.confirm('El guardado anterior pudo haberse completado. Confirma que revisaste el listado del servidor antes de iniciar otro borrador para no duplicarlo.'))return;
    if(!this.uncertain()&&this.isDirty()&&!window.confirm('¿Descartar los cambios locales y empezar un nuevo borrador?'))return;
    this.type='cpu';this.previous='cpu';this.use='pending';this.product={};this.values={};this.records={};this.drafts={};
    this.category='';this.categorySlug='';this.categoryParent='';this.section='hardware';this.source='';this.date='';this.notes='';
    this.media.setImages([]);this.serverDraft.set(null);this.catalogProductId=null;this.baseline=null;this.conflict.set(false);this.uncertain.set(false);
    this.serverError.set('');this.serverNotice.set('');this.recoveryNotice.set('');this.invalidate();
  }

  private remoteError(err:any):string {
    if(err?.status===401)return 'La sesión venció. Vuelve a iniciar sesión; los cambios siguen en este formulario.';
    if(err?.status===403)return 'Tu cuenta no tiene permiso para esta operación. No se cambiarán los datos locales.';
    if(err?.status===409)return 'El borrador cambió en otra sesión. Descarga una copia local y recupera la versión del servidor antes de guardar.';
    if(err?.status===503)return 'Los borradores técnicos no están habilitados en este backend. Comprueba la configuración y reinícialo.';
    if(!err?.status||err.status>=500||[408,429].includes(err.status))return 'No se pudo confirmar el resultado. Si estabas guardando, revisa el listado antes de repetir para evitar duplicados. Los cambios locales se conservaron.';
    return err?.error?.message||'No se pudo completar la operación. Reintenta sin perder los datos locales.';
  }
}
