import { Component, OnInit, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CartService } from '../../core/services/cart.service';
import { CartItem } from '../../core/models/cart.model';
import { AuthService } from '../../core/services/auth.service';
import { OrderService } from '../../core/services/order.service';
import { NotificationService } from '../../core/services/notification.service';
import { OrderType, PaymentMethod } from '../../core/models/order.model';

@Component({
  selector: 'app-cart',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.css'
})
export class CartComponent implements OnInit {
  // Exponer OrderType al template
  OrderType = OrderType;

  items = computed(() => this.cartService.items$());
  totalItems = computed(() => this.cartService.totalItems$());
  totalPrice = computed(() => this.cartService.totalPrice$());
  
  orderType = signal<OrderType>(OrderType.EN_MESA);
  tableNumber = signal<number | null>(null);
  deliveryAddress = signal<string>('');
  deliveryPhone = signal<string>('');
  paymentMethod = signal<PaymentMethod>(PaymentMethod.CASH);
  paymentProofFile = signal<File | null>(null);
  paymentProofPreview = signal<string | null>(null);
  
  showCheckoutForm = signal(false);
  loading = signal(false);
  
  // Exponer PaymentMethod al template
  PaymentMethod = PaymentMethod;

  constructor(
    public cartService: CartService,
    public authService: AuthService,
    private orderService: OrderService,
    private notificationService: NotificationService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.cartService.isEmpty()) {
      this.notificationService.info('Tu carrito está vacío');
    }
  }

  removeItem(productId: number): void {
    this.cartService.removeItem(productId);
  }

  updateQuantity(productId: number, quantity: number): void {
    // Validar que la cantidad sea un número válido
    const numQuantity = Math.floor(Number(quantity));
    if (isNaN(numQuantity) || numQuantity < 0) {
      return;
    }
    this.cartService.updateQuantity(productId, numQuantity);
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      this.updateQuantity(item.product.id!, item.quantity - 1);
    } else {
      this.removeItem(item.product.id!);
    }
  }

  increaseQuantity(item: CartItem): void {
    const newQuantity = item.quantity + 1;
    // Verificar stock antes de incrementar
    if (newQuantity <= item.product.stock) {
      this.updateQuantity(item.product.id!, newQuantity);
    } else {
      this.notificationService.warning(`Solo hay ${item.product.stock} unidades disponibles`);
    }
  }

  onQuantityInputChange(item: CartItem, event: Event): void {
    const input = event.target as HTMLInputElement;
    const newQuantity = Math.floor(Number(input.value));
    
    if (isNaN(newQuantity) || newQuantity < 1) {
      // Restaurar el valor anterior si es inválido
      input.value = item.quantity.toString();
      return;
    }
    
    if (newQuantity > item.product.stock) {
      this.notificationService.warning(`Solo hay ${item.product.stock} unidades disponibles`);
      input.value = Math.min(newQuantity, item.product.stock).toString();
      this.updateQuantity(item.product.id!, item.product.stock);
      return;
    }
    
    this.updateQuantity(item.product.id!, newQuantity);
  }

  toggleCheckoutForm(): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.warning('Debes iniciar sesión para realizar un pedido');
      return;
    }
    this.showCheckoutForm.set(!this.showCheckoutForm());
  }

  onOrderTypeChange(type: OrderType): void {
    this.orderType.set(type);
    // Limpiar campos cuando se cambia el tipo
    if (type === OrderType.EN_MESA) {
      this.deliveryAddress.set('');
      this.deliveryPhone.set('');
    } else {
      this.tableNumber.set(null);
    }
  }

  resetForm(): void {
    this.showCheckoutForm.set(false);
    this.orderType.set(OrderType.EN_MESA);
    this.tableNumber.set(null);
    this.deliveryAddress.set('');
    this.deliveryPhone.set('');
    this.paymentMethod.set(PaymentMethod.CASH);
    this.paymentProofFile.set(null);
    this.paymentProofPreview.set(null);
    this.loading.set(false);
  }

  onPaymentMethodChange(method: PaymentMethod): void {
    this.paymentMethod.set(method);
    // Si cambia a efectivo, limpiar el archivo de comprobante
    if (method === PaymentMethod.CASH) {
      this.paymentProofFile.set(null);
      this.paymentProofPreview.set(null);
    }
  }

  onPaymentProofFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      
      // Validar tipo de archivo (solo imágenes)
      if (!file.type.startsWith('image/')) {
        this.notificationService.error('Por favor selecciona un archivo de imagen');
        input.value = '';
        return;
      }
      
      // Validar tamaño (máximo 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.notificationService.error('El archivo es demasiado grande. Máximo 5MB');
        input.value = '';
        return;
      }
      
      this.paymentProofFile.set(file);
      
      // Crear preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.paymentProofPreview.set(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  removePaymentProof(): void {
    this.paymentProofFile.set(null);
    this.paymentProofPreview.set(null);
  }

  placeOrder(): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.error('Debes iniciar sesión para realizar un pedido');
      return;
    }

    const user = this.authService.currentUser();
    if (!user) {
      this.notificationService.error('Error al obtener información del usuario');
      return;
    }

    // Validaciones según el tipo de pedido
    if (this.orderType() === OrderType.EN_MESA) {
      if (!this.tableNumber() || this.tableNumber()! <= 0) {
        this.notificationService.error('Debes ingresar un número de mesa válido');
        return;
      }
    }

    if (this.orderType() === OrderType.DOMICILIO) {
      const address = this.deliveryAddress().trim();
      const phone = this.deliveryPhone().trim();
      
      if (!address) {
        this.notificationService.error('Debes ingresar la dirección de entrega');
        return;
      }
      
      if (!phone) {
        this.notificationService.error('Debes ingresar el teléfono de contacto');
        return;
      }

      // Validar que el teléfono sea numérico y tenga un formato válido
      const phoneNumber = phone.replace(/\D/g, ''); // Remover caracteres no numéricos
      if (phoneNumber.length < 7 || phoneNumber.length > 15) {
        this.notificationService.error('El teléfono debe tener entre 7 y 15 dígitos');
        return;
      }

      // Validar dirección mínima
      if (address.length < 10) {
        this.notificationService.error('Por favor ingresa una dirección más completa');
        return;
      }
    }

    if (this.items().length === 0) {
      this.notificationService.error('Tu carrito está vacío');
      return;
    }

    this.loading.set(true);

    const orderItems = this.items().map(item => ({
      productId: item.product.id!,
      quantity: item.quantity
    }));

    if (this.orderType() === OrderType.EN_MESA) {
      // Para pedidos en mesa, se paga directamente en la tienda (efectivo)
      this.orderService.createOrder({
        userId: user.id!,
        items: orderItems,
        tableNumber: this.tableNumber()!,
        paymentMethod: PaymentMethod.CASH // Siempre efectivo para pedidos en mesa
      }).subscribe({
        next: async (response) => {
          console.log('[Cart] Pedido creado exitosamente:', response);
          
          // Si hay un archivo de comprobante y no es efectivo, subirlo
          const file = this.paymentProofFile();
          if (file && this.paymentMethod() !== PaymentMethod.CASH) {
            try {
              await this.orderService.uploadPaymentProof(response.id, file).toPromise();
              this.notificationService.success('Pedido y comprobante subidos exitosamente');
            } catch (error) {
              console.error('Error al subir comprobante:', error);
              this.notificationService.warning('Pedido creado pero hubo un error al subir el comprobante. Puedes subirlo después.');
            }
          } else {
            this.notificationService.success('Pedido en mesa realizado exitosamente');
          }
          
          this.cartService.clear();
          this.resetForm();
          this.router.navigate(['/dashboard/orders']);
        },
        error: (error) => {
          console.error('Error creating order:', error);
          console.error('Error details:', JSON.stringify(error, null, 2));
          let errorMessage = 'Error al realizar el pedido. Intenta nuevamente.';
          
          if (error.error) {
            if (typeof error.error === 'string') {
              errorMessage = error.error;
            } else if (error.error.message) {
              errorMessage = error.error.message;
            } else if (error.error.error) {
              errorMessage = error.error.error;
            }
          } else if (error.message) {
            errorMessage = error.message;
          }
          
          this.notificationService.error(errorMessage);
          this.loading.set(false);
        }
      });
    } else {
      // Preparar el teléfono (solo números)
      const phoneNumber = parseInt(this.deliveryPhone().replace(/\D/g, ''));
      
      this.orderService.createDelivery({
        userId: user.id!,
        items: orderItems,
        deliveryAddress: this.deliveryAddress().trim(),
        deliveryPhone: phoneNumber,
        paymentMethod: this.paymentMethod()
      }).subscribe({
        next: async (response) => {
          console.log('[Cart] Domicilio creado exitosamente:', response);
          
          // Si hay un archivo de comprobante y no es efectivo, subirlo
          const file = this.paymentProofFile();
          if (file && this.paymentMethod() !== PaymentMethod.CASH) {
            try {
              await this.orderService.uploadDeliveryPaymentProof(response.id, file).toPromise();
              this.notificationService.success('Domicilio y comprobante subidos exitosamente');
            } catch (error) {
              console.error('Error al subir comprobante:', error);
              this.notificationService.warning('Domicilio creado pero hubo un error al subir el comprobante. Puedes subirlo después.');
            }
          } else {
            this.notificationService.success('Domicilio realizado exitosamente. Te contactaremos pronto.');
          }
          
          this.cartService.clear();
          this.resetForm();
          this.router.navigate(['/dashboard/orders']);
        },
        error: (error) => {
          console.error('Error creating delivery:', error);
          const errorMessage = error.error?.message || error.error || 'Error al realizar el domicilio. Intenta nuevamente.';
          this.notificationService.error(errorMessage);
          this.loading.set(false);
        }
      });
    }
  }

  clearCart(): void {
    if (confirm('¿Estás seguro de que quieres vaciar el carrito?')) {
      this.cartService.clear();
    }
  }
}

