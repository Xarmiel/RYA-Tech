import { CurrencyPipe } from '@angular/common';
import { LoadingState } from '../../shared/loading-state/loading-state';
import { RequestError } from '../../shared/request-error/request-error';
import { DialogFocus } from '../../shared/dialog-focus/dialog-focus';
import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CatalogFacets, CatalogFilterGroup, CatalogFilterOption, CatalogPage } from './catalog.models';
import { CatalogService } from './catalog.service';
import { CartService } from '../../core/cart/cart.service';
import { ProductSummary } from './catalog.models';
import { FavoriteButton } from '../../shared/favorite-button/favorite-button';
import { Subscription } from 'rxjs';

export interface CategoryBubble {
  id: string;
  name: string;
  icon: string;
  imageUrl?: string;
  slug: string;
}

type ViewState = 'loading' | 'ready' | 'empty' | 'error';

@Component({
  selector: 'app-catalog',
  imports: [CurrencyPipe, RouterLink, FavoriteButton, LoadingState, RequestError, DialogFocus],
  templateUrl: './catalog.html',
  styleUrl: './catalog.scss',
})
export class Catalog implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cart = inject(CartService);
  private productsRequest?: Subscription;
  private filtersRequest?: Subscription;
  private readonly pageSize = 12;

  protected readonly categoryBubbles: CategoryBubble[] = [
    {
      id: 'mouse',
      name: 'Mouse para gaming',
      icon: 'mouse',
      slug: 'mouse-gaming',
    },
    {
      id: 'mousepads',
      name: 'Mousepads para gaming',
      icon: 'crop_landscape',
      slug: 'mousepads-gaming',
    },
    {
      id: 'teclados',
      name: 'Teclados para gaming',
      icon: 'keyboard',
      slug: 'teclados-gaming',
    },
    {
      id: 'audio',
      name: 'Audio y audífonos con micrófono para juegos',
      icon: 'headphones',
      slug: 'audio-audifonos',
    },
    {
      id: 'volantes',
      name: 'Volantes de carreras',
      icon: 'sports_motorsports',
      slug: 'volantes-carreras',
    },
    {
      id: 'microfonos',
      name: 'Micrófonos',
      icon: 'mic',
      slug: 'microfonos',
    },
    {
      id: 'simulacion',
      name: 'Simulación',
      icon: 'sports_esports',
      slug: 'simulacion',
    },
    {
      id: 'camaras',
      name: 'Cámaras e iluminación',
      icon: 'videocam',
      slug: 'camaras-iluminacion',
    },
  ];

  protected readonly state = signal<ViewState>('loading');
  protected readonly page = signal<CatalogPage | null>(null);
  protected readonly query = signal('');
  protected readonly filtersOpen = signal(false);
  protected readonly filtersState = signal<'idle' | 'loading' | 'ready' | 'error'>('idle');
  protected readonly facets = signal<CatalogFacets | null>(null);
  protected readonly selectedFilters = signal<Record<string, string[]>>({});
  protected readonly addingProducts = signal<Set<string>>(new Set());
  protected readonly cartMessage = signal('');
  protected readonly appliedFilters = signal<Record<string, string[]>>({});
  protected readonly currentPage = signal(0);
  protected readonly sort = signal('nombre-asc');
  protected readonly failedImages = signal<Set<string>>(new Set());
  protected readonly sortOptions = [
    { value: 'nombre-asc', label: 'Nombre: A–Z', field: 'nombre', direction: 'asc' },
    { value: 'nombre-desc', label: 'Nombre: Z–A', field: 'nombre', direction: 'desc' },
    { value: 'precio-asc', label: 'Precio: menor a mayor', field: 'precio', direction: 'asc' },
    { value: 'precio-desc', label: 'Precio: mayor a menor', field: 'precio', direction: 'desc' },
    { value: 'disponibilidad-desc', label: 'Disponibles primero', field: 'disponibilidad', direction: 'desc' },
  ];
  protected readonly pageNumbers = computed(() => {
    const total = this.page()?.totalPages ?? 0;
    const start = Math.max(0, Math.min(this.currentPage() - 2, total - 5));
    return Array.from({ length: Math.min(5, total) }, (_, index) => start + index);
  });
  protected readonly resultStart = computed(() => this.page()?.totalItems ? this.currentPage() * this.pageSize + 1 : 0);
  protected readonly resultEnd = computed(() => Math.min((this.currentPage() + 1) * this.pageSize, this.page()?.totalItems ?? 0));
  protected readonly activeFilters = computed(() => {
    const chips: Array<{ code: string; value: string; label: string }> = [];
    if (this.query()) chips.push({ code: 'q', value: this.query(), label: `Búsqueda: ${this.query()}` });
    for (const [code, values] of Object.entries(this.appliedFilters())) {
      const group = this.facets()?.groups.find(group => group.code === code);
      for (const value of values) {
        const option = code === '__categoria'
          ? this.facets()?.groups.filter(group => group.kind === 'section').flatMap(group => group.options).find(option => option.value === value)
          : group?.options.find(option => option.value === value);
        const name = code === '__categoria' ? 'Categoría' : code === '__stock' ? 'Disponibilidad' : group?.label ?? code.replace(/_/g, ' ');
        const label = code === '__stock' ? 'Con stock' : option?.label ?? value;
        chips.push({ code, value, label: `${name}: ${label}` });
      }
    }
    return chips;
  });

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      this.query.set(params.get('q') ?? '');
      const category = params.get('categoria');
      const filters: Record<string, string[]> = category ? { __categoria: [category] } : {};
      if (params.get('marca')) filters['__marca'] = [params.get('marca')!];
      if (params.get('enStock') === 'true') filters['__stock'] = ['true'];
      for (const key of params.keys.filter(key => key.startsWith('filtro.'))) filters[key.slice(7)] = params.getAll(key);
      this.appliedFilters.set(filters);
      this.selectedFilters.set({ ...filters });
      const requestedPage = Number(params.get('pagina') ?? 1);
      this.currentPage.set(Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage - 1 : 0);
      const order = params.get('orden');
      this.sort.set(this.sortOptions.some(option => option.value === order) ? order! : 'nombre-asc');
      this.loadFilters(category ?? '');
      this.load();
    });
  }

  protected load(): void {
    this.state.set('loading');
    this.productsRequest?.unsubscribe();
    const order = this.sortOptions.find(option => option.value === this.sort())!;
    this.productsRequest = this.catalog.getProducts(this.query(), this.currentPage(), this.pageSize, this.appliedFilters(), order.field, order.direction).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: page => {
        if (page.totalPages > 0 && this.currentPage() >= page.totalPages) {
          this.navigateCatalog(this.appliedFilters(), page.totalPages - 1);
          return;
        }
        this.page.set(page);
        this.state.set(page.items.length ? 'ready' : 'empty');
      },
      error: () => {
        this.page.set(null);
        this.state.set('error');
      },
    });
  }

  protected openFilters(): void {
    this.selectedFilters.set({ ...this.appliedFilters() });
    this.filtersOpen.set(true);
    this.loadFilters(this.selectedFilters()['__categoria']?.[0] ?? '');
  }

  protected retryFilters(): void { this.loadFilters(this.selectedFilters()['__categoria']?.[0] ?? ''); }

  private loadFilters(category = ''): void {
    this.filtersState.set('loading');
    this.filtersRequest?.unsubscribe();
    this.filtersRequest = this.catalog.getFilters(this.query(), category).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: facets => { this.facets.set(facets); this.filtersState.set('ready'); },
      error: () => this.filtersState.set('error'),
    });
  }

  protected toggleFilter(code: string, value: string, checked: boolean): void {
    const next = { ...this.selectedFilters() };
    if (code === '__hardware' || code === '__software') {
      if (checked) {
        next['__categoria'] = [value];
      } else {
        delete next['__categoria'];
      }
      for (const key of Object.keys(next)) {
        if (key !== '__categoria') delete next[key];
      }
      this.selectedFilters.set(next);
      this.loadFilters(checked ? value : '');
      return;
    }
    if (checked) next[code] = [value]; else delete next[code];
    this.selectedFilters.set(next);
  }

  protected isSelected(code: string, value: string): boolean {
    if (code === '__hardware' || code === '__software') {
      return this.selectedFilters()['__categoria']?.includes(value) ?? false;
    }
    return this.selectedFilters()[code]?.includes(value) ?? false;
  }

  protected selectedCategoryLabel(): string | null {
    const selected = this.selectedFilters()['__categoria']?.[0];
    if (!selected) return null;
    return this.facets()?.groups
      .filter(group => group.kind === 'section')
      .flatMap(group => group.options)
      .find(option => option.value === selected)?.label ?? selected;
  }

  protected visibleFilterOptions(group: CatalogFilterGroup): CatalogFilterOption[] {
    if (group.kind === 'section') return group.options;
    const selected = this.selectedFilters()[group.code]?.[0];
    return selected ? group.options.filter(option => option.value === selected) : group.options;
  }

  protected applyFilters(): void { this.filtersOpen.set(false); this.navigateCatalog(this.selectedFilters()); }
  protected clearFilters(): void {
    this.selectedFilters.set({});
    this.filtersOpen.set(false);
    this.navigateCatalog({}, 0, this.sort(), '');
  }

  protected removeFilter(code: string, value: string): void {
    const filters = { ...this.appliedFilters() };
    if (code === '__categoria') { this.navigateCatalog({}, 0); return; }
    const remaining = (filters[code] ?? []).filter(item => item !== value);
    if (remaining.length) filters[code] = remaining; else delete filters[code];
    this.navigateCatalog(filters, 0, this.sort(), code === 'q' ? '' : this.query());
  }

  protected changeSort(value: string): void {
    if (this.sortOptions.some(option => option.value === value)) this.navigateCatalog(this.appliedFilters(), 0, value);
  }

  protected goToPage(page: number): void {
    if (page < 0 || page >= (this.page()?.totalPages ?? 0) || page === this.currentPage()) return;
    void this.navigateCatalog(this.appliedFilters(), page).then(() => {
      document.getElementById('catalog-results')?.scrollIntoView?.({ block: 'start' });
    });
  }

  protected imageFailed(id: string): void { this.failedImages.update(ids => new Set(ids).add(id)); }

  protected categoryIcon(category: string): string {
    const name = category.toLocaleLowerCase();
    if (name.includes('procesador')) return 'memory';
    if (name.includes('placa')) return 'developer_board';
    if (name.includes('memoria') || name.includes('ram')) return 'memory';
    if (name.includes('gráfica') || name.includes('grafica')) return 'videogame_asset';
    if (name.includes('almacenamiento')) return 'storage';
    if (name.includes('monitor')) return 'desktop_windows';
    return 'inventory_2';
  }

  private navigateCatalog(filters: Record<string, string[]>, page = 0, sort = this.sort(), query = this.query()): Promise<boolean> {
    const queryParams: Record<string, string | string[] | number> = {};
    if (query) queryParams['q'] = query;
    if (page > 0) queryParams['pagina'] = page + 1;
    if (sort !== 'nombre-asc') queryParams['orden'] = sort;
    for (const [code, values] of Object.entries(filters)) {
      if (!values.length) continue;
      queryParams[code === '__categoria' ? 'categoria' : code === '__marca' ? 'marca' : code === '__stock' ? 'enStock' : `filtro.${code}`] = values;
    }
    return this.router.navigate([], { relativeTo: this.route, queryParams });
  }

  protected isCategoryActive(slug: string): boolean {
    return this.appliedFilters()['__categoria']?.includes(slug) ?? false;
  }

  protected toggleCategory(slug: string): void {
    const isCurrent = this.isCategoryActive(slug);
    this.navigateCatalog(isCurrent ? {} : { __categoria: [slug] });
  }

  protected activeCategoryName(): string | null {
    const slug = this.selectedFilters()['__categoria']?.[0];
    if (!slug) return null;
    const bubble = this.categoryBubbles.find(b => b.slug === slug);
    if (bubble) return bubble.name;
    return this.selectedCategoryLabel();
  }

  protected selectedCategorySlug(): string | null {
    return this.selectedFilters()['__categoria']?.[0] ?? null;
  }

  protected addToCart(product: ProductSummary): void {
    if (product.availableStock < 1 || this.addingProducts().has(product.id)) return;
    this.addingProducts.update(current => new Set(current).add(product.id));
    this.cartMessage.set('');
    this.cart.add(product.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.addingProducts.update(current => {
          const next = new Set(current); next.delete(product.id); return next;
        });
        this.cartMessage.set(`${product.name} fue añadido al carrito.`);
      },
      error: error => {
        this.addingProducts.update(current => {
          const next = new Set(current); next.delete(product.id); return next;
        });
        this.cartMessage.set(error?.error?.message ?? 'No se pudo añadir el producto al carrito.');
      },
    });
  }
}
