import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { LoadingState } from './loading-state';
import { RequestError } from '../request-error/request-error';

describe('Estados compartidos', () => {
  it('anuncia la carga sin exponer los skeletons como datos reales', () => {
    const fixture = TestBed.createComponent(LoadingState); fixture.componentRef.setInput('label', 'Cargando catálogo'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=status]').textContent).toContain('Cargando catálogo');
    expect(fixture.nativeElement.querySelector('.skeleton-grid').getAttribute('aria-hidden')).toBe('true');
    expect(fixture.nativeElement.querySelector('a')).toBeNull();
  });
  it('ofrece reintento explícito, sin repetir automáticamente una operación', () => {
    const fixture = TestBed.createComponent(RequestError); let retries = 0;
    fixture.componentInstance.retry.subscribe(() => retries++); fixture.detectChanges();
    expect(retries).toBe(0); fixture.nativeElement.querySelector('button').click(); expect(retries).toBe(1);
    expect(fixture.nativeElement.querySelector('[role=alert]')).toBeTruthy();
  });
});
