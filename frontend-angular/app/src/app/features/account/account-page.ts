import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AccountService } from '../../core/auth/account.service';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/cart/cart.service';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { RequestError } from '../../shared/request-error/request-error';

@Component({
  selector: 'app-account-page',
  imports: [FormsModule, RouterLink, LoadingState, RequestError],
  templateUrl: './account-page.html',
  styleUrl: './account-page.scss',
})
export class AccountPage implements OnInit {
  protected readonly account = inject(AccountService);
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  protected name = '';
  protected phone = '';
  protected loading = true;
  protected saving = false;
  protected message = '';
  protected success = false;
  protected loadError = false;

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading = true; this.loadError = false; this.message = '';
    this.account.load().subscribe({
      next: profile => { this.name = profile.nombre; this.phone = profile.telefono ?? ''; this.loading = false; },
      error: error => { this.message = error?.error?.message ?? 'No se pudo cargar el perfil.'; this.loading = false; this.loadError = true; },
    });
  }

  protected save(): void {
    this.message = ''; this.success = false;
    if (this.name.trim().length < 3) { this.message = 'Ingresa tu nombre completo.'; return; }
    this.saving = true;
    this.account.update(this.name, this.phone).subscribe({
      next: profile => { this.name = profile.nombre; this.phone = profile.telefono ?? ''; this.success = true; this.message = 'Perfil actualizado.'; this.saving = false; },
      error: error => { this.message = error?.error?.message ?? 'No se pudo actualizar el perfil.'; this.saving = false; },
    });
  }

  protected async logout(): Promise<void> {
    await this.auth.signOut();
    this.account.clear();
    this.cart.load().subscribe({ error: () => undefined });
    await this.router.navigate(['/']);
  }
}
