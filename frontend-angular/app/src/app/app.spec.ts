import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { App } from './app';
import { PreferencesService } from './core/preferences/preferences.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient()],
    }).compileComponents();
  });

  it('creates the RYA shell', () => {
    const fixture = TestBed.createComponent(App);
    expect(fixture.componentInstance).toBeTruthy();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('header')).toBeTruthy();
  });

  it('abre la cuenta al pasar el puntero y la cierra al salir', () => {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    const dropdown = (fixture.nativeElement as HTMLElement).querySelector('.account-dropdown')!;
    const trigger = dropdown.querySelector('button')!;
    dropdown.dispatchEvent(new MouseEvent('mouseenter')); fixture.detectChanges();
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    expect(dropdown.classList.contains('is-open')).toBe(true);
    dropdown.dispatchEvent(new MouseEvent('mouseleave')); fixture.detectChanges();
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
  });

  it('cambia de tema e icono en cada clic sin desplegar un menú', () => {
    const preferences = TestBed.inject(PreferencesService); preferences.setTheme('dark');
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    const button = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.theme-toggle')!;
    expect(button.textContent?.trim()).toBe('dark_mode');
    for (const [theme, icon] of [['light', 'light_mode'], ['system', 'desktop_windows'], ['dark', 'dark_mode']] as const) {
      button.click(); fixture.detectChanges();
      expect(preferences.theme()).toBe(theme);
      expect(button.textContent?.trim()).toBe(icon);
      expect(button.getAttribute('aria-label')).toContain('Cambiar a tema');
      expect(fixture.nativeElement.querySelector('.appearance-menu')).toBeNull();
      expect(JSON.parse(localStorage.getItem('rya.preferences.v1')!).theme).toBe(theme);
    }
  });
});
