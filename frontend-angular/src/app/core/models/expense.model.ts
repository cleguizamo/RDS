export interface ExpenseRequest {
  description: string;
  category: string;
  amount: number;
  expenseDate: string;
  paymentMethod?: string;
  notes?: string;
  receiptUrl?: string;
}

export interface ExpenseResponse {
  id: number;
  description: string;
  category: string;
  amount: number;
  expenseDate: string;
  paymentMethod?: string;
  notes?: string;
  receiptUrl?: string;
}

export const EXPENSE_CATEGORIES = [
  'Nómina',
  'Inventario',
  'Servicios',
  'Mantenimiento',
  'Marketing',
  'Alquiler',
  'Impuestos',
  'Seguros',
  'Otros'
];

export const PAYMENT_METHODS = [
  'Efectivo',
  'Transferencia',
  'Tarjeta',
  'Cheque'
];

