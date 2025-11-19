export enum OrderType {
  EN_MESA = 'EN_MESA',
  DOMICILIO = 'DOMICILIO'
}

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface Order {
  id: number;
  date: string;
  time: string;
  totalPrice: number;
  status: boolean;
  type: OrderType;
  tableNumber?: number;
  deliveryAddress?: string;
  deliveryPhone?: number;
  userId: number;
  userName: string;
  userEmail: string;
  items?: OrderItem[];
}

export interface OrderRequest {
  userId: number;
  items: OrderItemRequest[];
  tableNumber: number;
}

export interface DeliveryRequest {
  userId: number;
  items: OrderItemRequest[];
  deliveryAddress: string;
  deliveryPhone: number;
}

export interface UnifiedOrder extends Order {
  type: OrderType;
}

