export interface CartItem {
  id: string;
  productId: string;
  sku: string;
  name: string;
  slug: string;
  image: string | null;
  quantity: number;
  availableStock: number;
  unitPrice: number;
  subtotal: number;
  priceUpdated: boolean;
}

export interface Cart {
  id: string | null;
  currency: 'PEN';
  items: CartItem[];
  totalQuantity: number;
  subtotal: number;
}
