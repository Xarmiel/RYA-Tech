import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { Order, OrderPage, PurchasesService } from './purchases.service';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { RequestError } from '../../shared/request-error/request-error';

@Component({
  selector: 'app-purchases',
  imports: [RouterLink, CurrencyPipe, DatePipe, LoadingState, RequestError],
  templateUrl: './purchases.html',
  styleUrl: './purchases.scss',
})
export class Purchases implements OnInit {
  private readonly service = inject(PurchasesService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private request?: Subscription;
  protected readonly detailId = signal<string | null>(null);
  protected readonly currentPage = signal(0);
  protected readonly state = signal<'loading' | 'ready' | 'error' | 'not-found' | 'unauthorized'>('loading');
  protected readonly page = signal<OrderPage | null>(null);
  protected readonly order = signal<Order | null>(null);
  protected readonly brokenImages = signal<Set<string>>(new Set());

  ngOnInit(): void {
    combineLatest([this.route.paramMap, this.route.queryParamMap]).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(([params, query]) => {
      this.detailId.set(params.get('id'));
      const page = Number(query.get('pagina') ?? 1);
      this.currentPage.set(Number.isSafeInteger(page) && page > 0 && page < 1000000 ? page - 1 : 0);
      this.load();
    });
  }

  protected load(): void {
    this.request?.unsubscribe();
    this.state.set('loading');
    const error = (response: { status: number }) => {
      this.order.set(null); this.page.set(null);
      this.state.set(response.status === 401 ? 'unauthorized' : response.status === 404 ? 'not-found' : 'error');
    };
    const id = this.detailId();
    if (id) {
      this.request = this.service.detail(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: order => { this.order.set(order); this.state.set('ready'); }, error,
      });
    } else {
      this.request = this.service.list(this.currentPage()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: page => {
          if (page.totalPages > 0 && this.currentPage() >= page.totalPages) { this.goTo(page.totalPages - 1); return; }
          this.page.set(page); this.state.set('ready');
        }, error,
      });
    }
  }

  protected goTo(page: number): void {
    if (page < 0) return;
    void this.router.navigate(['/compras'], { queryParams: page ? { pagina: page + 1 } : {} });
  }
  protected units(order: Order): number { return order.productos.reduce((sum, item) => sum + item.cantidad, 0); }
  protected statusLabel(state: string): string {
    const label = state.replace(/_/g, ' ').toLocaleLowerCase('es');
    return label.charAt(0).toUpperCase() + label.slice(1);
  }
  protected statusStyle(state: string): string {
    if (['ENTREGADO', 'COMPLETADO'].includes(state)) return 'complete';
    if (['CANCELADO', 'RECHAZADO', 'REEMBOLSADO'].includes(state)) return 'cancelled';
    return 'progress';
  }
  protected imageError(id: string): void { this.brokenImages.update(ids => new Set(ids).add(id)); }
  protected loginReturnUrl(): string { return this.router.url; }
}
