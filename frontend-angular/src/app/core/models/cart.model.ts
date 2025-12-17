import { ProductResponse } from './product.model';

export interface CartItem {
  product: ProductResponse;
  quantity: number;
  subtotal: number;
}

export interface Cart {
  items: CartItem[];
  totalItems: number;
  totalPrice: number;
}

