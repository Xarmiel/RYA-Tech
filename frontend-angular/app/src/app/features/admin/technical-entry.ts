import { Injectable, inject, signal } from '@angular/core';
import { CanActivateFn, CanDeactivateFn, Router } from '@angular/router';

export const adminLeaveGuard: CanDeactivateFn<{canLeave():boolean}> = component => component.canLeave();

/** UI entry control only. Real authorization remains in adminGuard and the API. */
@Injectable({providedIn:'root'})
export class TechnicalEntry {
  sectionLeave?: () => boolean;
  readonly openedWithShortcut=signal(false);
  open():void { this.openedWithShortcut.set(true); }
}

// Angular reuses /admin/:section when only the parameter changes; CanDeactivate alone does not run.
export const adminSectionLeaveGuard:CanActivateFn=(route,state)=>{
  const router=inject(Router),current=router.routerState.snapshot.root.firstChild;
  if(current?.routeConfig===route.routeConfig && current?.paramMap.get('section')!==route.paramMap.get('section'))return inject(TechnicalEntry).sectionLeave?.()??true;
  return true;
};

export const technicalEntryGuard:CanActivateFn=(route,state)=>{
  if(route.paramMap.get('section')!=='carga-tecnica')return true;
  if(inject(TechnicalEntry).openedWithShortcut())return true;
  return inject(Router).createUrlTree([state.url.startsWith('/admin/diseno/')?'/admin/diseno':'/admin']);
};
