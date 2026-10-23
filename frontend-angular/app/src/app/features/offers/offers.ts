import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';
import { FavoriteButton } from '../../shared/favorite-button/favorite-button';
import { OfferFilters, OfferPage, OffersService } from './offers.service';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { RequestError } from '../../shared/request-error/request-error';

@Component({
  selector: 'app-offers',
  imports: [FormsModule, RouterLink, CurrencyPipe, DatePipe, DecimalPipe, FavoriteButton, LoadingState, RequestError],
  templateUrl: './offers.html',
  styleUrl: './offers.scss',
})
export class Offers implements OnInit {
  private readonly service = inject(OffersService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  protected readonly data = signal<OfferPage | null>(null);
  protected readonly state = signal<'loading' | 'ready' | 'error'>('loading');
  protected readonly currentPage = signal(0);
  protected readonly brokenImages = signal<Set<string>>(new Set());
  protected readonly orders = [
    { value: 'descuento', label: 'Mayor descuento' }, { value: 'precio-asc', label: 'Menor precio' },
    { value: 'precio-desc', label: 'Mayor precio' }, { value: 'nombre', label: 'Nombre: A–Z' },
    { value: 'fin', label: 'Terminan pronto' },
  ];
  protected draft: OfferFilters = this.empty();
  private applied: OfferFilters = this.empty();

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const minimum = params.get('descuentoMin') ?? '';
      const order = params.get('orden') ?? 'descuento';
      this.applied = {
        q: (params.get('q') ?? '').slice(0, 200), categoria: params.get('categoria') ?? '', marca: params.get('marca') ?? '',
        enStock: params.get('enStock') === 'true',
        descuentoMin: minimum && Number.isFinite(Number(minimum)) && Number(minimum) >= 0 && Number(minimum) <= 100 ? minimum : '',
        orden: this.orders.some(item => item.value === order) ? order : 'descuento',
      };
      this.draft = { ...this.applied };
      const page = Number(params.get('pagina') ?? 1);
      this.currentPage.set(Number.isSafeInteger(page) && page > 0 && page < 1000000 ? page - 1 : 0);
      this.load();
    });
    // Revalida la vigencia mientras la página permanece abierta.
    interval(60000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
  }

  protected load(): void {
    this.request?.unsubscribe();
    this.state.set('loading');
    this.request = this.service.list(this.applied, this.currentPage()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: data => {
        if (data.totalPages > 0 && this.currentPage() >= data.totalPages) { this.navigate(this.applied, data.totalPages - 1); return; }
        this.data.set(data); this.state.set('ready');
      },
      error: () => this.state.set('error'),
    });
  }

  protected apply(): void { this.navigate(this.draft); }
  protected clear(): void { this.navigate(this.empty()); }
  protected filtered(): boolean {
    const f = this.applied;
    return !!(f.q || f.categoria || f.marca || f.enStock || f.descuentoMin);
  }
  protected goTo(page: number): void {
    if (page >= 0 && page < (this.data()?.totalPages ?? 0)) this.navigate(this.applied, page);
  }
  protected imageError(id: string): void { this.brokenImages.update(ids => new Set(ids).add(id)); }

  private navigate(filters: OfferFilters, page = 0): void {
    const queryParams: Record<string, string | number | boolean> = {};
    if (filters.q.trim()) queryParams['q'] = filters.q.trim();
    if (filters.categoria) queryParams['categoria'] = filters.categoria;
    if (filters.marca) queryParams['marca'] = filters.marca;
    if (filters.enStock) queryParams['enStock'] = true;
    if (filters.descuentoMin) queryParams['descuentoMin'] = filters.descuentoMin;
    if (filters.orden !== 'descuento') queryParams['orden'] = filters.orden;
    if (page) queryParams['pagina'] = page + 1;
    void this.router.navigate([], { relativeTo: this.route, queryParams });
  }
  private empty(): OfferFilters { return { q: '', categoria: '', marca: '', enStock: false, descuentoMin: '', orden: 'descuento' }; }
}
