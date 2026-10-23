import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { DialogFocus } from './dialog-focus';

@Component({ imports: [DialogFocus], template: `@if(open){<div appDialogFocus aria-label="Prueba" (dialogClose)="open=false"><button>Primero</button><button>Último</button></div>}` })
class Host { open = true; }

describe('Diálogos accesibles', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [Host] });
    vi.spyOn(HTMLElement.prototype, 'getClientRects').mockImplementation(() => [{ width: 44 }] as unknown as DOMRectList);
  });
  afterEach(() => vi.restoreAllMocks());
  it('cierra con Escape, restaura el foco y libera el desplazamiento', async () => {
    const opener = document.createElement('button'); document.body.append(opener); opener.focus();
    const previousOverflow = document.body.style.overflow;
    const fixture = TestBed.createComponent(Host); fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 0));
    const dialog = fixture.nativeElement.querySelector('[role=dialog]') as HTMLElement;
    expect(dialog.getAttribute('aria-modal')).toBe('true');
    expect(document.body.style.overflow).toBe('hidden');
    dialog.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role=dialog]')).toBeNull();
    expect(document.activeElement).toBe(opener);
    expect(document.body.style.overflow).toBe(previousOverflow); opener.remove();
  });
  it('mantiene Tab y Mayús+Tab dentro del panel', async () => {
    const fixture = TestBed.createComponent(Host); fixture.detectChanges();
    await new Promise(resolve => setTimeout(resolve, 0));
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[1].focus(); buttons[1].dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }));
    expect(document.activeElement).toBe(buttons[0]);
    buttons[0].dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true }));
    expect(document.activeElement).toBe(buttons[1]); fixture.destroy();
  });
});
