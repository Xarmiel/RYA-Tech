import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Notifications } from './shared/notifications/notifications';
import { NotificationService } from './core/notifications/notification.service';
import { MenuKeyboard } from './shared/menu-keyboard/menu-keyboard';
import { PreferencesService, ThemePreference } from './core/preferences/preferences.service';
import { CartService } from './core/cart/cart.service';
import { AuthService } from './core/auth/auth.service';
import { AccountService } from './core/auth/account.service';
import { FavoritesService } from './core/favorites/favorites.service';
import { AdminDock } from './features/admin/admin-dock';

@Component({
  selector: 'app-root',
  imports: [FormsModule, RouterLink, RouterLinkActive, RouterOutlet, Notifications, MenuKeyboard, AdminDock],
  styleUrl: './app.scss',
  templateUrl: './app.html',
  host: {
    '(document:click)': 'onDocumentClick($event)',
    '(document:focusin)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'closeMenus($event)',
  },
})
export class App implements OnInit {
  protected isAdminRoute():boolean { return /^\/admin(?:\/|$)/.test(this.router.url); }
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly notifications = inject(NotificationService);
  protected readonly preferences = inject(PreferencesService);
  protected readonly cart = inject(CartService);
  protected readonly auth = inject(AuthService);
  protected readonly account = inject(AccountService);
  protected readonly favorites = inject(FavoritesService);
  protected readonly searchTerm = signal('');
  protected readonly themeModes: Record<ThemePreference, { icon: string; label: string; next: ThemePreference }> = {
    dark: { icon: 'dark_mode', label: 'oscuro', next: 'light' },
    light: { icon: 'light_mode', label: 'claro', next: 'system' },
    system: { icon: 'desktop_windows', label: 'del sistema', next: 'dark' },
  };
  protected readonly mobileMenuOpen = signal(false);
  protected readonly accountMenuOpen = signal(false);
  protected readonly catalogMenuOpen = signal(false);
  private catalogCloseTimer: ReturnType<typeof setTimeout> | undefined;

  constructor() {this.destroyRef.onDestroy(()=>clearTimeout(this.catalogCloseTimer));}
  protected openCatalogMenu():void {
    clearTimeout(this.catalogCloseTimer);this.catalogCloseTimer=undefined;this.catalogMenuOpen.set(true);
  }
  protected crossCatalogHeader(event:MouseEvent):void {
    if(!this.catalogMenuOpen())return;
    const target=event.target as HTMLElement;
    if(target.closest('.header-discovery')){clearTimeout(this.catalogCloseTimer);this.catalogCloseTimer=undefined;}
    else if(!target.closest('.nav-mega-item')&&this.catalogCloseTimer===undefined)this.leaveCatalogMenu(event);
  }
  protected leaveCatalogMenu(event:MouseEvent|FocusEvent):void {
    if(event.relatedTarget instanceof Node && (event.currentTarget as HTMLElement).contains(event.relatedTarget))return;
    clearTimeout(this.catalogCloseTimer);
    // The search row sits between the trigger and panel in wrapped headers.
    this.catalogCloseTimer=setTimeout(()=>{this.catalogMenuOpen.set(false);this.catalogCloseTimer=undefined;},250);
  }

  ngOnInit(): void {
    this.router.events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      if (event instanceof NavigationEnd) this.closeMenus();
    });
    void this.auth.initialize().then(session => {
      if (session) {
        this.account.load().subscribe({ error: () => undefined });
        this.cart.linkToAccount().subscribe({ error: () => this.cart.load().subscribe({ error: () => undefined }) });
      } else {
        this.cart.load().subscribe({ error: () => undefined });
      }
    }).catch(() => this.cart.load().subscribe({ error: () => undefined }));
  }

  protected search(): void {
    const query = this.searchTerm().trim();
    if (!query) return;
    void this.router.navigate(['/catalogo'], { queryParams: { q: query } });
  }

  protected cycleTheme(): void {
    this.preferences.setTheme(this.themeModes[this.preferences.theme()].next);
  }

  protected themeButtonLabel(): string {
    const mode = this.themeModes[this.preferences.theme()];
    return `Tema ${mode.label}. Cambiar a tema ${this.themeModes[mode.next].label}`;
  }

  protected async logout(): Promise<void> {
    try { await this.auth.signOut(); }
    catch { this.notifications.show('No se pudo cerrar la sesión. Inténtalo nuevamente.', 'error'); return; }
    this.account.clear();
    this.cart.load().subscribe({ error: () => undefined });
    void this.router.navigate(['/']);
  }

  protected isAuthRoute(): boolean {
    const path = this.router.url.split(/[?#]/, 1)[0];
    return ['/login', '/registro', '/recuperar-clave', '/restablecer-clave'].includes(path);
  }

  protected onDocumentClick(event: Event): void {
    const target = event.target as HTMLElement | null;
    if (!target) return;
    if (!target.closest('.account-dropdown')) {
      this.accountMenuOpen.set(false);
    }
    if(!target.closest('.nav-mega-item')){clearTimeout(this.catalogCloseTimer);this.catalogMenuOpen.set(false);}
  }

  protected loginReturnUrl(): string {
    return this.isAuthRoute() ? '/' : this.router.url;
  }

  protected skipToContent(event: Event): void {
    event.preventDefault();
    const main = document.getElementById('main-content');
    main?.focus(); main?.scrollIntoView({ block: 'start' });
  }

  protected routeActivated(): void {
    // No robar el foco al cambiar filtros o página dentro de la misma pantalla.
    setTimeout(() => {
      const target = document.getElementById('main-content') ?? document.getElementById('admin-main') ?? document.querySelector<HTMLElement>('app-auth-page main');
      target?.setAttribute('tabindex', '-1'); target?.focus({ preventScroll: true });
    });
  }

  protected closeMenus(event?: Event): void {
    if (event) {
      const selector = this.accountMenuOpen() ? '.account-trigger' : this.mobileMenuOpen() ? '.mobile-menu-toggle' : '';
      if (selector) { event.preventDefault(); document.querySelector<HTMLElement>(selector)?.focus(); }
      else if ((event.target as HTMLElement | null)?.closest('.nav-mega-item')) {
        event.preventDefault(); document.querySelector<HTMLElement>('.nav-mega-item > a')?.focus();
      }
    }
    clearTimeout(this.catalogCloseTimer);this.catalogMenuOpen.set(false);
    this.accountMenuOpen.set(false); this.mobileMenuOpen.set(false);
  }

  protected openAccountMenu(event: Event): void {
    event.preventDefault();
    this.accountMenuOpen.set(true);
    setTimeout(() => document.querySelector<HTMLElement>('.account-menu a[href], .account-menu button')?.focus());
  }
}
