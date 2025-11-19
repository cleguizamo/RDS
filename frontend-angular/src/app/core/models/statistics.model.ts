export interface FinancialStatsResponse {
  totalRevenue: number;
  totalExpenses: number;
  netProfit: number;
  startDate: string;
  endDate: string;
  ordersRevenue: number;
  deliveriesRevenue: number;
  expensesByCategory: CategoryExpenseResponse[];
  dailyStats: DailyStatsResponse[];
}

export interface CategoryExpenseResponse {
  category: string;
  totalAmount: number;
  count?: number;
}

export interface DailyStatsResponse {
  date: string;
  revenue: number;
  expenses: number;
  profit: number;
  ordersCount: number;
  deliveriesCount: number;
}

export interface BusinessStatsResponse {
  totalOrders: number;
  totalDeliveries: number;
  totalReservations: number;
  totalCustomers: number;
  totalProducts: number;
  topProducts: TopProductResponse[];
  topCustomers: TopCustomerResponse[];
  todayStats: DailySummaryResponse;
  monthlyStats: MonthlySummaryResponse;
}

export interface TopProductResponse {
  productId: number;
  productName: string;
  totalQuantity: number;
  totalRevenue: number;
}

export interface TopCustomerResponse {
  userId: number;
  name: string;
  lastName: string;
  totalOrders: number;
  totalSpent: number;
}

export interface DailySummaryResponse {
  ordersCount: number;
  deliveriesCount: number;
  reservationsCount: number;
  revenue: number;
  expenses: number;
  profit: number;
}

export interface MonthlySummaryResponse {
  ordersCount: number;
  deliveriesCount: number;
  reservationsCount: number;
  revenue: number;
  expenses: number;
  profit: number;
  month: number;
  year: number;
}

