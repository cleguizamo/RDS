export interface Balance {
  id: number;
  currentBalance: number;
  lowBalanceThreshold: number;
  lastUpdated: string;
  isLowBalance?: boolean;
}

export interface BalanceResponse extends Balance {
  isLowBalance: boolean;
}

export interface Transaction {
  id: number;
  transactionType: TransactionType;
  amount: number;
  balanceBefore: number;
  balanceAfter: number;
  description: string;
  referenceId?: number;
  referenceType?: string;
  notes?: string;
  createdAt: string;
}

export enum TransactionType {
  INCOME = 'INCOME',
  EXPENSE = 'EXPENSE',
  SALARY_PAYMENT = 'SALARY_PAYMENT',
  ADJUSTMENT = 'ADJUSTMENT',
  REFUND = 'REFUND'
}

export interface Alert {
  id: number;
  alertType: AlertType;
  status: AlertStatus;
  message: string;
  severity: string;
  createdAt: string;
  resolvedAt?: string;
}

export enum AlertType {
  LOW_BALANCE = 'LOW_BALANCE',
  BALANCE_THRESHOLD = 'BALANCE_THRESHOLD',
  PENDING_PAYMENTS = 'PENDING_PAYMENTS'
}

export enum AlertStatus {
  ACTIVE = 'ACTIVE',
  RESOLVED = 'RESOLVED',
  DISMISSED = 'DISMISSED'
}

export interface BalanceInitializationRequest {
  initialBalance: number;
  lowBalanceThreshold: number;
}

export interface BalanceAdjustmentRequest {
  amount: number;
  reason: string;
  notes?: string;
}

export interface MigrationResult {
  ordersMigrated: number;
  deliveriesMigrated: number;
  expensesMigrated: number;
  totalOrdersRevenue: number;
  totalDeliveriesRevenue: number;
  totalExpenses: number;
  totalRevenue: number;
  netBalance: number;
}

