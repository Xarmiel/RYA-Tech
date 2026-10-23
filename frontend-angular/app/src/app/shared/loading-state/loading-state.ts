import { Component, input } from '@angular/core';

/** Placeholders visuales: no representan productos ni datos de demostración. */
@Component({
  selector: 'app-loading-state',
  template: `
    <section class="loading-state" role="status" aria-live="polite" aria-busy="true">
      <h2>{{ label() }}</h2>
      <div class="skeleton-grid" [class.rows]="layout() === 'rows'" aria-hidden="true">
        @for (item of [0, 1, 2]; track item) {
          <div class="skeleton-card"><div class="skeleton-image"></div><div class="skeleton-line"></div><div class="skeleton-line short"></div><div class="skeleton-line"></div></div>
        }
      </div>
    </section>`,
  styles: `
    :host{display:block;min-width:0;width:100%}
    h2{font-size:1rem;color:var(--muted);margin:0 0 1rem}
    .skeleton-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:1.25rem}
    .skeleton-card{padding:1rem;border:1px solid var(--line);border-radius:16px;background:var(--surface)}
    .skeleton-image,.skeleton-line{background:linear-gradient(100deg,var(--surface-raised) 25%,var(--line) 50%,var(--surface-raised) 75%);background-size:200% 100%;animation:shimmer 1.6s ease-in-out infinite;border-radius:8px}
    .skeleton-image{height:clamp(120px,20vw,230px);margin-bottom:1.25rem}
    .skeleton-line{height:16px;margin:1rem 0}.short{width:60%}
    .rows{grid-template-columns:1fr}.rows .skeleton-card{display:grid;grid-template-columns:64px 1fr;gap:1rem;align-items:center}.rows .skeleton-image{height:64px;margin:0;grid-row:span 3}.rows .skeleton-line{margin:0}
    @keyframes shimmer{to{background-position:-200% 0}}
    @media(max-width:700px){.skeleton-grid{grid-template-columns:1fr}.skeleton-grid:not(.rows) .skeleton-card:not(:first-child){display:none}}
    @media(prefers-reduced-motion:reduce){.skeleton-image,.skeleton-line{animation:none}}
  `,
})
export class LoadingState {
  readonly label = input('Cargando información…');
  readonly layout = input<'cards' | 'rows'>('cards');
}
