import { Injectable, OnDestroy, signal } from '@angular/core';

export interface Notification { id: number; kind: 'success' | 'error' | 'info'; message: string; }

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  readonly items = signal<Notification[]>([]);
  private sequence = 0;
  private timers = new Map<number, ReturnType<typeof setTimeout>>();

  show(message: string, kind: Notification['kind'] = 'info'): number {
    const existing = this.items().find(item => item.message === message && item.kind === kind);
    if (existing) return existing.id;
    const id = ++this.sequence;
    if (this.items().length >= 4) this.dismiss(this.items()[0].id);
    this.items.update(items => [...items, { id, message, kind }]);
    this.resume(id);
    return id;
  }

  dismiss(id: number): void { this.pause(id); this.items.update(items => items.filter(item => item.id !== id)); }
  pause(id: number): void { clearTimeout(this.timers.get(id)); this.timers.delete(id); }
  resume(id: number): void {
    this.pause(id);
    // Los errores requieren cierre explícito para no perder información importante.
    if (this.items().some(item => item.id === id && item.kind !== 'error')) {
      this.timers.set(id, setTimeout(() => this.dismiss(id), 6000));
    }
  }
  ngOnDestroy(): void { this.timers.forEach(timer => clearTimeout(timer)); this.timers.clear(); }
}
