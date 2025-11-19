import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { OrderService } from '../../../core/services/order.service';
import { OrderType, UnifiedOrder } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './order-management.component.html',
  styleUrl: './order-management.component.css'
})
export class OrderManagementComponent implements OnInit {
  orders = signal<UnifiedOrder[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  filterType = signal<OrderType | 'all'>('all');
  OrderType = OrderType;

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const typeFilter = this.filterType();
    this.orderService.getAllUnifiedOrdersForAdmin(typeFilter).subscribe({
      next: (orders) => {
        this.orders.set(orders);
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
    return this.orders();
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
}

