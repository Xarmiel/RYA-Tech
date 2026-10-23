export interface ProductDetailImage {
  id: string;
  url: string;
  alt: string | null;
  position: number;
  primary: boolean;
}

export interface ProductAttribute {
  code: string;
  name: string;
  dataType: string;
  unit: string | null;
  value: string;
}

export interface ProductDetail {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description: string | null;
  price: number;
  brand: string | null;
  brandSlug: string | null;
  category: string;
  categorySlug: string;
  type: string | null;
  availableStock: number;
  shippable: boolean;
  images: ProductDetailImage[];
  attributes: ProductAttribute[];
}

export interface ProductReview {
  id: string;
  rating: number;
  comment: string | null;
  author: string;
  date: string;
}

export interface ProductReviews {
  average: number;
  total: number;
  items: ProductReview[];
}
