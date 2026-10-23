export type BuilderMode = 'presupuesto' | 'manual' | 'componente';

export interface BuilderProfile {
  id: string;
  name: string;
  needs?: Array<{ id: string; name: string }>;
}

export interface BuilderProfileFamily {
  id: string;
  name: string;
  profiles: BuilderProfile[];
}

export interface ComponentType {
  code: string;
  name: string;
  icon?: string;
}

export interface AdvisorBootstrap {
  profileFamilies: BuilderProfileFamily[];
  componentTypes: ComponentType[];
  resolutions: Array<{ id: string; name: string }>;
}

export interface ComponentOption {
  id: string;
  componentTypeCode: string;
  name: string;
  brand?: string | null;
  slug: string;
  summary: string | null;
  price: number;
  currency: 'PEN';
  stock: number;
  imageUrl: string | null;
  imageUrls?: string[];
  compatibility: 'VERIFIED' | 'CONDITIONAL';
}

export interface BuilderValidation {
  components: ComponentOption[];
  total: number;
  currency: 'PEN';
  powerDrawWatts: number | null;
  recommendedPsuWatts: number | null;
  compatible: boolean;
  warnings: string[];
  budget?: number | null;
}

export interface BuilderDraftUi {
  mode: BuilderMode;
  familyId: string;
  profileId: string;
  needId: string;
  resolutionId: string;
  budget: number | null;
  activeComponentCode: string | null;
  anchorComponentCode: string;
  anchorProductId: string;
}

export interface SavedConfigurationComponent {
  id: string;
  tipoComponenteId: string;
  tipoCodigo: string;
  tipoNombre: string;
  productoId: string;
  sku: string;
  productoNombre: string;
  cantidad: number;
  bloqueado: boolean;
  precioSnapshot: number;
  subtotal: number;
  stockDisponible: number;
}

export interface SavedConfiguration {
  id: string;
  nombre: string | null;
  presupuesto: number;
  moneda: 'PEN';
  estado: string;
  totalEstimado: number;
  presupuestoRestante: number;
  componentes: SavedConfigurationComponent[];
}

export interface CpuComparisonDetail {
  productoId: string;
  nombre: string;
  marca: string | null;
  socket: string | null;
  familia: string | null;
  generacion: string | null;
  nucleos: number | null;
  hilos: number | null;
  frecuenciaBaseMhz: number | null;
  frecuenciaMaximaMhz: number | null;
  cacheL3Kb: number | null;
  potenciaBaseW: number | null;
  potenciaMaximaW: number | null;
  tieneGraficosIntegrados: boolean | null;
  precio: number;
  stockDisponible: number;
}

export interface CpuComparison {
  procesadores: CpuComparisonDetail[];
  diferenciaPrecioReferencia: Record<string, number>;
}
