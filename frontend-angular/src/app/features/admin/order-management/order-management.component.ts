import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { NotificationService } from '../../../core/services/notification.service';
import { OrderType, UnifiedOrder, PaymentStatus } from '../../../core/models/order.model';

interface OrderSearchRequest {
  orderId?: number | string;
  clientName?: string;
  clientEmail?: string;
  status?: boolean | 'all';
  paymentStatus?: PaymentStatus | 'all';
  minDate?: string;
  maxDate?: string;
  minPrice?: number;
  maxPrice?: number;
  tableNumber?: number;
}

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './order-management.component.html',
  styleUrl: './order-management.component.css'
})
export class OrderManagementComponent implements OnInit {
  allOrders = signal<UnifiedOrder[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  filterType = signal<OrderType | 'all'>('all');
  filterPaymentStatus = signal<PaymentStatus | 'all'>('all');
  OrderType = OrderType;
  PaymentStatus = PaymentStatus;

  // Búsqueda avanzada
  showAdvancedSearch = signal<boolean>(false);
  searchRequest = signal<OrderSearchRequest>({});
  searchTerm = signal<string>('');
  copiedOrderId = signal<number | null>(null);

  // Para ver comprobantes
  selectedProofImage = signal<string | null>(null);
  selectedOrderForProof = signal<UnifiedOrder | null>(null);

  constructor(
    private orderService: OrderService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const typeFilter = this.filterType();
    this.orderService.getAllUnifiedOrdersForAdmin(typeFilter).subscribe({
      next: (orders) => {
        // Debug: verificar datos de pago
        console.log('=== Orders loaded ===', orders.length);
        orders.forEach((order, index) => {
          console.log(`Order ${index + 1} (ID: ${order.id}):`, {
            paymentStatus: order.paymentStatus,
            paymentProofUrl: order.paymentProofUrl,
            paymentMethod: order.paymentMethod,
            hasPaymentStatus: !!order.paymentStatus,
            hasPaymentProofUrl: !!order.paymentProofUrl,
            paymentProofUrlType: typeof order.paymentProofUrl,
            paymentProofUrlValue: order.paymentProofUrl
          });
        });
        this.allOrders.set(orders);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading orders:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar pedidos. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  getFilteredOrders(): UnifiedOrder[] {
    let filtered = [...this.allOrders()];
    const search = this.searchRequest();
    const term = this.searchTerm().toLowerCase().trim();

    // Búsqueda simple por término
    if (term) {
      const termLower = term.toLowerCase().trim();
      filtered = filtered.filter(order => {
        const userNameMatch = order.userName.toLowerCase().includes(termLower);
        const userEmailMatch = order.userEmail.toLowerCase().includes(termLower);
        const idMatch = order.id.toString().includes(termLower);
        // Buscar también por formato M001 o D001
        const formattedIdMatch = this.formatOrderId(order.id, order.type).toLowerCase().includes(termLower);
        // Buscar solo por número si el término es numérico
        const numericMatch = !isNaN(Number(termLower)) && order.id.toString().includes(termLower);
        return userNameMatch || userEmailMatch || idMatch || formattedIdMatch || numericMatch;
      });
    }

    // Búsqueda avanzada
    if (search.orderId !== undefined && search.orderId !== null) {
      const searchValue = search.orderId.toString().toUpperCase();
      filtered = filtered.filter(order => {
        const formattedId = this.formatOrderId(order.id, order.type);
        // Si es un número, buscar por ID exacto
        if (!isNaN(Number(searchValue))) {
          return order.id === Number(searchValue);
        }
        // Si es un formato M001 o D001, buscar por formato exacto o parcial
        if (/^[MD]\d+$/.test(searchValue)) {
          return formattedId === searchValue || formattedId.includes(searchValue);
        }
        // Búsqueda parcial
        return formattedId.includes(searchValue) || order.id.toString().includes(searchValue);
      });
    }

    if (search.clientName) {
      const name = search.clientName.toLowerCase().trim();
      filtered = filtered.filter(order =>
        order.userName.toLowerCase().includes(name)
      );
    }

    if (search.clientEmail) {
      const email = search.clientEmail.toLowerCase().trim();
      filtered = filtered.filter(order =>
        order.userEmail.toLowerCase().includes(email)
      );
    }

    if (search.status !== undefined && search.status !== 'all') {
      filtered = filtered.filter(order => order.status === search.status);
    }

    // Filtro por estado de pago
    if (search.paymentStatus !== undefined && search.paymentStatus !== 'all') {
      filtered = filtered.filter(order => order.paymentStatus?.toString() === search.paymentStatus?.toString());
    }
    
    // Filtro general de estado de pago
    if (this.filterPaymentStatus() !== 'all') {
      const filterStatus = this.filterPaymentStatus()?.toString();
      filtered = filtered.filter(order => order.paymentStatus?.toString() === filterStatus);
    }

    if (search.minDate) {
      filtered = filtered.filter(order => order.date >= search.minDate!);
    }

    if (search.maxDate) {
      filtered = filtered.filter(order => order.date <= search.maxDate!);
    }

    if (search.minPrice !== undefined) {
      filtered = filtered.filter(order => order.totalPrice >= search.minPrice!);
    }

    if (search.maxPrice !== undefined) {
      filtered = filtered.filter(order => order.totalPrice <= search.maxPrice!);
    }

    if (search.tableNumber !== undefined && search.tableNumber !== null) {
      filtered = filtered.filter(order =>
        order.type === OrderType.EN_MESA && order.tableNumber === search.tableNumber
      );
    }

    return filtered;
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
  }

  toggleAdvancedSearch(): void {
    this.showAdvancedSearch.set(!this.showAdvancedSearch());
  }

  closeAdvancedSearch(): void {
    this.showAdvancedSearch.set(false);
  }

  clearAdvancedFilters(): void {
    this.searchRequest.set({});
  }

  applyAdvancedSearch(): void {
    this.closeAdvancedSearch();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.searchRequest.set({});
  }

  updateSearchField(field: keyof OrderSearchRequest, value: any): void {
    let processedValue: any = value;
    
    // Procesar valores numéricos
    if (field === 'orderId') {
      // Para orderId, puede ser un número o un formato M001/D001
      if (value && value !== '') {
        const valueStr = value.toString().trim().toUpperCase();
        // Si es un formato M001 o D001, extraer el número
        if (/^[MD]\d+$/.test(valueStr)) {
          processedValue = parseInt(valueStr.substring(1), 10);
        } else if (!isNaN(Number(valueStr))) {
          // Si es un número puro
          processedValue = parseInt(valueStr, 10);
        } else {
          // Si no es un formato válido, mantener como string para búsqueda
          processedValue = valueStr;
        }
      } else {
        processedValue = undefined;
      }
    } else if (field === 'minPrice' || field === 'maxPrice') {
      processedValue = value && value !== '' ? parseFloat(value) : undefined;
    } else if (field === 'tableNumber') {
      processedValue = value && value !== '' ? parseInt(value, 10) : undefined;
    }
    
    this.searchRequest.update(r => ({ ...r, [field]: processedValue }));
  }

  hasActiveFilters(): boolean {
    const search = this.searchRequest();
    return Object.keys(search).length > 0;
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  formatTime(time: string): string {
    return time.substring(0, 5); // HH:MM
  }

  getOrderTypeLabel(type: OrderType): string {
    return type === OrderType.EN_MESA ? 'En Mesa' : 'Domicilio';
  }

  confirmOrder(id: number, type: OrderType): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.orderService.updateUnifiedOrderStatusAsAdmin(id, type, true).subscribe({
      next: () => {
        this.loadOrders();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error?.message || 'Error al confirmar pedido');
        this.loading.set(false);
        console.error('Error confirming order:', error);
      }
    });
  }

  formatOrderId(id: number, orderType?: OrderType): string {
    // Determinar prefijo basado en el tipo de pedido
    const prefix = orderType === OrderType.EN_MESA ? 'M' : 'D';
    // Formatear ID con prefijo y padding de 3 dígitos (ej: M001, D023, M1234)
    return `${prefix}${id.toString().padStart(3, '0')}`;
  }

  copyOrderIdToClipboard(orderId: number, orderType: OrderType, event: Event): void {
    event.stopPropagation();
    const formattedId = this.formatOrderId(orderId, orderType);
    
    navigator.clipboard.writeText(formattedId).then(() => {
      this.copiedOrderId.set(orderId);
      setTimeout(() => {
        this.copiedOrderId.set(null);
      }, 2000);
    }).catch(err => {
      console.error('Error al copiar ID:', err);
      // Fallback para navegadores antiguos
      const textArea = document.createElement('textarea');
      textArea.value = formattedId;
      document.body.appendChild(textArea);
      textArea.select();
      try {
        document.execCommand('copy');
        this.copiedOrderId.set(orderId);
        setTimeout(() => {
          this.copiedOrderId.set(null);
        }, 2000);
      } catch (fallbackErr) {
        console.error('Error al copiar con fallback:', fallbackErr);
      }
      document.body.removeChild(textArea);
    });
  }

  viewPaymentProof(order: UnifiedOrder): void {
    if (order.paymentProofUrl) {
      this.selectedProofImage.set(order.paymentProofUrl);
      this.selectedOrderForProof.set(order);
    }
  }

  closeProofModal(): void {
    this.selectedProofImage.set(null);
    this.selectedOrderForProof.set(null);
  }

  verifyPayment(orderId: number, isDelivery: boolean = false): void {
    if (confirm('¿Estás seguro de que deseas verificar este pago?')) {
      this.loading.set(true);
      const verification$ = isDelivery
        ? this.orderService.verifyDeliveryPayment(orderId)
        : this.orderService.verifyPayment(orderId);

      verification$.subscribe({
        next: () => {
          this.notificationService.success('Pago verificado exitosamente');
          this.loadOrders();
          this.closeProofModal();
        },
        error: (error) => {
          console.error('Error verifying payment:', error);
          const errorMessage = error.error?.message || 'Error al verificar el pago';
          this.notificationService.error(errorMessage);
          this.loading.set(false);
        }
      });
    }
  }

  rejectPayment(orderId: number, isDelivery: boolean = false): void {
    const reason = prompt('¿Motivo del rechazo? (opcional)');
    if (confirm('¿Estás seguro de que deseas rechazar este pago?')) {
      this.loading.set(true);
      const rejection$ = isDelivery
        ? this.orderService.rejectDeliveryPayment(orderId)
        : this.orderService.rejectPayment(orderId, reason || undefined);

      rejection$.subscribe({
        next: () => {
          this.notificationService.success('Pago rechazado exitosamente');
          this.loadOrders();
          this.closeProofModal();
        },
        error: (error) => {
          console.error('Error rejecting payment:', error);
          const errorMessage = error.error?.message || 'Error al rechazar el pago';
          this.notificationService.error(errorMessage);
          this.loading.set(false);
        }
      });
    }
  }

  getPaymentMethodLabel(method?: string): string {
    const labels: { [key: string]: string } = {
      'CASH': 'Efectivo',
      'NEQUI': 'Nequi',
      'DAVIPLATA': 'Daviplata',
      'BANK_TRANSFER': 'Transferencia Bancaria',
      'CARD': 'Tarjeta'
    };
    return labels[method || ''] || method || 'No especificado';
  }

  getPaymentStatusLabel(status?: PaymentStatus | string | null): string {
    if (!status) {
      return 'Sin estado';
    }
    
    // Convertir a string
    const statusStr = String(status).toUpperCase();
    
    const labels: { [key: string]: string } = {
      'PENDING': 'Pendiente',
      'VERIFIED': 'Verificado',
      'REJECTED': 'Rechazado',
      'PAID': 'Pagado',
      'FAILED': 'Fallido',
      'CANCELLED': 'Cancelado'
    };
    
    const label = labels[statusStr] || statusStr;
    return label;
  }

  isPaymentPending(order: UnifiedOrder): boolean {
    const status = order.paymentStatus?.toString();
    return status === PaymentStatus.PENDING.toString() || !order.paymentStatus;
  }

  isPaymentVerified(order: UnifiedOrder): boolean {
    const status = order.paymentStatus?.toString();
    return status === PaymentStatus.VERIFIED.toString();
  }
}

