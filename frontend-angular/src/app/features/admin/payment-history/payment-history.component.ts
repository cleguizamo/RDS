import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { forkJoin } from 'rxjs';
import { OrderService } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Order, PaymentStatus } from '../../../core/models/order.model';

@Component({
  selector: 'app-payment-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './payment-history.component.html',
  styleUrl: './payment-history.component.css'
})
export class PaymentHistoryComponent implements OnInit {
  verifiedOrders = signal<Order[]>([]);
  loading = signal<boolean>(false);
  selectedOrder = signal<Order | null>(null);
  showProofModal = signal<boolean>(false);

  constructor(
    private orderService: OrderService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadVerifiedPayments();
  }

  loadVerifiedPayments(): void {
    this.loading.set(true);
    
    // Cargar orders y deliveries verificados en paralelo
    forkJoin({
      orders: this.orderService.getOrdersWithVerifiedPayments(),
      deliveries: this.orderService.getDeliveriesWithVerifiedPayments()
    }).subscribe({
      next: ({ orders, deliveries }) => {
        // Combinar orders y deliveries
        const allVerifiedPayments = [...orders, ...deliveries];
        console.log('Pagos verificados recibidos:', allVerifiedPayments);
        
        // Ordenar por fecha de verificación descendente (más recientes primero)
        const sortedPayments = allVerifiedPayments.sort((a, b) => {
          const dateA = a.verifiedAt ? new Date(a.verifiedAt).getTime() : 0;
          const dateB = b.verifiedAt ? new Date(b.verifiedAt).getTime() : 0;
          return dateB - dateA;
        });
        this.verifiedOrders.set(sortedPayments);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error al cargar historial de pagos:', error);
        console.error('Detalles del error:', {
          status: error.status,
          statusText: error.statusText,
          message: error.message,
          error: error.error
        });
        const errorMessage = error.error?.message || error.message || 'Error desconocido al cargar el historial de pagos';
        this.notificationService.error(`Error al cargar el historial de pagos: ${errorMessage}`);
        this.loading.set(false);
      }
    });
  }

  viewProof(order: Order): void {
    if (order.paymentProofUrl) {
      this.selectedOrder.set(order);
      this.showProofModal.set(true);
    }
  }

  closeModal(): void {
    this.showProofModal.set(false);
    this.selectedOrder.set(null);
  }

  formatDate(dateString: string | undefined): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('es-CO', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 0
    }).format(amount);
  }

  getPaymentMethodLabel(method: string | undefined): string {
    const methods: { [key: string]: string } = {
      'CASH': 'Efectivo',
      'NEQUI': 'Nequi',
      'DAVIPLATA': 'Daviplata',
      'BANK_TRANSFER': 'Transferencia Bancaria',
      'CARD': 'Tarjeta'
    };
    return method ? methods[method] || method : 'N/A';
  }
}
