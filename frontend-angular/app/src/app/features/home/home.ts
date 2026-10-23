import { Component, DestroyRef, OnInit, afterNextRender, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.scss',
  host: { '[style.--store-header-height.px]': 'headerHeight()' },
})
export class Home implements OnInit {
  readonly phrases = [
    'Hardware y software\nen un solo lugar.',
    'Tu setup completo,\nde inicio a fin.',
    'Toda la tecnología\npara tu espacio.',
    'Domina tus partidas\nal máximo nivel.',
    'Diseña sin límites,\ncrea tu realidad.',
    'Escala tu negocio\nal siguiente nivel.',
    'Máxima precisión\nen cada movimiento.',
    'Rendimiento puro\nen todo momento.',
    'Potencia absoluta\npara tus proyectos.',
  ];
  readonly typed = signal('');
  readonly firstLine = computed(() => this.typed().split('\n')[0]);
  readonly secondLine = computed(() => this.typed().split('\n')[1] ?? '');
  readonly onSecondLine = computed(() => this.typed().includes('\n'));
  readonly phraseLines = this.phrases.map(phrase => phrase.split('\n'));
  readonly paused = signal(false);
  readonly reducedMotion = signal(false);
  readonly stopped = computed(() => this.paused() || this.reducedMotion());
  readonly headerHeight = signal(76);
  private readonly destroyRef = inject(DestroyRef);
  private timer?: ReturnType<typeof setTimeout>;
  private index = 0;
  private deleting = false;

  constructor() {
    afterNextRender(() => {
      const header = document.querySelector<HTMLElement>('header.app-header');
      if (!header) return;
      const updateHeight = () => this.headerHeight.set(Math.ceil(header.getBoundingClientRect().height));
      updateHeight();
      if (typeof ResizeObserver !== 'undefined') {
        const observer = new ResizeObserver(updateHeight);
        observer.observe(header);
        this.destroyRef.onDestroy(() => observer.disconnect());
      }
    });
  }

  ngOnInit(): void {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') {
      this.reducedMotion.set(true);
      this.typed.set(this.phrases[0]);
      return;
    }
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    const updateMotion = () => {
      this.reducedMotion.set(preference.matches);
      this.clearTimer();
      if (preference.matches) this.typed.set(this.phrases[this.index]);
      else this.schedule(600);
    };
    const visibility = () => {
      this.clearTimer();
      if (!document.hidden) this.schedule(600);
    };
    preference.addEventListener('change', updateMotion);
    document.addEventListener('visibilitychange', visibility);
    this.destroyRef.onDestroy(() => {
      this.clearTimer();
      preference.removeEventListener('change', updateMotion);
      document.removeEventListener('visibilitychange', visibility);
    });
    updateMotion();
  }

  toggleAnimation(): void {
    this.paused.update(value => !value);
    this.clearTimer();
    if (this.paused()) {
      // Al pausar, deja una frase legible, no una palabra incompleta.
      this.typed.set(this.phrases[this.index]);
      this.deleting = true;
    } else this.schedule(1600);
  }

  private schedule(delay: number): void {
    if (this.stopped() || document.hidden) return;
    this.timer = setTimeout(() => this.advance(), delay);
  }

  private advance(): void {
    const phrase = this.phrases[this.index];
    const length = this.typed().length;
    if (this.deleting) {
      this.typed.set(phrase.slice(0, Math.max(0, length - 1)));
      if (!this.typed()) {
        this.index = (this.index + 1) % this.phrases.length;
        this.deleting = false;
        this.schedule(350);
      } else this.schedule(24);
    } else {
      this.typed.set(phrase.slice(0, length + 1));
      if (this.typed() === phrase) {
        this.deleting = true;
        this.schedule(2500);
      } else this.schedule(55);
    }
  }

  private clearTimer(): void {
    if (this.timer !== undefined) clearTimeout(this.timer);
    this.timer = undefined;
  }
}
