import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { CartService } from '../../../core/services/cart.service';
import { Order, OrderType, OrderItemRequest } from '../../../core/models/order.model';

@Component({
  selector: 'app-order-history',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './order-history.component.html',
  styleUrl: './order-history.component.css'
})
export class OrderHistoryComponent implements OnInit {
  // Exponer OrderType al template
  OrderType = OrderType;

  orders = signal<Order[]>([]);
  deliveries = signal<Order[]>([]);
  allOrders = computed(() => [...this.orders(), ...this.deliveries()]);
  
  filteredOrders = signal<Order[]>([]);
  
  selectedOrderType = signal<'all' | OrderType>('all');
  selectedStatus = signal<'all' | boolean>('all');
  searchDate = signal<string>('');
  
  loading = signal(false);
  selectedOrder = signal<Order | null>(null);
  showOrderDetails = signal(false);

  constructor(
    private orderService: OrderService,
    public authService: AuthService,
    private notificationService: NotificationService,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    const user = this.authService.currentUser();
    if (!user || !user.id) {
      this.notificationService.error('Error al obtener información del usuario');
      return;
    }

    // Limpiar notificaciones previas antes de cargar
    this.notificationService.clear();
    this.loading.set(true);

    // Cargar pedidos en mesa y domicilios en paralelo
    const ordersRequest = this.orderService.getClientOrders(user.id);
    const deliveriesRequest = this.orderService.getClientDeliveries(user.id);

    let ordersLoaded = false;
    let deliveriesLoaded = false;
    let ordersSuccess = false;
    let deliveriesSuccess = false;
    let ordersError: any = null;
    let deliveriesError: any = null;

    const checkAndSetLoading = () => {
      if (ordersLoaded && deliveriesLoaded) {
        this.loading.set(false);
        
        // Solo mostrar error si ambas peticiones fallaron
        // Si al menos una petición fue exitosa, NO mostrar error
        const hasAnySuccess = ordersSuccess || deliveriesSuccess;
        
        console.log('Order loading completed:', {
          ordersSuccess,
          deliveriesSuccess,
          hasAnySuccess,
          ordersError: ordersError?.status,
          deliveriesError: deliveriesError?.status,
          ordersCount: this.orders().length,
          deliveriesCount: this.deliveries().length
        });
        
        // Solo mostrar error si NO hay éxito en ninguna petición
        if (!hasAnySuccess && (ordersError || deliveriesError)) {
          // Ambas peticiones fallaron, mostrar error
          const error = ordersError || deliveriesError;
          console.warn('Showing error because both requests failed:', error.status);
          if (error.status === 403) {
            this.notificationService.error('No tienes permisos para ver los pedidos. Por favor, inicia sesión nuevamente.');
          } else if (error.status === 401) {
            this.notificationService.error('Tu sesión ha expirado. Por favor, inicia sesión nuevamente.');
          } else {
            this.notificationService.error('Error al cargar los pedidos. Verifica tu conexión o permisos.');
          }
        } else {
          console.log('No error shown because at least one request succeeded or no errors occurred');
        }
        // Si al menos una petición fue exitosa, NO mostrar ningún error
        // Esto evita mostrar errores cuando los datos se están cargando correctamente
        
        this.applyFilters();
      }
    };

    ordersRequest.subscribe({
      next: (orders) => {
        console.log('Pedidos cargados:', orders);
        // Mapear los pedidos para asegurar que tengan el tipo correcto y todos los campos necesarios
        const mappedOrders = (orders || []).map(order => {
          // Convertir string de tipo a enum si es necesario
          let orderType: OrderType = OrderType.EN_MESA;
          if (order.type) {
            if (typeof order.type === 'string') {
              orderType = order.type === 'EN_MESA' ? OrderType.EN_MESA : OrderType.DOMICILIO;
            } else {
              orderType = order.type;
            }
          }
          
          return {
            ...order,
            type: orderType,
            date: order.date || '',
            time: order.time || '',
            totalPrice: order.totalPrice || 0,
            status: order.status !== undefined ? order.status : false
          };
        });
        this.orders.set(mappedOrders);
        ordersSuccess = true;
        ordersError = null;
        ordersLoaded = true;
        checkAndSetLoading();
      },
      error: (error) => {
        console.error('Error loading orders:', error);
        console.error('Error status:', error.status);
        console.error('Error details:', JSON.stringify(error, null, 2));
        
        this.orders.set([]);
        ordersSuccess = false;
        ordersError = error;
        ordersLoaded = true;
        checkAndSetLoading();
      }
    });

    deliveriesRequest.subscribe({
      next: (deliveries) => {
        console.log('Domicilios cargados:', deliveries);
        // Mapear los domicilios para asegurar que tengan el tipo correcto y todos los campos necesarios
        const mappedDeliveries = (deliveries || []).map(delivery => {
          // Convertir string de tipo a enum si es necesario
          let orderType: OrderType = OrderType.DOMICILIO;
          if (delivery.type) {
            if (typeof delivery.type === 'string') {
              orderType = delivery.type === 'DOMICILIO' ? OrderType.DOMICILIO : OrderType.EN_MESA;
            } else {
              orderType = delivery.type;
            }
          }
          
          return {
            ...delivery,
            type: orderType,
            date: delivery.date || '',
            time: delivery.time || '',
            totalPrice: delivery.totalPrice || 0,
            status: delivery.status !== undefined ? delivery.status : false
          };
        });
        this.deliveries.set(mappedDeliveries);
        deliveriesSuccess = true;
        deliveriesError = null;
        deliveriesLoaded = true;
        checkAndSetLoading();
      },
      error: (error) => {
        console.error('Error loading deliveries:', error);
        console.error('Error status:', error.status);
        console.error('Error details:', JSON.stringify(error, null, 2));
        
        this.deliveries.set([]);
        deliveriesSuccess = false;
        deliveriesError = error;
        deliveriesLoaded = true;
        checkAndSetLoading();
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.allOrders()];

    // Filtrar por tipo
    const selectedType = this.selectedOrderType();
    if (selectedType !== 'all') {
      filtered = filtered.filter(order => {
        // Comparar tanto con enum como con string para compatibilidad
        const orderType = order.type;
        return orderType === selectedType || 
               (typeof orderType === 'string' && orderType === selectedType) ||
               (typeof selectedType === 'string' && orderType === selectedType) ||
               orderType?.toString() === selectedType?.toString();
      });
    }

    // Filtrar por estado
    const selectedStatusValue = this.selectedStatus();
    if (selectedStatusValue !== 'all') {
      // selectedStatusValue ya es boolean cuando no es 'all'
      filtered = filtered.filter(order => order.status === selectedStatusValue);
    }

    // Filtrar por fecha
    const searchDateValue = this.searchDate();
    if (searchDateValue) {
      // Normalizar el formato de fecha para comparación
      const searchDateNormalized = this.normalizeDate(searchDateValue);
      filtered = filtered.filter(order => {
        if (!order.date) return false;
        const orderDateNormalized = this.normalizeDate(order.date);
        return orderDateNormalized === searchDateNormalized;
      });
    }

    // Ordenar por fecha descendente (más recientes primero)
    filtered.sort((a, b) => {
      try {
        const dateA = this.parseDate(a.date, a.time);
        const dateB = this.parseDate(b.date, b.time);
        if (!dateA || !dateB) return 0;
        return dateB.getTime() - dateA.getTime();
      } catch (e) {
        return 0;
      }
    });

    this.filteredOrders.set(filtered);
  }

  private normalizeDate(dateStr: string): string {
    if (!dateStr) return '';
    // Convertir diferentes formatos de fecha a YYYY-MM-DD
    try {
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return dateStr;
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    } catch (e) {
      return dateStr;
    }
  }

  private parseDate(dateStr: string, timeStr?: string): Date | null {
    if (!dateStr) return null;
    try {
      if (timeStr) {
        return new Date(`${dateStr}T${timeStr}`);
      }
      return new Date(dateStr);
    } catch (e) {
      return null;
    }
  }

  onOrderTypeChange(value: string): void {
    if (value === 'all') {
      this.selectedOrderType.set('all');
    } else if (value === OrderType.EN_MESA || value === 'EN_MESA') {
      this.selectedOrderType.set(OrderType.EN_MESA);
    } else if (value === OrderType.DOMICILIO || value === 'DOMICILIO') {
      this.selectedOrderType.set(OrderType.DOMICILIO);
    } else {
      this.selectedOrderType.set(value as OrderType);
    }
    this.applyFilters();
  }

  onStatusChange(value: string): void {
    if (value === 'all') {
      this.selectedStatus.set('all');
    } else if (value === 'true') {
      this.selectedStatus.set(true);
    } else if (value === 'false') {
      this.selectedStatus.set(false);
    } else {
      // Por defecto, convertir a boolean
      this.selectedStatus.set(value === 'true');
    }
    this.applyFilters();
  }

  onDateChange(value: string): void {
    this.searchDate.set(value);
    this.applyFilters();
  }

  getStatusValue(): string {
    const status = this.selectedStatus();
    if (status === 'all') return 'all';
    return status ? 'true' : 'false';
  }

  clearFilters(): void {
    this.selectedOrderType.set('all');
    this.selectedStatus.set('all');
    this.searchDate.set('');
    this.applyFilters();
  }

  viewOrderDetails(order: Order): void {
    this.selectedOrder.set(order);
    this.showOrderDetails.set(true);
  }

  closeOrderDetails(): void {
    this.showOrderDetails.set(false);
    this.selectedOrder.set(null);
  }

  reorder(order: Order): void {
    if (!order.items || order.items.length === 0) {
      this.notificationService.warning('Este pedido no tiene items para reordenar');
      return;
    }

    // Agregar todos los items al carrito
    order.items.forEach(item => {
      const product = {
        id: item.productId,
        name: item.productName,
        description: '',
        imageUrl: '',
        price: item.productPrice,
        categoryId: 0,
        categoryName: '',
        stock: 1000, // Asumimos que hay stock suficiente para reordenar
        subCategoryId: undefined,
        subCategoryName: undefined
      };
      this.cartService.addItem(product, item.quantity);
    });

    this.notificationService.success('Productos agregados al carrito');
    this.closeOrderDetails();
  }

  getOrderTypeLabel(type: OrderType): string {
    return type === OrderType.EN_MESA ? 'En Mesa' : 'Domicilio';
  }

  getStatusLabel(status: boolean): string {
    return status ? 'Completado' : 'Pendiente';
  }

  getStatusClass(status: boolean): string {
    return status ? 'status-completed' : 'status-pending';
  }
}

