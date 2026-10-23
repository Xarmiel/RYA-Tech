import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';

export type ThemePreference = 'dark' | 'light' | 'system';

interface UserPreferences {
  version: 1;
  theme: ThemePreference;
}

const STORAGE_KEY = 'rya.preferences.v1';
const DEFAULT_PREFERENCES: UserPreferences = { version: 1, theme: 'dark' };

@Injectable({ providedIn: 'root' })
export class PreferencesService {
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);
  private readonly colorScheme = globalThis.matchMedia?.('(prefers-color-scheme: light)');
  private readonly preferences = signal<UserPreferences>(this.read());

  readonly theme = () => this.preferences().theme;

  constructor() {
    this.applyTheme(this.preferences().theme);
    const followSystem = () => {
      if (this.theme() === 'system') this.applyTheme('system');
    };
    this.colorScheme?.addEventListener?.('change', followSystem);
    this.destroyRef.onDestroy(() => this.colorScheme?.removeEventListener?.('change', followSystem));
  }

  setTheme(theme: ThemePreference): void {
    const next: UserPreferences = { ...this.preferences(), theme };
    this.preferences.set(next);
    this.write(next);
    this.applyTheme(theme);
  }

  private read(): UserPreferences {
    if (typeof localStorage === 'undefined') return DEFAULT_PREFERENCES;
    try {
      const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '') as Partial<UserPreferences>;
      const theme = parsed.theme;
      return parsed.version === 1 && (theme === 'dark' || theme === 'light' || theme === 'system')
        ? { version: 1, theme }
        : DEFAULT_PREFERENCES;
    } catch {
      return DEFAULT_PREFERENCES;
    }
  }

  private write(preferences: UserPreferences): void {
    if (typeof localStorage !== 'undefined') {
      try { localStorage.setItem(STORAGE_KEY, JSON.stringify(preferences)); }
      catch { /* La preferencia sigue funcionando en memoria si el navegador bloquea el almacenamiento. */ }
    }
  }

  private applyTheme(theme: ThemePreference): void {
    const resolved = theme === 'system'
      ? this.colorScheme?.matches ? 'light' : 'dark'
      : theme;
    this.document.documentElement.dataset['theme'] = resolved;
  }
}
