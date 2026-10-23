import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { describe, expect, it, vi } from 'vitest';
import { authGuard } from './core/auth/auth.guard';
import { AuthService } from './core/auth/auth.service';
import { Experiences } from './features/experiences/experiences';
import { routes } from './app.routes';

@Component({ template: '<h1>Pantalla de prueba</h1>' })
class Page {}

describe('Navegación y protección de rutas', () => {
  it('conserva la dirección original al pedir inicio de sesión', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([
      { path: 'compras', component: Page, canActivate: [authGuard] }, { path: 'login', component: Page },
    ]), { provide: AuthService, useValue: { initialize: vi.fn().mockResolvedValue(null) } }] });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/compras?pagina=2');
    const router = TestBed.inject(Router);
    expect(router.url.split('?')[0]).toBe('/login');
    expect(router.parseUrl(router.url).queryParams['returnUrl']).toBe('/compras?pagina=2');
  });
  it('permite al usuario autenticado abrir sus compras', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter([{ path: 'compras', component: Page, canActivate: [authGuard] }]),
      { provide: AuthService, useValue: { initialize: vi.fn().mockResolvedValue({ user: { id: 'test' } }) } }] });
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/compras', Page);
    expect(TestBed.inject(Router).url).toBe('/compras');
  });
  it('mantiene checkout y favoritos públicos y protege los datos personales', () => {
    for (const path of ['checkout', 'favoritos', 'catalogo']) expect(routes.find(route => route.path === path)?.canActivate).toBeUndefined();
    for (const path of ['cuenta', 'compras', 'compras/:id']) expect(routes.find(route => route.path === path)?.canActivate).toContain(authGuard);
  });
  it('permite recorrer las pestañas de experiencias con flechas e Inicio/Fin', () => {
    TestBed.configureTestingModule({ imports: [Experiences], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(Experiences); fixture.detectChanges();
    const tabs = fixture.nativeElement.querySelectorAll('[role=tab]') as NodeListOf<HTMLButtonElement>;
    tabs[0].focus(); tabs[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true })); fixture.detectChanges();
    expect(tabs[1].getAttribute('aria-selected')).toBe('true'); expect(document.activeElement).toBe(tabs[1]);
    tabs[1].dispatchEvent(new KeyboardEvent('keydown', { key: 'End', bubbles: true })); fixture.detectChanges();
    expect(tabs[2].getAttribute('aria-selected')).toBe('true');
    tabs[2].dispatchEvent(new KeyboardEvent('keydown', { key: 'Home', bubbles: true })); fixture.detectChanges();
    expect(tabs[0].getAttribute('aria-selected')).toBe('true');
  });
});
