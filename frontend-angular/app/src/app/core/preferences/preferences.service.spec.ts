import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PreferencesService } from './preferences.service';

describe('Preferencias de presentación', () => {
  afterEach(() => { vi.restoreAllMocks(); vi.unstubAllGlobals(); localStorage.removeItem('rya.preferences.v1'); document.documentElement.removeAttribute('data-theme'); });
  it('ignora preferencias corruptas', () => {
    localStorage.setItem('rya.preferences.v1', '{malformado');
    const preferences = TestBed.inject(PreferencesService);
    expect(preferences.theme()).toBe('dark');
  });
  it('mantiene el tema operativo cuando el navegador bloquea localStorage', () => {
    const preferences = TestBed.inject(PreferencesService);
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => { throw new DOMException('Bloqueado'); });
    expect(() => preferences.setTheme('light')).not.toThrow();
    expect(preferences.theme()).toBe('light');
    expect(document.documentElement.dataset['theme']).toBe('light');
  });

  it('sigue los cambios del sistema solo cuando está seleccionado el modo sistema', () => {
    let listener: (() => void) | undefined;
    const media = { matches: false, addEventListener: vi.fn((_event: string, callback: () => void) => listener = callback), removeEventListener: vi.fn() };
    vi.stubGlobal('matchMedia', vi.fn(() => media));
    const preferences = TestBed.inject(PreferencesService);
    preferences.setTheme('system');
    expect(document.documentElement.dataset['theme']).toBe('dark');
    media.matches = true; listener?.();
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(preferences.theme()).toBe('system');
    preferences.setTheme('dark'); listener?.();
    expect(document.documentElement.dataset['theme']).toBe('dark');
    TestBed.resetTestingModule();
    expect(media.removeEventListener).toHaveBeenCalledWith('change', listener);
  });
});
