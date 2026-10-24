import { Injectable, inject } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { AccountService } from './account.service';
import { AuthService } from './auth.service';
import { CartService } from '../cart/cart.service';
import { NotificationService } from '../notifications/notification.service';

/** Ancillary synchronization survives navigation, but cannot hold up login. No automatic POST retries. */
@Injectable({providedIn:'root'})
export class PostLoginSync {
  private readonly account = inject(AccountService);
  private readonly cart = inject(CartService);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);

  start(): void {
    const userId = this.auth.user()?.id;
    if (!userId) return;
    void Promise.allSettled([
      firstValueFrom(this.account.load().pipe(timeout(15000))),
      firstValueFrom(this.cart.linkToAccount().pipe(timeout(15000))),
    ]).then(results => {
      if (this.auth.user()?.id !== userId || results.every(result => result.status === 'fulfilled')) return;
      this.notifications.show('La sesión está iniciada, pero no pudimos confirmar la sincronización del perfil o carrito. Revisa tu cuenta y carrito; no se ha reintentado automáticamente.', 'info');
    });
  }
}
