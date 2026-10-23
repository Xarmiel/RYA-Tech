import { AfterViewInit, Directive, ElementRef, OnDestroy, inject, output } from '@angular/core';

@Directive({
  selector: '[appDialogFocus]',
  host: { role: 'dialog', 'aria-modal': 'true', tabindex: '-1', '(keydown)': 'onKeydown($event)' },
})
export class DialogFocus implements AfterViewInit, OnDestroy {
  readonly dialogClose = output<void>();
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private readonly previous = document.activeElement as HTMLElement | null;
  private readonly overflow = document.body.style.overflow;
  private timer?: ReturnType<typeof setTimeout>;
  private readonly background: Array<{ element: HTMLElement; inert: boolean }> = [];

  ngAfterViewInit(): void {
    document.body.style.overflow = 'hidden';
    // El fondo queda fuera de la navegación y del lector de pantalla.
    let branch: HTMLElement = this.element;
    while (branch.parentElement) {
      for (const sibling of Array.from(branch.parentElement.children)) {
        if (sibling instanceof HTMLElement && sibling !== branch && !['SCRIPT', 'STYLE', 'LINK'].includes(sibling.tagName)) {
          this.background.push({ element: sibling, inert: sibling.inert }); sibling.inert = true;
        }
      }
      branch = branch.parentElement;
      if (branch === document.body) break;
    }
    this.timer = setTimeout(() => (this.focusable()[0] ?? this.element).focus());
  }
  onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); this.dialogClose.emit(); }
    if (event.key !== 'Tab') return;
    const items = this.focusable();
    const first = items[0]; const last = items.at(-1);
    if (!first) { event.preventDefault(); this.element.focus(); return; }
    if (event.shiftKey && (document.activeElement === first || document.activeElement === this.element)) { event.preventDefault(); last?.focus(); }
    else if (!event.shiftKey && (document.activeElement === last || document.activeElement === this.element)) { event.preventDefault(); first.focus(); }
  }
  ngOnDestroy(): void {
    clearTimeout(this.timer);
    document.body.style.overflow = this.overflow;
    for (const item of this.background) item.element.inert = item.inert;
    if (this.previous?.isConnected) this.previous.focus();
  }
  private focusable(): HTMLElement[] {
    return Array.from(this.element.querySelectorAll<HTMLElement>('a[href],button,input,select,textarea,summary,[tabindex]'))
      .filter(element => !element.matches(':disabled,[tabindex="-1"]') && !element.closest('[hidden],[inert]') && element.getClientRects().length > 0);
  }
}
