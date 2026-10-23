export interface ProductImage {
  url: string;
  alt: string | null;
}

export interface ProductSummary {
  id: string;
  sku: string;
  slug: string;
  name: string;
  brand: string | null;
  category: string;
  price: number;
  currency: 'PEN';
  availableStock: number;
  primaryImage: ProductImage | null;
}

export interface CatalogPage {
  items: ProductSummary[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CatalogFilterOption {
  value: string;
  label: string;
  count: number;
}

export interface CatalogFilterGroup {
  code: string;
  label: string;
  kind: 'section' | 'filter';
  options: CatalogFilterOption[];
}

export interface CatalogFacets {
  groups: CatalogFilterGroup[];
}
