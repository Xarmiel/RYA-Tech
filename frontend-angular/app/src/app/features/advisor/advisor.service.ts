import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, forkJoin, map, of } from 'rxjs';
import { AdvisorBootstrap, BuilderValidation, ComponentOption, CpuComparison, SavedConfiguration } from './advisor.models';

@Injectable({ providedIn: 'root' })
export class AdvisorService {
  private readonly http = inject(HttpClient);

  getBootstrap(): Observable<AdvisorBootstrap> {
    return this.http.get<AdvisorBootstrap>('/api/v1/pc-builder/inicial');
  }

  getCompatibleOptions(componentCode: string, selectedIds: string[] = []): Observable<ComponentOption[]> {
    let params = new HttpParams().set('tipo', componentCode);
    for (const id of selectedIds) params = params.append('componentesActuales', id);
    return this.http.get<BackendProductSummary[]>('/api/v1/pc-builder/opciones', { params }).pipe(
      map(products => products.map(product => ({
        id: product.id,
        componentTypeCode: componentCode,
        name: product.nombre,
        brand: product.marca,
        slug: product.slug,
        summary: product.sku,
        price: product.precioBase,
        currency: 'PEN' as const,
        stock: product.stockDisponible,
        imageUrl: product.imagenPrincipal,
        imageUrls: product.imagenPrincipal ? [product.imagenPrincipal] : [],
        compatibility: 'CONDITIONAL' as const,
      }))),
    );
  }

  validateSelection(selection: Record<string, ComponentOption>, lockedIds: string[] = []): Observable<BuilderValidation> {
    const components = Object.values(selection);
    const validation = this.http.post<BackendCompatibility>('/api/v1/pc-builder/validar-cambio', this.validationPayload(selection, lockedIds)).pipe(
      catchError(() => of({ esCompatible: false, estadoCompatibilidad: 'POR_VERIFICAR' as const, mensajes: ['La compatibilidad técnica todavía no pudo verificarse.'] })),
    );
    const calculation = this.http.post<BackendCalculation>('/api/v1/pc-builder/calcular', {
      items: components.map(component => ({ productoId: component.id, cantidad: 1 })),
    });
    return forkJoin({ validation, calculation }).pipe(map(({ validation: compatibility, calculation: totals }) => ({
      components,
      total: totals.precioTotal,
      currency: 'PEN' as const,
      powerDrawWatts: totals.consumoEstimadoW,
      recommendedPsuWatts: totals.fuenteRecomendadaW,
      compatible: compatibility.esCompatible,
      warnings: [...(compatibility.mensajes ?? []), ...(totals.notas ?? [])],
    })));
  }

  recommend(payload: Record<string, unknown>): Observable<BuilderValidation> {
    return this.http.post<BuilderValidation>('/api/v1/pc-builder/recomendar', payload);
  }

  compareCpus(ids: string[]): Observable<CpuComparison> {
    return this.http.post<CpuComparison>('/api/v1/cpu/comparar', { cpuIds: ids });
  }

  createConfiguration(name: string, budget: number): Observable<SavedConfiguration> {
    return this.http.post<SavedConfiguration>('/api/v1/pc-builder/configuraciones', {
      nombre: name.trim() || null,
      presupuesto: budget,
      moneda: 'PEN',
    });
  }

  listConfigurations(): Observable<SavedConfiguration[]> {
    return this.http.get<SavedConfiguration[]>('/api/v1/pc-builder/configuraciones');
  }

  getConfiguration(id: string): Observable<SavedConfiguration> {
    return this.http.get<SavedConfiguration>(`/api/v1/pc-builder/configuraciones/${id}`);
  }

  applyConfigurationChange(configurationId: string, code: string, option: ComponentOption, locked: boolean): Observable<unknown> {
    return this.http.post('/api/v1/pc-builder/aplicar-cambio', {
      configuracionId: configurationId,
      tipoComponente: code,
      productoId: option.id,
      cantidad: 1,
      bloqueado: locked,
    });
  }

  private validationPayload(selection: Record<string, ComponentOption>, lockedIds: string[]): Record<string, unknown> {
    const normalized = new Map(Object.entries(selection).map(([code, option]) => [this.normalizeCode(code), option.id]));
    const find = (...codes: string[]) => codes.map(code => normalized.get(code)).find(Boolean) ?? null;
    return {
      cpuId: find('CPU', 'PROCESADOR', 'PROCESADORES'),
      placaId: find('PLACA', 'MOTHERBOARD', 'PLACAMADRE', 'PLACASMADRE'),
      ramId: find('RAM', 'MEMORIA', 'MEMORIARAM'),
      gpuId: find('GPU', 'TARJETAGRAFICA', 'TARJETASGRAFICAS'),
      fuenteId: find('PSU', 'FUENTE', 'FUENTEDEPODER', 'FUENTESDEPODER'),
      gabineteId: find('CASE', 'GABINETE', 'GABINETES'),
      refrigeracionId: find('COOLER', 'REFRIGERACION', 'ENFRIAMIENTO', 'ENFRIAMIENTOLIQUIDO'),
      componentesActuales: Object.values(selection).map(option => option.id),
      componentesBloqueados: lockedIds,
    };
  }

  private normalizeCode(code: string): string {
    return code.normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
  }
}

interface BackendProductSummary {
  id: string;
  sku: string;
  nombre: string;
  slug: string;
  precioBase: number;
  marca: string | null;
  imagenPrincipal: string | null;
  stockDisponible: number;
}

interface BackendCompatibility {
  esCompatible: boolean;
  estadoCompatibilidad: 'VERIFICADA' | 'CONDICIONAL' | 'INCOMPATIBLE' | 'POR_VERIFICAR';
  mensajes: string[];
}

interface BackendCalculation {
  precioTotal: number;
  consumoEstimadoW: number;
  fuenteRecomendadaW: number | null;
  notas: string[];
}
