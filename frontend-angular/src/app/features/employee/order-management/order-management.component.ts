import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { OrderService } from '../../../core/services/order.service';
import { EmployeeService } from '../../../core/services/employee.service';
import { ProductService } from '../../../core/services/product.service';
import { AuthService } from '../../../core/services/auth.service';
import { Order, OrderRequest, DeliveryRequest, OrderItemRequest, OrderType, UnifiedOrder, PaymentMethod } from '../../../core/models/order.model';
import { ProductResponse } from '../../../core/models/product.model';
import { UserResponse } from '../../../core/models/user.model';

interface CartItem {
  product: ProductResponse;
  quantity: number;
}

@Component({
  selector: 'app-order-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './order-management.component.html',
  styleUrl: './order-management.component.css'
})
export class OrderManagementComponent implements OnInit {
  orders = signal<UnifiedOrder[]>([]);
  filteredOrders = signal<UnifiedOrder[]>([]);
  users = signal<UserResponse[]>([]);
  products = signal<ProductResponse[]>([]);
  cart = signal<CartItem[]>([]);
  loading = signal<boolean>(false);
  loadingUsers = signal<boolean>(false);
  loadingProducts = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  filterStatus = signal<'all' | 'pending' | 'completed'>('all');
  filterType = signal<OrderType | 'all'>('all');
  showForm = signal<boolean>(false);
  selectedCategory = signal<string>('all');
  categories = signal<string[]>([]);
  orderForm: FormGroup;
  OrderType = OrderType;

  constructor(
    private orderService: OrderService,
    private employeeService: EmployeeService,
    private productService: ProductService,
    private authService: AuthService,
    private fb: FormBuilder
  ) {
    this.orderForm = this.fb.group({
      userId: ['', [Validators.required]],
      orderType: [OrderType.EN_MESA, [Validators.required]],
      tableNumber: [''],
      deliveryAddress: [''],
      deliveryPhone: ['']
    });

    // Validaciones condicionales
    this.orderForm.get('orderType')?.valueChanges.subscribe(type => {
      const tableNumberControl = this.orderForm.get('tableNumber');
      const deliveryAddressControl = this.orderForm.get('deliveryAddress');
      const deliveryPhoneControl = this.orderForm.get('deliveryPhone');

      if (type === OrderType.EN_MESA) {
        tableNumberControl?.setValidators([Validators.required, Validators.min(1)]);
        deliveryAddressControl?.clearValidators();
        deliveryPhoneControl?.clearValidators();
      } else {
        tableNumberControl?.clearValidators();
        deliveryAddressControl?.setValidators([Validators.required]);
        deliveryPhoneControl?.setValidators([Validators.required]);
      }

      tableNumberControl?.updateValueAndValidity();
      deliveryAddressControl?.updateValueAndValidity();
      deliveryPhoneControl?.updateValueAndValidity();
    });
  }

  ngOnInit(): void {
    // Esperar un momento para asegurar que el token se haya guardado después del login
    // Esto es necesario porque después del login, el token se guarda en localStorage
    // pero el componente puede inicializarse antes de que se complete la escritura
    setTimeout(() => {
      this.loadOrders();
      this.loadUsers();
      this.loadProducts();
    }, 100);
  }

  loadUsers(): void {
    this.loadingUsers.set(true);
    this.employeeService.getAllUsers().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loadingUsers.set(false);
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.errorMessage.set('Error al cargar los clientes. Verifica tu conexión o permisos.');
        this.loadingUsers.set(false);
      }
    });
  }

  loadProducts(): void {
    this.loadingProducts.set(true);
    this.productService.getAllPublicProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        const uniqueCategories = [...new Set(products.map(p => p.categoryName))];
        this.categories.set(uniqueCategories);
        this.loadingProducts.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.errorMessage.set('Error al cargar los productos.');
        this.loadingProducts.set(false);
      }
    });
  }

  getFilteredProducts(): ProductResponse[] {
    const category = this.selectedCategory();
    if (category === 'all') {
      return this.products();
    }
    return this.products().filter(p => p.categoryName === category);
  }

  getProductStock(productId: number): number {
    const product = this.products().find(p => p.id === productId);
    return product?.stock || 0;
  }

  getCartItemQuantity(productId: number): number {
    const item = this.cart().find(c => c.product.id === productId);
    return item?.quantity || 0;
  }

  addToCart(product: ProductResponse, quantity: number = 1): void {
    if (quantity < 1 || quantity > product.stock) {
      this.errorMessage.set(`Cantidad inválida. Stock disponible: ${product.stock}`);
      setTimeout(() => this.errorMessage.set(null), 3000);
      return;
    }

    const currentCart = [...this.cart()];
    const existingItemIndex = currentCart.findIndex(item => item.product.id === product.id);

    if (existingItemIndex >= 0) {
      const newQuantity = currentCart[existingItemIndex].quantity + quantity;
      if (newQuantity > product.stock) {
        this.errorMessage.set(`No hay suficiente stock. Stock disponible: ${product.stock}`);
        setTimeout(() => this.errorMessage.set(null), 3000);
        return;
      }
      currentCart[existingItemIndex].quantity = newQuantity;
    } else {
      currentCart.push({ product, quantity });
    }

    this.cart.set(currentCart);
    this.errorMessage.set(null);
  }

  updateCartItemQuantity(productId: number, quantity: number): void {
    if (quantity < 1) {
      this.removeFromCart(productId);
      return;
    }

    const product = this.products().find(p => p.id === productId);
    if (!product || quantity > product.stock) {
      this.errorMessage.set(`Cantidad inválida. Stock disponible: ${product?.stock || 0}`);
      setTimeout(() => this.errorMessage.set(null), 3000);
      return;
    }

    const currentCart = [...this.cart()];
    const itemIndex = currentCart.findIndex(item => item.product.id === productId);
    if (itemIndex >= 0) {
      currentCart[itemIndex].quantity = quantity;
      this.cart.set(currentCart);
    }
    this.errorMessage.set(null);
  }

  removeFromCart(productId: number): void {
    const currentCart = this.cart().filter(item => item.product.id !== productId);
    this.cart.set(currentCart);
  }

  calculateTotal(): number {
    return this.cart().reduce((total, item) => {
      return total + (item.product.price * item.quantity);
    }, 0);
  }

  toggleForm(): void {
    this.showForm.set(!this.showForm());
    if (!this.showForm()) {
      this.orderForm.reset({
        orderType: OrderType.EN_MESA
      });
      this.cart.set([]);
      this.selectedCategory.set('all');
      this.errorMessage.set(null);
      this.successMessage.set(null);
    }
  }

  onSubmit(): void {
    if (this.orderForm.invalid) {
      this.markFormGroupTouched(this.orderForm);
      return;
    }

    if (this.cart().length === 0) {
      this.errorMessage.set('Debes agregar al menos un producto al pedido.');
      return;
    }

    // Validar stock antes de enviar
    for (const item of this.cart()) {
      if (item.quantity > item.product.stock) {
        this.errorMessage.set(`El producto "${item.product.name}" no tiene suficiente stock. Stock disponible: ${item.product.stock}`);
        return;
      }
    }

    const formValue = this.orderForm.value;
    const items: OrderItemRequest[] = this.cart().map(item => ({
      productId: item.product.id!,
      quantity: item.quantity
    }));

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const orderType = formValue.orderType;

    if (orderType === OrderType.EN_MESA) {
      const orderRequest: OrderRequest = {
        userId: parseInt(formValue.userId),
        items: items,
        tableNumber: parseInt(formValue.tableNumber),
        paymentMethod: PaymentMethod.CASH // Los empleados siempre registran pagos en efectivo
      };

      this.orderService.createOrderAsEmployee(orderRequest).subscribe({
        next: () => {
          this.successMessage.set('¡Pedido en mesa creado exitosamente!');
          this.resetForm();
          this.loadOrders();
          this.loadProducts();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error creating order:', error);
          this.errorMessage.set(error.error?.message || 'Error al crear el pedido. Por favor, intenta nuevamente.');
          this.loading.set(false);
        }
      });
    } else {
      const deliveryRequest: DeliveryRequest = {
        userId: parseInt(formValue.userId),
        items: items,
        deliveryAddress: formValue.deliveryAddress,
        deliveryPhone: parseInt(formValue.deliveryPhone),
        paymentMethod: PaymentMethod.CASH // Los empleados siempre registran pagos en efectivo
      };

      this.orderService.createDeliveryAsEmployee(deliveryRequest).subscribe({
        next: () => {
          this.successMessage.set('¡Pedido a domicilio creado exitosamente!');
          this.resetForm();
          this.loadOrders();
          this.loadProducts();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error creating delivery:', error);
          this.errorMessage.set(error.error?.message || 'Error al crear el pedido a domicilio. Por favor, intenta nuevamente.');
          this.loading.set(false);
        }
      });
    }
  }

  resetForm(): void {
    this.orderForm.reset({
      orderType: OrderType.EN_MESA
    });
    this.cart.set([]);
    this.selectedCategory.set('all');
    this.loading.set(false);
  }

  hideFormAfterDelay(): void {
    setTimeout(() => {
      this.successMessage.set(null);
      this.showForm.set(false);
    }, 3000);
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  getErrorMessage(field: string): string {
    const control = this.orderForm.get(field);
    if (control?.hasError('required')) {
      return 'Este campo es obligatorio';
    }
    if (control?.hasError('min')) {
      return `El valor mínimo es ${control.errors?.['min'].min}`;
    }
    return '';
  }

  getUserName(userId: number): string {
    const user = this.users().find(u => u.id === userId);
    return user ? `${user.name} ${user.lastName}` : '';
  }

  loadOrders(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    // Verificar token y rol antes de hacer la petición
    const token = this.authService.getToken();
    const currentUser = this.authService.currentUser();
    
    console.log('[OrderManagement] Loading orders...');
    console.log('[OrderManagement] Current user:', currentUser);
    console.log('[OrderManagement] Token available:', !!token);
    console.log('[OrderManagement] Token (first 50 chars):', token ? token.substring(0, 50) + '...' : 'null');
    console.log('[OrderManagement] User role:', currentUser?.role);
    
    if (!token) {
      this.errorMessage.set('No hay token de autenticación. Por favor, inicia sesión nuevamente.');
      this.loading.set(false);
      return;
    }
    
    if (currentUser?.role !== 'EMPLOYEE') {
      this.errorMessage.set(`No tienes permisos para ver los pedidos. Tu rol actual es: ${currentUser?.role || 'N/A'}. Por favor, inicia sesión con una cuenta de empleado.`);
      this.loading.set(false);
      return;
    }

    const typeFilter: OrderType | 'all' | undefined = this.filterType();
    this.orderService.getAllUnifiedOrdersForEmployee(typeFilter).subscribe({
      next: (orders) => {
        console.log('Orders loaded:', orders);
        this.orders.set(orders || []);
        this.applyFilter();
        this.loading.set(false);
        if (orders && orders.length === 0) {
          this.errorMessage.set(null); // No error, just no data
        }
      },
      error: (error) => {
        console.error('Error loading orders:', error);
        console.error('Error status:', error.status);
        console.error('Error error:', error.error);
        
        let errorMsg = 'Error al cargar los pedidos. Verifica tu conexión o permisos.';
        
        if (error.status === 403) {
          errorMsg = 'No tienes permisos para ver los pedidos. Por favor, inicia sesión nuevamente con una cuenta de empleado.';
          // Si es un error 403, probablemente el token expiró o no tiene el rol correcto
          // Podríamos redirigir al login o intentar refrescar el token
        } else if (error.status === 401) {
          errorMsg = 'Tu sesión ha expirado. Por favor, inicia sesión nuevamente.';
        } else if (error.status === 0) {
          errorMsg = 'Error de conexión. Verifica que el servidor esté corriendo.';
        } else if (error.error?.message) {
          errorMsg = `Error al cargar los pedidos: ${error.error.message}`;
        } else if (error.message) {
          errorMsg = `Error al cargar los pedidos: ${error.message}`;
        }
        
        this.errorMessage.set(errorMsg);
        this.orders.set([]);
        this.filteredOrders.set([]);
        this.loading.set(false);
      }
    });
  }

  applyFilter(): void {
    const status = this.filterStatus();
    let filtered = this.orders();

    if (status === 'pending') {
      filtered = filtered.filter(o => !o.status);
    } else if (status === 'completed') {
      filtered = filtered.filter(o => o.status);
    }

    this.filteredOrders.set(filtered);
  }

  setFilter(status: 'all' | 'pending' | 'completed'): void {
    this.filterStatus.set(status);
    this.applyFilter();
  }

  setTypeFilter(type: OrderType | 'all'): void {
    this.filterType.set(type);
    this.loadOrders();
  }

  updateOrderStatus(order: UnifiedOrder, status: boolean): void {
    if (!confirm(`¿Estás seguro de ${status ? 'completar' : 'marcar como pendiente'} este pedido?`)) {
      return;
    }

    this.orderService.updateUnifiedOrderStatus(order.id, order.type, status).subscribe({
      next: () => {
        this.loadOrders();
      },
      error: (error) => {
        console.error('Error updating order status:', error);
        this.errorMessage.set('Error al actualizar el estado del pedido');
        setTimeout(() => this.errorMessage.set(null), 3000);
      }
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  formatTime(time: string): string {
    const [hours, minutes] = time.split(':');
    return `${hours}:${minutes}`;
  }

  getOrderTypeLabel(orderType: OrderType): string {
    return orderType === OrderType.EN_MESA ? 'En Mesa' : 'Domicilio';
  }

  getTypeFilterLabel(type: OrderType | 'all'): string {
    if (type === 'all') return 'Todos';
    return type === OrderType.EN_MESA ? 'En Mesa' : 'Domicilio';
  }
}

