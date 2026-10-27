import { describe, it, expect, vi } from 'vitest';
import { SidebarResize } from './sidebar-resize';

describe('Resizable admin sidebar', () => {
  it('supports collapse, expansion and reset with the keyboard', () => {
    const sidebar = new SidebarResize();
    const key = (value: string) => sidebar.key(new KeyboardEvent('keydown', {key: value}));
    key('Home'); expect(sidebar.width()).toBe(72);
    key('ArrowRight'); expect(sidebar.width()).toBe(160);
    key('ArrowLeft'); expect(sidebar.width()).toBe(72);
    key('Enter'); expect(sidebar.width()).toBe(260);
    key('ArrowRight'); expect(sidebar.width()).toBe(270);
    key('ArrowLeft'); expect(sidebar.width()).toBe(260);
    key('End'); expect(sidebar.width()).toBeLessThanOrEqual(360);
  });
  it('snaps near the original width, ignores other pointers and restores cancelled drags', () => {
    const sidebar = new SidebarResize();
    const handle = {setPointerCapture: vi.fn(), hasPointerCapture: () => true, releasePointerCapture: vi.fn(), focus: vi.fn()};
    const event = (x: number, pointerId = 1) => ({button: 0, pointerId, clientX: x, currentTarget: handle, preventDefault: vi.fn()} as unknown as PointerEvent);
    sidebar.start(event(260));
    sidebar.move(event(310, 2)); expect(sidebar.width()).toBe(260);
    sidebar.move(event(270)); expect(sidebar.width()).toBe(260);
    sidebar.move(event(330)); expect(sidebar.width()).toBe(330);
    sidebar.cancel(event(330)); expect(sidebar.width()).toBe(260); expect(sidebar.dragging()).toBe(false);
    sidebar.start(event(260)); sidebar.move(event(80)); sidebar.finish(event(80));
    expect(sidebar.width()).toBe(72); expect(sidebar.dragging()).toBe(false);
  });
});
