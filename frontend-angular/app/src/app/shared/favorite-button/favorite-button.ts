import { Component, inject, input } from '@angular/core';
import { FavoritesService } from '../../core/favorites/favorites.service';

@Component({
  selector: 'app-favorite-button',
  template: `<button type="button" [class.selected]="favorites.has(productId())"
    [attr.aria-pressed]="favorites.has(productId())"
    [attr.aria-label]="(favorites.has(productId()) ? 'Quitar de favoritos: ' : 'Añadir a favoritos: ') + productName()"
    [title]="favorites.has(productId()) ? 'Quitar de favoritos' : 'Añadir a favoritos'"
    (click)="toggle($event)"><span class="material-symbols-rounded" aria-hidden="true">favorite</span></button>`,
  styles: `
    :host { display: inline-flex; }
    button { display: grid; place-items: center; width: 42px; height: 42px; padding: 0; border: 1px solid var(--line); border-radius: 50%; background: var(--surface); color: var(--muted); cursor: pointer; transition: color .15s, border-color .15s, transform .15s; }
    button:hover { color: var(--cyan); border-color: var(--cyan); transform: scale(1.06); }
    button.selected { color: var(--cyan); border-color: var(--cyan); }
    button.selected span { font-variation-settings: 'FILL' 1; }
  `,
})
export class FavoriteButton {
  readonly productId = input.required<string>();
  readonly productName = input('producto');
  protected readonly favorites = inject(FavoritesService);
  protected toggle(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.favorites.toggle(this.productId());
  }
}
