import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { CatalogFacets, CatalogPage } from './catalog.models';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);

  getProducts(query = '', page = 0, size = 12, filters: Record<string, string[]> = {}, sortBy = 'nombre', sortDir = 'asc'): Observable<CatalogPage> {
    let params = new HttpParams().set('page', page).set('size', size).set('sortBy', sortBy).set('sortDir', sortDir);
    if (query.trim()) params = params.set('q', query.trim());
    for (const [code, values] of Object.entries(filters)) {
      if (code === '__categoria' && values[0]) {
        params = params.set('categoria', values[0]);
      } else if (code === '__marca' && values[0]) {
        params = params.set('marca', values[0]);
      } else if (code === '__stock') {
        if (values.includes('true')) params = params.set('enStock', true);
      } else {
        for (const value of values) params = params.append(`filtro.${code}`, value);
      }
    }
    return this.http.get<BackendCatalogPage>('/api/v1/productos', { params }).pipe(
      map(response => ({
        items: response.content.map(product => ({
          id: product.id,
          sku: product.sku,
          slug: product.slug,
          name: product.nombre,
          brand: product.marca,
          category: product.categoria,
          price: product.precioBase,
          currency: 'PEN' as const,
          availableStock: product.stockDisponible,
          primaryImage: product.imagenPrincipal
            ? { url: product.imagenPrincipal, alt: product.nombre }
            : null,
        })),
        page: response.page,
        size: response.size,
        totalItems: response.totalElements,
        totalPages: response.totalPages,
      })),
    );
  }

  getFilters(query = '', category = ''): Observable<CatalogFacets> {
    let params = new HttpParams();
    if (query.trim()) params = params.set('q', query.trim());
    if (category) params = params.set('categoria', category);
    return this.http.get<BackendCatalogFilters>('/api/v1/productos/filtros', { params }).pipe(
      map(response => {
        const softwareTerms = ['software', 'sistema-operativo', 'sistemas-operativos', 'licencia', 'licencias', 'antivirus'];
        const isSoftware = (slug: string, name: string) => {
          const normalized = `${slug} ${name}`.toLocaleLowerCase();
          return softwareTerms.some(term => normalized.includes(term));
        };
        const toOption = (option: BackendFilterOption) => ({
          value: option.slug,
          label: option.nombre,
          count: option.conteo,
        });
        const hardware = response.categorias.filter(option => !isSoftware(option.slug, option.nombre));
        const software = response.categorias.filter(option => isSoftware(option.slug, option.nombre));

        return { groups: [
          { code: '__hardware', label: 'Hardware', kind: 'section' as const, options: hardware.map(toOption) },
          { code: '__software', label: 'Software', kind: 'section' as const, options: software.map(toOption) },
          ...(response.marcas.length ? [{
            code: '__marca',
            label: 'Fabricante',
            kind: 'filter' as const,
            options: response.marcas.map(option => ({
              value: option.slug,
              label: option.nombre,
              count: option.conteo,
            })),
          }] : []),
          ...response.atributos.map(attribute => ({
            code: attribute.codigo,
            label: this.formatFilterName(attribute.codigo, attribute.nombre),
            kind: 'filter' as const,
            options: [...attribute.valores]
              .sort((left, right) => this.compareFilterValues(left, right))
              .map(value => ({
                value,
                label: this.formatFilterValue(attribute.codigo, attribute.tipoDato, attribute.unidad, value),
                count: 0,
              })),
          })),
        ] };
      }),
    );
  }

  private formatFilterName(code: string, fallback: string): string {
    const normalized = code.toLocaleLowerCase();
    if (normalized.includes('frecuencia_base')) return 'Frecuencia base';
    if (normalized.includes('frecuencia_maxima') || normalized.includes('frecuencia_max')) return 'Frecuencia turbo';
    if (normalized.includes('cache_l3')) return 'Caché L3';
    if (normalized.includes('potencia_base') || normalized.includes('tdp')) return 'Consumo (TDP)';
    return fallback;
  }

  private formatFilterValue(code: string, type: string, unit: string | null, value: string): string {
    if (type.toLocaleUpperCase() === 'BOOLEANO') {
      return value.toLocaleLowerCase() === 'true' ? 'Sí' : 'No';
    }

    const numericValue = Number(value);
    if (!Number.isFinite(numericValue)) return value;

    const normalizedCode = code.toLocaleLowerCase();
    if (normalizedCode.includes('frecuencia')) {
      return `${this.compactNumber(numericValue / 1000)} GHz`;
    }
    if (normalizedCode.includes('cache') && unit?.toLocaleLowerCase() === 'kb') {
      return `${this.compactNumber(numericValue / 1024)} MB`;
    }
    if (normalizedCode.includes('potencia') || normalizedCode.includes('tdp') || unit?.toLocaleLowerCase() === 'w') {
      return `${this.compactNumber(numericValue)} W`;
    }
    return this.compactNumber(numericValue);
  }

  private compareFilterValues(left: string, right: string): number {
    const leftNumber = Number(left);
    const rightNumber = Number(right);
    if (Number.isFinite(leftNumber) && Number.isFinite(rightNumber)) return leftNumber - rightNumber;
    return left.localeCompare(right, 'es', { numeric: true });
  }

  private compactNumber(value: number): string {
    return new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 }).format(value);
  }
}

interface BackendProductSummary {
  id: string;
  sku: string;
  nombre: string;
  slug: string;
  precioBase: number;
  marca: string | null;
  categoria: string;
  imagenPrincipal: string | null;
  stockDisponible: number;
}

interface BackendCatalogPage {
  content: BackendProductSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

interface BackendFilterOption {
  nombre: string;
  slug: string;
  conteo: number;
}

interface BackendCatalogFilters {
  categorias: BackendFilterOption[];
  marcas: BackendFilterOption[];
  rangoPrecio: { min: number; max: number };
  atributos: Array<{
    codigo: string;
    nombre: string;
    tipoDato: string;
    unidad: string | null;
    valores: string[];
  }>;
}
