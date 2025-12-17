export enum OrderType {
  EN_MESA = 'EN_MESA',
  DOMICILIO = 'DOMICILIO'
}

export enum PaymentMethod {
  CASH = 'CASH',
  NEQUI = 'NEQUI',
  DAVIPLATA = 'DAVIPLATA',
  BANK_TRANSFER = 'BANK_TRANSFER',
  CARD = 'CARD'
}

export enum PaymentStatus {
  PENDING = 'PENDING',
  VERIFIED = 'VERIFIED',
  REJECTED = 'REJECTED',
  PAID = 'PAID',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
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
  // Campos de pago
  paymentStatus?: PaymentStatus;
  paymentMethod?: PaymentMethod;
  paymentProofUrl?: string;
  verifiedBy?: number;
  verifiedByName?: string;
  verifiedAt?: string;
}

export interface OrderRequest {
  userId: number;
  items: OrderItemRequest[];
  tableNumber: number;
  paymentMethod: PaymentMethod;
  paymentProofUrl?: string;
}

export interface DeliveryRequest {
  userId: number;
  items: OrderItemRequest[];
  deliveryAddress: string;
  deliveryPhone: number;
  paymentMethod: PaymentMethod;
  paymentProofUrl?: string;
}

export interface UnifiedOrder extends Order {
  type: OrderType;
}

