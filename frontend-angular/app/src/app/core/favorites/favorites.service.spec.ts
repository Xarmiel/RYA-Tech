import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AuthService } from '../auth/auth.service';
import { FavoritesService } from './favorites.service';

const product = '11111111-1111-4111-8111-111111111111';
const other = '22222222-2222-4222-8222-222222222222';
const account = 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa';

describe('Favoritos locales y sincronización', () => {
  let service: FavoritesService;
  let http: HttpTestingController;
  let user: ReturnType<typeof signal<{ id: string } | null>>;
  const settle = async () => { await new Promise(resolve => setTimeout(resolve, 0)); TestBed.tick(); };

  beforeEach(() => {
    localStorage.clear();
    user = signal<{ id: string } | null>(null);
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(), provideHttpClientTesting(),
      { provide: AuthService, useValue: { ready: signal(true), user } },
    ] });
    service = TestBed.inject(FavoritesService);
    http = TestBed.inject(HttpTestingController);
    TestBed.tick();
  });

  afterEach(() => http.verify());

  it('guarda y elimina favoritos invitados sin realizar peticiones', () => {
    service.toggle(product);
    expect(service.has(product)).toBe(true);
    expect(JSON.parse(localStorage.getItem('rya.favorites.v1.guest')!).ids).toEqual([product]);
    service.toggle(product);
    expect(service.count()).toBe(0);
    http.expectNone('/api/v1/favoritos');
  });

  it('combina los favoritos invitados con los de la cuenta y separa la sesión al salir', async () => {
    service.toggle(product);
    user.set({ id: account });
    TestBed.tick();
    http.expectOne('/api/v1/favoritos').flush([other]);
    await settle();
    const sync = http.expectOne('/api/v1/favoritos/sincronizar');
    expect(sync.request.body).toEqual({ agregar: [product], eliminar: [] });
    sync.flush([other, product]);
    await settle();
    expect(service.ids()).toEqual([other, product]);
    expect(localStorage.getItem('rya.favorites.v1.guest')).toBeNull();
    user.set(null);
    TestBed.tick();
    expect(service.ids()).toEqual([]);
    expect(JSON.parse(localStorage.getItem(`rya.favorites.v1.${account}`)!).ids).toEqual([other, product]);
  });

  it('conserva cambios al fallar la red y los reintenta', async () => {
    user.set({ id: account });
    TestBed.tick();
    http.expectOne('/api/v1/favoritos').flush([], { status: 503, statusText: 'Offline' });
    await settle();
    service.toggle(product);
    http.expectOne('/api/v1/favoritos').flush([], { status: 503, statusText: 'Offline' });
    await settle();
    expect(service.has(product)).toBe(true);
    expect(service.message()).toContain('sincronizar');
    service.retry();
    http.expectOne('/api/v1/favoritos').flush([]);
    await settle();
    http.expectOne('/api/v1/favoritos/sincronizar').flush([product]);
    await settle();
    expect(service.message()).toBe('');
    expect(JSON.parse(localStorage.getItem(`rya.favorites.v1.${account}`)!).pending).toEqual({});
  });

  it('respeta una eliminación mientras el alta está en curso', async () => {
    service.toggle(product);
    user.set({ id: account });
    TestBed.tick();
    http.expectOne('/api/v1/favoritos').flush([]);
    await settle();
    const add = http.expectOne('/api/v1/favoritos/sincronizar');
    service.remove(product);
    add.flush([product]);
    await settle();
    const remove = http.expectOne('/api/v1/favoritos/sincronizar');
    expect(remove.request.body).toEqual({ agregar: [], eliminar: [product] });
    remove.flush([]);
    await settle();
    expect(service.ids()).toEqual([]);
  });

  it('descarta respuestas de la cuenta anterior después de cerrar sesión', async () => {
    user.set({ id: account });
    TestBed.tick();
    const load = http.expectOne('/api/v1/favoritos');
    user.set(null);
    TestBed.tick();
    load.flush([product]);
    await settle();
    expect(service.ids()).toEqual([]);
    expect(service.isGuest()).toBe(true);
  });
});
