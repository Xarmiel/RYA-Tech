import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationService } from './notification.service';

describe('Notificaciones globales', () => {
  let service: NotificationService;
  beforeEach(() => { vi.useFakeTimers(); service = new NotificationService(); });
  afterEach(() => { service.ngOnDestroy(); vi.useRealTimers(); });
  it('evita duplicados y limita la cola a cuatro mensajes', () => {
    const id = service.show('Guardado', 'success');
    expect(service.show('Guardado', 'success')).toBe(id);
    for (let index = 0; index < 5; index++) service.show(`Aviso ${index}`);
    expect(service.items()).toHaveLength(4);
  });
  it('cierra éxitos pero conserva los errores hasta que se descartan', () => {
    service.show('Guardado', 'success'); const error = service.show('Sin conexión', 'error');
    vi.advanceTimersByTime(7000);
    expect(service.items().map(item => item.id)).toEqual([error]);
    service.dismiss(error); expect(service.items()).toEqual([]);
  });
  it('pausa el cierre mientras se lee o se enfoca una notificación', () => {
    const id = service.show('Información'); service.pause(id);
    vi.advanceTimersByTime(20000); expect(service.items()).toHaveLength(1);
    service.resume(id); vi.advanceTimersByTime(6000); expect(service.items()).toHaveLength(0);
  });
});
