import { Directive, HostListener, signal } from '@angular/core';

@Directive({selector: '[adminSidebarResize]', exportAs: 'sidebarResize'})
export class SidebarResize {
  readonly width = signal(260);
  readonly dragging = signal(false);
  private pointer: number | null = null;
  private startX = 0;
  private startWidth = 260;
  private maximum() { return Math.min(360, Math.floor(window.innerWidth * .45)); }
  private resize(value: number, snap = true) {
    this.width.set(value <= 120 ? 72 : Math.max(160, Math.min(this.maximum(), snap && Math.abs(value - 260) <= 12 ? 260 : value)));
  }
  @HostListener('pointerdown', ['$event']) start(event: PointerEvent) {
    if (event.button !== 0 || this.pointer !== null) return;
    const handle = event.currentTarget as HTMLElement;
    handle.setPointerCapture(event.pointerId);
    this.pointer = event.pointerId;
    this.startX = event.clientX;
    this.startWidth = this.width();
    this.dragging.set(true);
    handle.focus();
    event.preventDefault();
  }
  @HostListener('pointermove', ['$event']) move(event: PointerEvent) {
    if (this.pointer === event.pointerId) this.resize(this.startWidth + event.clientX - this.startX);
  }
  @HostListener('pointerup', ['$event']) finish(event: PointerEvent) {
    if (this.pointer !== event.pointerId) return;
    this.pointer = null;
    this.dragging.set(false);
    const handle = event.currentTarget as HTMLElement;
    if (handle.hasPointerCapture(event.pointerId)) handle.releasePointerCapture(event.pointerId);
  }
  @HostListener('pointercancel', ['$event']) cancel(event: PointerEvent) {
    if (this.pointer !== event.pointerId) return;
    this.width.set(this.startWidth);
    this.finish(event);
  }
  @HostListener('lostpointercapture') lostCapture() { this.pointer = null; this.dragging.set(false); }
  @HostListener('dblclick') reset() { this.width.set(260); }
  @HostListener('keydown', ['$event']) key(event: KeyboardEvent) {
    const step = event.shiftKey ? 40 : 10;
    switch (event.key) {
      case 'ArrowLeft': this.resize(this.width() <= 160 ? 72 : this.width() - step, false); break;
      case 'ArrowRight': this.resize(this.width() === 72 ? 160 : this.width() + step, false); break;
      case 'Home': this.width.set(72); break;
      case 'End': this.resize(this.maximum()); break;
      case 'Enter': this.reset(); break;
      default: return;
    }
    event.preventDefault();
  }
}
