import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';

@Injectable({ providedIn: 'root' })
export class AdminAccess {
  private readonly auth = inject(AuthService);
  private readonly http = inject(HttpClient);
  private readonly verifiedUser = signal<string | null>(null);
  readonly allowed = computed(() => !!this.auth.user() && this.verifiedUser() === this.auth.user()?.id);
  constructor() {
    effect(() => {
      const user = this.auth.user();
      this.verifiedUser.set(null);
      if (user) void this.check();
    });
  }
  async check(): Promise<boolean> {
    const id = this.auth.user()?.id;
    if (!id) return false;
    try {
      const result = await firstValueFrom(this.http.get<{allowed:boolean}>('/api/v1/admin/acceso'));
      if (this.auth.user()?.id === id) this.verifiedUser.set(result.allowed ? id : null);
      return result.allowed && this.auth.user()?.id === id;
    } catch { this.verifiedUser.set(null); return false; }
  }
}

export const adminGuard: CanActivateFn = async (_, state) => {
  const auth = inject(AuthService), access = inject(AdminAccess), router = inject(Router);
  await auth.initialize().catch(()=>null);
  if (!auth.user()) return router.createUrlTree(['/login'], {queryParams:{returnUrl:state.url}});
  return await access.check() || router.createUrlTree(['/cuenta'], {queryParams:{acceso:'restringido'}});
};
