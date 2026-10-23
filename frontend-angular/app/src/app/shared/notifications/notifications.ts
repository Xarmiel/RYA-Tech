import { Component, inject } from '@angular/core';
import { NotificationService } from '../../core/notifications/notification.service';

@Component({
  selector: 'app-notifications',
  template: `<section class="notifications" aria-label="Notificaciones"><div aria-live="polite" aria-atomic="false">@for (item of notifications.items(); track item.id) {<div class="toast" [class.is-error]="item.kind === 'error'" (mouseenter)="notifications.pause(item.id)" (mouseleave)="notifications.resume(item.id)" (focusin)="notifications.pause(item.id)" (focusout)="notifications.resume(item.id)"><span class="material-symbols-rounded" aria-hidden="true">{{ item.kind === 'success' ? 'check_circle' : item.kind === 'error' ? 'error' : 'info' }}</span><p>{{ item.message }}</p><button type="button" aria-label="Cerrar notificación" (click)="notifications.dismiss(item.id)"><span class="material-symbols-rounded" aria-hidden="true">close</span></button></div>}</div></section>`,
  styles: `
    .notifications{position:fixed;right:20px;bottom:20px;width:min(420px,calc(100vw - 40px));z-index:10000;pointer-events:none}
    .toast{display:flex;align-items:center;gap:12px;margin-top:12px;padding:12px 16px;border:1px solid var(--cyan);border-radius:12px;background:var(--surface);color:var(--text);box-shadow:0 8px 30px #0004;pointer-events:auto}
    .toast>span{color:var(--cyan)}.toast.is-error{border-color:var(--danger)}.toast.is-error>span{color:var(--danger)}p{flex:1;min-width:0;margin:0;overflow-wrap:anywhere}
    button{display:grid;place-items:center;flex-shrink:0;width:44px;height:44px;border:0;background:transparent;color:var(--text);cursor:pointer}
  `,
})
export class Notifications { protected readonly notifications = inject(NotificationService); }
