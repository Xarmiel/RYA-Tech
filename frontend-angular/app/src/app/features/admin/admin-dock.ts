import { Component, DestroyRef, HostListener, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AdminAccess } from './admin-access';
import { TECHNICAL_MODEL } from './technical-model';
import { TechnicalEntry } from './technical-entry';

@Component({selector:'app-admin-dock',imports:[RouterLink],template:`
@if(access.allowed() || design){<div class="admin-dock" (keydown.escape)="open.set(false)">
@if(open()){<nav aria-label="Herramientas rápidas de administración"><strong>{{design?'Herramientas · diseño':'Herramientas de administrador'}}</strong><a [routerLink]="base+'/catalogo'" (click)="open.set(false)">Catálogo y categorías</a><a [routerLink]="base" (click)="open.set(false)">Abrir panel de administrador →</a><small>Ctrl + Shift + − · Carga técnica reservada</small></nav>}
<button type="button" (click)="open.set(!open())" [attr.aria-expanded]="open()"><span class="material-symbols-rounded" aria-hidden="true">construction</span>Herramientas <span aria-hidden="true">{{open()?'−':'+'}}</span></button></div>}`,styles:[`
:host{position:relative;z-index:200}.admin-dock{position:fixed;bottom:20px;right:24px;width:min(340px,calc(100vw - 32px));color:var(--text)}button{width:100%;display:flex;justify-content:space-between;align-items:center;gap:12px;padding:14px 22px;border-radius:40px;border:1px solid var(--line);background:var(--surface-raised);color:var(--text);box-shadow:0 8px 30px #0005;cursor:pointer}nav{display:grid;gap:8px;background:var(--surface-raised);border:1px solid var(--line);padding:20px;margin-bottom:10px;border-radius:6px;box-shadow:0 8px 30px #0004}a{padding:10px 0;color:var(--text)}small{color:var(--muted)}
`]})
export class AdminDock {
  protected readonly access=inject(AdminAccess);private readonly router=inject(Router);open=signal(false);
  private readonly entry=inject(TechnicalEntry);
  private readonly currentUrl=signal(this.router.url);
  constructor(){this.router.events.pipe(takeUntilDestroyed(inject(DestroyRef))).subscribe(event=>{if(event instanceof NavigationEnd){this.currentUrl.set(event.urlAfterRedirects);this.open.set(false);}});}
  get design(){return this.currentUrl().split('?')[0].startsWith('/admin/diseno');}
  get base(){return this.design?'/admin/diseno':'/admin';}
  @HostListener('document:keydown',['$event']) shortcut(event:KeyboardEvent):void{
    if(!TECHNICAL_MODEL.shortcut(event))return;
    event.preventDefault();this.entry.open();void this.router.navigateByUrl(this.base+'/carga-tecnica');
  }
}
