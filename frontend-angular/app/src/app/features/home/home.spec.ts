import {provideHttpClient} from '@angular/common/http';
import {provideHttpClientTesting} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Home } from './home';

describe('Home typewriter', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal('matchMedia', vi.fn(() => ({matches:false,addEventListener:vi.fn(),removeEventListener:vi.fn()})));
    TestBed.configureTestingModule({imports:[Home],providers:[provideRouter([]),provideHttpClient(),provideHttpClientTesting()]});
  });
  afterEach(() => { TestBed.resetTestingModule(); vi.useRealTimers(); vi.restoreAllMocks(); vi.unstubAllGlobals(); });

  it('types and deletes both lines as one continuous sequence', () => {
    const fixture=TestBed.createComponent(Home);fixture.detectChanges();
    const home=fixture.componentInstance;
    expect(home.phrases).toHaveLength(9);
    vi.advanceTimersByTime(600);expect(home.typed()).toBe('H');
    vi.advanceTimersByTime(('Hardware y software'.length-1)*55);
    expect(home.firstLine()).toBe('Hardware y software');expect(home.secondLine()).toBe('');
    expect(home.onSecondLine()).toBe(false);
    vi.advanceTimersByTime(55);expect(home.onSecondLine()).toBe(true);
    vi.advanceTimersByTime('en un solo lugar.'.length*55);expect(home.typed()).toBe(home.phrases[0]);
    vi.advanceTimersByTime(2500);expect(home.secondLine()).toBe('en un solo lugar');
    vi.advanceTimersByTime(('en un solo lugar.'.length-1)*24);
    expect(home.firstLine()).toBe('Hardware y software');expect(home.secondLine()).toBe('');
    vi.advanceTimersByTime(24);expect(home.onSecondLine()).toBe(false);
    vi.advanceTimersByTime(24);expect(home.firstLine()).toBe('Hardware y softwar');
    vi.advanceTimersByTime(('Hardware y software'.length-1)*24+350);expect(home.typed()).toBe('T');
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h1').getAttribute('aria-label')).toBe('Hardware y software en un solo lugar.');
    expect(fixture.nativeElement.querySelectorAll('.typing-cursor')).toHaveLength(1);
  });

  it('pauses with a full readable phrase and clears timers on leaving', () => {
    const fixture=TestBed.createComponent(Home);fixture.detectChanges();const home=fixture.componentInstance;
    vi.advanceTimersByTime(800);home.toggleAnimation();expect(home.typed()).toBe(home.phrases[0]);
    vi.advanceTimersByTime(10000);expect(home.typed()).toBe(home.phrases[0]);
    fixture.detectChanges();expect(fixture.nativeElement.querySelector('.experience-hero').classList.contains('hero-animation-paused')).toBe(true);
    expect(fixture.nativeElement.querySelector('.hero-contact-shadow').getAttribute('aria-hidden')).toBe('true');
    home.toggleAnimation();vi.advanceTimersByTime(1600);expect(home.typed()).not.toBe(home.phrases[0]);
    fixture.destroy();expect(vi.getTimerCount()).toBe(0);
  });

  it('shows a static phrase when reduced motion is requested', () => {
    vi.spyOn(window,'matchMedia').mockReturnValue({matches:true,addEventListener:vi.fn(),removeEventListener:vi.fn()} as unknown as MediaQueryList);
    const fixture=TestBed.createComponent(Home);fixture.detectChanges();const home=fixture.componentInstance;
    expect(home.typed()).toBe(home.phrases[0]);vi.advanceTimersByTime(10000);expect(home.typed()).toBe(home.phrases[0]);
    expect(fixture.nativeElement.querySelector('.animation-control')).toBeNull();
  });
});
