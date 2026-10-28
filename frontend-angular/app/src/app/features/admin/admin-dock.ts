import { Component, DestroyRef, HostListener, effect, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminAccess } from './admin-access';
import { TECHNICAL_MODEL } from './technical-model';
import { TechnicalEntry } from './technical-entry';
import { firstValueFrom, from, map, mergeMap, timeout, toArray } from 'rxjs';
import { AdminRow, AdminService } from './admin.service';
import { ProductDetailService } from '../product-detail/product-detail.service';

type PendingKind = 'images' | 'drafts' | 'stock' | 'technical';
@Component({selector:'app-admin-dock',imports:[RouterLink],templateUrl:'./admin-dock.html',styleUrl:'./admin-dock.scss'})
export class AdminDock {
  protected readonly access=inject(AdminAccess);private readonly router=inject(Router);open=signal(false);
  private readonly entry=inject(TechnicalEntry);
  private readonly currentUrl=signal(this.router.url);
  private readonly api=inject(AdminService);
  private readonly products=inject(ProductDetailService);
  readonly minimized=signal(false);
  readonly right=signal(24);
  readonly bottom=signal(20);
  readonly dragging=signal(false);
  readonly busy=signal(false);
  readonly error=signal('');
  readonly pending=signal<AdminRow[]|null>(null);
  readonly pendingKind=signal<PendingKind>('images');
  readonly product=signal<{id:string;name:string;sku:string}|null>(null);
  readonly contextBusy=signal(false);
  readonly technicalRows=signal<AdminRow[]|null>(null);
  readonly technicalBusy=signal(false);
  readonly technicalError=signal('');
  private revision=0;
  private destroyed=false;
  private hold?:ReturnType<typeof setTimeout>;
  private pointer:{id:number;x:number;y:number;right:number;bottom:number;target:HTMLElement}|null=null;
  private suppressClick=false;
  constructor(){
    const destroy=inject(DestroyRef);
    this.router.events.pipe(takeUntilDestroyed(destroy)).subscribe(event=>{if(event instanceof NavigationEnd){this.currentUrl.set(event.urlAfterRedirects);this.open.set(false);this.product.set(null);this.contextBusy.set(false);this.revision++;}});
    destroy.onDestroy(()=>{this.destroyed=true;clearTimeout(this.hold);});
    effect(()=>{if(!this.access.allowed()){this.open.set(false);this.pending.set(null);this.technicalRows.set(null);this.product.set(null);this.revision++;}});
  }
  get path(){return this.currentUrl().split(/[?#]/)[0];}
  get inAdmin(){return this.path==='/admin'||this.path.startsWith('/admin/');}
  get context(){return this.product()?.name||(this.path.startsWith('/producto/')?'Producto actual':this.inAdmin?'Panel administrativo':this.path==='/catalogo'?'Catálogo de la tienda':'Tienda');}
  panelRight(){return Math.max(12,Math.min(this.right(),window.innerWidth-352));}
  panelBottom(){return Math.min(this.bottom()+70,Math.max(12,window.innerHeight*.2));}
  filtered(kind=this.pendingKind()){
    if(kind==='technical')return this.technicalRows()??[];
    return (this.pending()??[]).filter(row=>kind==='images'?!row.thumbnail:kind==='drafts'?row.status==='draft':typeof row.rawData?.stockDisponible==='number'&&row.rawData.stockDisponible<=3);
  }
  async selectTechnical(){
    this.pendingKind.set('technical');
    if(this.technicalBusy()||this.technicalRows()!==null||!this.pending()||!this.access.allowed())return;
    const revision=this.revision;this.technicalBusy.set(true);this.technicalError.set('');
    try{
      const rows=await firstValueFrom(from(this.pending()!).pipe(mergeMap(row=>this.api.getProductDetail(row.id).pipe(timeout(15000),map(detail=>({row,detail}))),4),toArray()));
      if(!this.destroyed&&revision===this.revision&&this.access.allowed())this.technicalRows.set(rows.filter(({detail})=>detail.fichaTecnica?.origen==='ANTERIOR'||detail.atributos?.some((a:any)=>a.codigo==='CATALOG_TECH_STATUS'&&/^pendiente/i.test(a.valor))).map(({row})=>row));
    }catch{this.technicalError.set('No se pudieron comprobar todas las fichas. Reintenta.');}
    finally{this.technicalBusy.set(false);}
  }
  toggle(){this.open.update(value=>!value);if(this.open()){void this.loadContext();if(this.pending()===null)void this.refresh();}}
  minimize(){if(this.suppressClick){this.suppressClick=false;return;}this.minimized.update(value=>!value);this.open.set(false);this.constrain();}
  async loadContext(){
    const slug=this.path.match(/^\/producto\/([^/]+)$/)?.[1];
    if(!slug||!this.access.allowed()||this.design)return;
    const revision=this.revision;this.contextBusy.set(true);
    try{const p=await firstValueFrom(this.products.getBySlug(decodeURIComponent(slug)).pipe(timeout(15000)));
      if(!this.destroyed&&revision===this.revision&&this.access.allowed())this.product.set({id:p.id,name:p.name,sku:p.sku});
    }catch{if(revision===this.revision)this.product.set(null);}
    finally{if(revision===this.revision)this.contextBusy.set(false);}
  }
  async refresh(){
    if(this.busy()||this.technicalBusy()||!this.access.allowed()||this.design)return;
    this.busy.set(true);this.error.set('');this.pending.set(null);this.technicalRows.set(null);this.pendingKind.set('images');const rows:AdminRow[]=[];
    const revision=this.revision;
    try{for(let page=0;page<100;page++){
      const result=await firstValueFrom(this.api.getItems('catalogo','',page,100).pipe(timeout(15000)));
      if(this.destroyed||!this.access.allowed()||revision!==this.revision)return;
      rows.push(...result.items);if(page+1>=result.totalPages){this.pending.set(rows);return;}
    }throw new Error('Consulta incompleta');}
    catch{if(!this.destroyed)this.error.set('No se pudieron consultar todos los pendientes. Reintenta.');}
    finally{if(!this.destroyed)this.busy.set(false);}
  }
  action(action:string,id?:string){this.open.set(false);void this.router.navigate([this.base,action==='stock'?'inventario':'catalogo'],{queryParams:{accion:action,producto:id,solicitud:Date.now()}});}
  start(event:PointerEvent){
    if(event.button!==0||this.pointer)return;
    const target=event.currentTarget as HTMLElement;target.setPointerCapture(event.pointerId);
    this.pointer={id:event.pointerId,x:event.clientX,y:event.clientY,right:this.right(),bottom:this.bottom(),target};
    this.hold=setTimeout(()=>{this.dragging.set(true);this.open.set(false);},250);
  }
  move(event:PointerEvent){
    const p=this.pointer;if(!p||p.id!==event.pointerId||!this.dragging())return;
    this.suppressClick=true;this.right.set(p.right+p.x-event.clientX);this.bottom.set(p.bottom+p.y-event.clientY);this.constrain();
  }
  end(event:PointerEvent){
    if(this.pointer?.id!==event.pointerId)return;
    clearTimeout(this.hold);const p=this.pointer;this.pointer=null;this.dragging.set(false);
    if(p.target.hasPointerCapture(event.pointerId))p.target.releasePointerCapture(event.pointerId);
  }
  @HostListener('window:resize') constrain(){
    this.right.set(Math.max(12,Math.min(this.right(),window.innerWidth-(this.minimized()?56:Math.min(340,window.innerWidth-32))-12)));
    this.bottom.set(Math.max(12,Math.min(this.bottom(),window.innerHeight-68)));
  }
  moveKey(event:KeyboardEvent){
    if(!event.altKey)return;
    const offsets:Record<string,number[]>={ArrowLeft:[16,0],ArrowRight:[-16,0],ArrowUp:[0,16],ArrowDown:[0,-16]};
    const offset=offsets[event.key];if(!offset)return;
    event.preventDefault();this.right.update(value=>value+offset[0]);this.bottom.update(value=>value+offset[1]);this.constrain();
  }
  resetPosition(){this.right.set(24);this.bottom.set(20);this.constrain();}
  get design(){return this.currentUrl().split('?')[0].startsWith('/admin/diseno');}
  get base(){return this.design?'/admin/diseno':'/admin';}
  @HostListener('document:keydown',['$event']) shortcut(event:KeyboardEvent):void{
    if(!TECHNICAL_MODEL.shortcut(event))return;
    event.preventDefault();this.entry.open();void this.router.navigateByUrl(this.base+'/carga-tecnica');
  }
}
