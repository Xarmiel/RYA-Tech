import { Directive, ElementRef, inject } from '@angular/core';

@Directive({ selector: '[appMenuKeyboard]', host: { '(keydown)': 'onKeydown($event)' } })
export class MenuKeyboard {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  onKeydown(event: KeyboardEvent): void {
    if (!['ArrowDown', 'ArrowUp', 'Home', 'End'].includes(event.key)) return;
    const items = Array.from(this.element.querySelectorAll<HTMLElement>('button:not(:disabled),a[href]'));
    if (!items.length) return;
    event.preventDefault();
    const index = items.indexOf(document.activeElement as HTMLElement);
    const next = event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1
      : (index + (event.key === 'ArrowDown' ? 1 : -1) + items.length) % items.length;
    items[next].focus();
  }
}
