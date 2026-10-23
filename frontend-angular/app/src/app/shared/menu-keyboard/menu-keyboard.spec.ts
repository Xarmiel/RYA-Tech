import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { MenuKeyboard } from './menu-keyboard';

@Component({ imports: [MenuKeyboard], template: '<div appMenuKeyboard><button>Oscuro</button><button>Claro</button><button>Sistema</button></div>' })
class Host {}

describe('Teclado en menús', () => {
  it('recorre las opciones de forma circular y admite Inicio/Fin', () => {
    const fixture = TestBed.createComponent(Host); fixture.detectChanges();
    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    buttons[0].focus();
    for (const [key, index] of [['ArrowUp', 2], ['Home', 0], ['ArrowDown', 1], ['End', 2]] as const) {
      document.activeElement?.dispatchEvent(new KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
      expect(document.activeElement).toBe(buttons[index]);
    }
  });
});
