import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { NotificationService } from '../notifications/notification.service';

interface FavoriteCache {
  ids: string[];
  pending: Record<string, boolean>;
}

@Injectable({ providedIn: 'root' })
export class FavoritesService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly notifications = inject(NotificationService);
  private owner = 'guest';
  private generation = 0;
  private initialized = false;
  private cache = this.read('guest');
  private syncingGeneration: number | null = null;

  readonly ids = signal<string[]>(this.cache.ids);
  readonly count = computed(() => this.ids().length);
  readonly syncing = signal(false);
  readonly message = signal('');
  readonly isGuest = signal(true);

  constructor() {
    effect(() => {
      if (!this.auth.ready()) return;
      const owner = this.auth.user()?.id ?? 'guest';
      untracked(() => this.switchOwner(owner));
    });
  }

  has(id: string): boolean { return this.ids().includes(id); }

  toggle(id: string): void {
    if (this.has(id)) this.remove(id);
    else this.change(id, true);
  }

  remove(id: string): void { this.change(id, false); }

  retry(): void { void this.synchronize(); }

  private change(id: string, selected: boolean): void {
    if (!this.validId(id)) return;
    if (selected && !this.has(id) && this.count() >= 500) {
      this.message.set('Puedes guardar hasta 500 favoritos.');
      return;
    }
    this.cache.pending[id] = selected;
    this.cache.ids = selected ? [...new Set([id, ...this.cache.ids])] : this.cache.ids.filter(value => value !== id);
    const persisted = this.publish();
    if (persisted) {
      this.message.set('');
      this.notifications.show(selected ? 'Producto guardado en favoritos.' : 'Producto eliminado de favoritos.', 'success');
    } else this.notifications.show('No pudimos guardar los favoritos en este navegador.', 'error');
    if (this.owner !== 'guest') void this.synchronize();
  }

  private switchOwner(owner: string): void {
    if (this.initialized && owner === this.owner) return;
    this.initialized = true;
    this.generation++;
    this.owner = owner;
    this.cache = this.read(owner);
    this.isGuest.set(owner === 'guest');
    this.syncing.set(false);
    this.message.set('');
    if (owner !== 'guest') {
      const guest = this.read('guest');
      for (const id of guest.ids) {
        if (!(id in this.cache.pending)) this.cache.pending[id] = true;
      }
      this.cache.ids = [...new Set([...guest.ids, ...this.cache.ids])].slice(0, 500);
      // Se transfiere al almacén de esta cuenta antes de vaciar la lista invitada.
      if (this.publish()) {
        try { localStorage.removeItem(this.key('guest')); } catch { /* La lista sigue en memoria. */ }
      }
      void this.synchronize();
    } else this.publish();
  }

  private async synchronize(): Promise<void> {
    const generation = this.generation;
    if (this.owner === 'guest' || this.syncingGeneration === generation) return;
    this.syncingGeneration = generation;
    this.syncing.set(true);
    try {
      // Incluso sin cambios locales, recupera los favoritos guardados en otros dispositivos.
      let remote = await firstValueFrom(this.http.get<string[]>('/api/v1/favoritos'));
      if (generation !== this.generation) return;
      do {
        const pending = { ...this.cache.pending };
        if (Object.keys(pending).length) {
          remote = await firstValueFrom(this.http.post<string[]>('/api/v1/favoritos/sincronizar', {
            agregar: Object.keys(pending).filter(id => pending[id]),
            eliminar: Object.keys(pending).filter(id => !pending[id]),
          }));
          if (generation !== this.generation) return;
          for (const [id, value] of Object.entries(pending)) {
            if (this.cache.pending[id] === value) delete this.cache.pending[id];
          }
        }
        const merged = new Set(remote);
        for (const [id, value] of Object.entries(this.cache.pending)) {
          if (value) merged.add(id); else merged.delete(id);
        }
        this.cache.ids = [...merged];
        this.publish();
      } while (Object.keys(this.cache.pending).length);
      this.message.set('');
    } catch {
      if (generation === this.generation) this.message.set('No pudimos sincronizar tus favoritos. Tus cambios siguen guardados en este dispositivo.');
    } finally {
      if (generation === this.generation) this.syncing.set(false);
      if (this.syncingGeneration === generation) this.syncingGeneration = null;
    }
  }

  private publish(): boolean {
    this.ids.set([...this.cache.ids]);
    try {
      localStorage.setItem(this.key(this.owner), JSON.stringify(this.cache));
      return true;
    } catch {
      this.message.set('El navegador no permite guardar favoritos. Se conservarán mientras esta página siga abierta.');
      return false;
    }
  }

  private read(owner: string): FavoriteCache {
    try {
      const value = JSON.parse(localStorage.getItem(this.key(owner)) ?? 'null');
      return {
        ids: Array.isArray(value?.ids) ? [...new Set<string>(value.ids.filter((id: unknown) => this.validId(id)))].slice(0, 500) : [],
        pending: Object.fromEntries(Object.entries(value?.pending ?? {}).filter(([id, selected]) => this.validId(id) && typeof selected === 'boolean')) as Record<string, boolean>,
      };
    } catch { return { ids: [], pending: {} }; }
  }

  private key(owner: string): string { return `rya.favorites.v1.${owner}`; }
  private validId(id: unknown): id is string {
    return typeof id === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
  }
}
