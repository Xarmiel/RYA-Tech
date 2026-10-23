import { CurrencyPipe } from '@angular/common';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, from, map, mergeMap, of, switchMap, toArray } from 'rxjs';
import { FavoritesService } from '../../core/favorites/favorites.service';
import { FavoriteButton } from '../../shared/favorite-button/favorite-button';
import { ProductDetail } from '../product-detail/product-detail.models';
import { ProductDetailService } from '../product-detail/product-detail.service';

interface FavoriteRow { id: string; product: ProductDetail | null; unavailable: boolean; }

@Component({
  selector: 'app-favorites',
  imports: [CurrencyPipe, RouterLink, FavoriteButton, LoadingState],
  templateUrl: './favorites.html',
  styleUrl: './favorites.scss',
})
export class Favorites {
  protected readonly favorites = inject(FavoritesService);
  private readonly products = inject(ProductDetailService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly rows = signal<FavoriteRow[]>([]);
  protected readonly loading = signal(true);
  private readonly refresh = signal(0);

  constructor() {
    toObservable(computed(() => ({ ids: this.favorites.ids(), refresh: this.refresh() }))).pipe(
      switchMap(({ ids }) => {
        this.loading.set(true);
        return from(ids).pipe(
          mergeMap(id => this.products.getById(id).pipe(
            map(product => ({ id, product, unavailable: false } as FavoriteRow)),
            catchError(error => of({ id, product: null, unavailable: error.status === 404 } as FavoriteRow)),
          ), 6),
          toArray(),
          map(rows => rows.sort((a, b) => ids.indexOf(a.id) - ids.indexOf(b.id))),
        );
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(rows => {
      this.rows.set(rows);
      this.loading.set(false);
    });
  }

  protected retry(): void { this.refresh.update(value => value + 1); this.favorites.retry(); }
}
