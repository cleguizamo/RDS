import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BalanceService } from '../../../core/services/balance.service';
import {
  BalanceResponse,
  Transaction,
  TransactionType,
  Alert,
  AlertType,
  AlertStatus,
  BalanceInitializationRequest,
  BalanceAdjustmentRequest
} from '../../../core/models/balance.model';
import { SalaryPayment, PaymentStatus } from '../../../core/models/employee.model';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency.pipe';

@Component({
  selector: 'app-balance-management',
  standalone: true,
  imports: [CommonModule, FormsModule, FormatCurrencyPipe],
  templateUrl: './balance-management.component.html',
  styleUrl: './balance-management.component.css'
})
export class BalanceManagementComponent implements OnInit {
  // Hacer los enums disponibles en el template
  TransactionType = TransactionType;
  PaymentStatus = PaymentStatus;
  
  balance = signal<BalanceResponse | null>(null);
  transactions = signal<Transaction[]>([]);
  alerts = signal<Alert[]>([]);
  pendingPayments = signal<SalaryPayment[]>([]);
  
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  
  // Modales
  showInitModal = signal<boolean>(false);
  showAdjustModal = signal<boolean>(false);
  showTransactionsModal = signal<boolean>(false);
  showAlertsModal = signal<boolean>(false);
  showPendingPaymentsModal = signal<boolean>(false);
  
  // Formularios
  initForm = signal<BalanceInitializationRequest>({
    initialBalance: 0,
    lowBalanceThreshold: 100000
  });
  
  adjustForm = signal<BalanceAdjustmentRequest>({
    amount: 0,
    reason: '',
    notes: ''
  });
  
  selectedTransactionType = signal<TransactionType | undefined>(undefined);

  constructor(private balanceService: BalanceService) {}

  ngOnInit(): void {
    this.loadBalance();
    this.loadAlerts(true); // Solo alertas activas por defecto
  }

  loadBalance(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.balanceService.getCurrentBalance().subscribe({
      next: (balance) => {
        this.balance.set(balance);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading balance:', error);
        this.errorMessage.set('Error al cargar el balance');
        this.loading.set(false);
      }
    });
  }

  loadTransactions(type?: TransactionType): void {
    this.selectedTransactionType.set(type);
    this.loading.set(true);
    
    this.balanceService.getAllTransactions(type).subscribe({
      next: (transactions) => {
        this.transactions.set(transactions);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading transactions:', error);
        this.errorMessage.set('Error al cargar las transacciones');
        this.loading.set(false);
      }
    });
  }

  loadTransactionsByType(type: TransactionType): void {
    this.loadTransactions(type);
  }

  loadAlerts(activeOnly: boolean = false): void {
    this.balanceService.getAlerts(activeOnly).subscribe({
      next: (alerts) => {
        this.alerts.set(alerts);
      },
      error: (error) => {
        console.error('Error loading alerts:', error);
      }
    });
  }

  loadPendingPayments(): void {
    this.loading.set(true);
    this.balanceService.getPendingPayments().subscribe({
      next: (payments) => {
        this.pendingPayments.set(payments);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading pending payments:', error);
        this.errorMessage.set('Error al cargar los pagos pendientes');
        this.loading.set(false);
      }
    });
  }

  openInitModal(): void {
    this.showInitModal.set(true);
  }

  closeInitModal(): void {
    this.showInitModal.set(false);
    this.initForm.set({ initialBalance: 0, lowBalanceThreshold: 100000 });
  }

  initializeBalance(): void {
    if (!this.initForm().initialBalance || !this.initForm().lowBalanceThreshold) {
      this.errorMessage.set('Por favor completa todos los campos');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.balanceService.initializeBalance(this.initForm()).subscribe({
      next: (balance) => {
        this.balance.set(balance);
        this.closeInitModal();
        this.successMessage.set('Balance inicializado correctamente');
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        console.error('Error initializing balance:', error);
        this.errorMessage.set(error.error?.message || 'Error al inicializar el balance');
        this.loading.set(false);
      }
    });
  }

  openAdjustModal(): void {
    this.showAdjustModal.set(true);
  }

  closeAdjustModal(): void {
    this.showAdjustModal.set(false);
    this.adjustForm.set({ amount: 0, reason: '', notes: '' });
  }

  adjustBalance(): void {
    if (!this.adjustForm().amount || !this.adjustForm().reason) {
      this.errorMessage.set('Por favor completa todos los campos requeridos');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.balanceService.adjustBalance(this.adjustForm()).subscribe({
      next: (transaction) => {
        this.loadBalance();
        this.loadTransactions(this.selectedTransactionType());
        this.closeAdjustModal();
        this.successMessage.set('Balance ajustado correctamente');
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        console.error('Error adjusting balance:', error);
        this.errorMessage.set(error.error?.message || 'Error al ajustar el balance');
        this.loading.set(false);
      }
    });
  }

  openTransactionsModal(): void {
    this.showTransactionsModal.set(true);
    this.loadTransactions();
  }

  closeTransactionsModal(): void {
    this.showTransactionsModal.set(false);
    this.selectedTransactionType.set(undefined);
  }

  openAlertsModal(): void {
    this.showAlertsModal.set(true);
    this.loadAlerts(false); // Cargar todas las alertas
  }

  closeAlertsModal(): void {
    this.showAlertsModal.set(false);
  }

  resolveAlert(alertId: number): void {
    this.balanceService.resolveAlert(alertId).subscribe({
      next: () => {
        this.loadAlerts(false);
        this.loadBalance();
        this.successMessage.set('Alerta resuelta correctamente');
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        console.error('Error resolving alert:', error);
        this.errorMessage.set('Error al resolver la alerta');
      }
    });
  }

  openPendingPaymentsModal(): void {
    this.showPendingPaymentsModal.set(true);
    this.loadPendingPayments();
  }

  closePendingPaymentsModal(): void {
    this.showPendingPaymentsModal.set(false);
  }

  processPendingPayments(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.balanceService.processPendingPayments().subscribe({
      next: () => {
        this.loadPendingPayments();
        this.loadBalance();
        this.loadAlerts(true);
        this.successMessage.set('Pagos pendientes procesados correctamente');
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        console.error('Error processing pending payments:', error);
        this.errorMessage.set(error.error?.message || 'Error al procesar los pagos pendientes');
        this.loading.set(false);
      }
    });
  }

  processSalaryPayments(): void {
    if (!confirm('¿Estás seguro de que deseas procesar los sueldos programados para hoy? Esto descontará el dinero del balance.')) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.balanceService.processSalaryPayments().subscribe({
      next: () => {
        this.loadPendingPayments();
        this.loadBalance();
        this.loadAlerts(true);
        this.successMessage.set('Procesamiento de sueldos completado. Revisa los pagos pendientes si no se procesaron todos.');
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 5000);
      },
      error: (error) => {
        console.error('Error processing salary payments:', error);
        this.errorMessage.set(error.error?.message || 'Error al procesar los sueldos');
        this.loading.set(false);
      }
    });
  }

  migrateHistoricalData(): void {
    if (!confirm('¿Estás seguro de que deseas migrar los datos históricos? Esto creará transacciones para todos los pedidos y entregas completados históricamente y recalculará el balance. Esta operación puede tardar unos minutos.')) {
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.balanceService.migrateHistoricalData().subscribe({
      next: (result) => {
        this.loadBalance();
        this.loadTransactions();
        this.loadAlerts(true);
        const formattedRevenue = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(result.totalRevenue);
        const formattedExpenses = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(result.totalExpenses);
        const formattedNet = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', minimumFractionDigits: 0, maximumFractionDigits: 0 }).format(result.netBalance);
        const message = `Migración completada:\n• ${result.ordersMigrated} pedidos migrados\n• ${result.deliveriesMigrated} entregas migradas\n• ${result.expensesMigrated} gastos migrados\n• Total ingresos: ${formattedRevenue}\n• Total gastos: ${formattedExpenses}\n• Balance neto: ${formattedNet}`;
        this.successMessage.set(message);
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 10000);
      },
      error: (error) => {
        console.error('Error migrating historical data:', error);
        this.errorMessage.set(error.error?.message || 'Error al migrar los datos históricos');
        this.loading.set(false);
      }
    });
  }

  updateThreshold(): void {
    const threshold = prompt('Ingresa el nuevo umbral de saldo bajo:');
    if (!threshold || isNaN(Number(threshold))) {
      return;
    }

    this.loading.set(true);
    this.balanceService.updateLowBalanceThreshold(Number(threshold)).subscribe({
      next: (balance) => {
        this.balance.set(balance);
        this.successMessage.set('Umbral actualizado correctamente');
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        console.error('Error updating threshold:', error);
        this.errorMessage.set('Error al actualizar el umbral');
        this.loading.set(false);
      }
    });
  }

  getTransactionTypeLabel(type: TransactionType): string {
    const labels: Record<TransactionType, string> = {
      INCOME: 'Ingreso',
      EXPENSE: 'Gasto',
      SALARY_PAYMENT: 'Pago de Sueldo',
      ADJUSTMENT: 'Ajuste',
      REFUND: 'Reembolso'
    };
    return labels[type] || type;
  }

  getTransactionTypeClass(type: TransactionType): string {
    return type === TransactionType.INCOME || type === TransactionType.REFUND 
      ? 'positive' 
      : 'negative';
  }

  getAlertTypeLabel(type: AlertType): string {
    const labels: Record<AlertType, string> = {
      LOW_BALANCE: 'Saldo Insuficiente',
      BALANCE_THRESHOLD: 'Umbral de Saldo',
      PENDING_PAYMENTS: 'Pagos Pendientes'
    };
    return labels[type] || type;
  }

  getAlertSeverityClass(severity: string): string {
    return severity.toLowerCase();
  }

  getPaymentStatusLabel(status: PaymentStatus | undefined): string {
    const actualStatus = status || PaymentStatus.PENDING;
    const labels: Record<PaymentStatus, string> = {
      PENDING: 'Pendiente',
      PAID: 'Pagado',
      FAILED: 'Fallido',
      CANCELLED: 'Cancelado'
    };
    return labels[actualStatus] || actualStatus;
  }

  getPaymentStatusClass(status: PaymentStatus | undefined): string {
    const actualStatus = status || PaymentStatus.PENDING;
    return actualStatus.toLowerCase();
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  getActiveAlertsCount(): number {
    return this.alerts().filter(a => a.status === AlertStatus.ACTIVE).length;
  }

  getPendingPaymentsCount(): number {
    return this.pendingPayments().length;
  }

}

