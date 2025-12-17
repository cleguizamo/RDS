import { Injectable, signal, computed } from '@angular/core';
import { Cart, CartItem } from '../models/cart.model';
import { ProductResponse } from '../models/product.model';
import { NotificationService } from './notification.service';

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cart = signal<Cart>({
    items: [],
    totalItems: 0,
    totalPrice: 0
  });

  public cart$ = computed(() => this.cart());
  // Recalcular totales dinámicamente desde los items para asegurar precisión en tiempo real
  public totalItems$ = computed(() => {
    const items = this.cart().items;
    return items.reduce((sum, item) => sum + item.quantity, 0);
  });
  public totalPrice$ = computed(() => {
    const items = this.cart().items;
    return items.reduce((sum, item) => {
      // Calcular desde el precio del producto y cantidad actual (siempre actualizado)
      const subtotal = item.product.price * item.quantity;
      return sum + subtotal;
    }, 0);
  });
  public items$ = computed(() => this.cart().items);

  constructor(private notificationService: NotificationService) {
    this.loadCartFromStorage();
  }

  addItem(product: ProductResponse, quantity: number = 1): void {
    if (product.stock === 0) {
      this.notificationService.error('Este producto no está disponible');
      return;
    }

    if (quantity > product.stock) {
      this.notificationService.warning(`Solo hay ${product.stock} unidades disponibles`);
      quantity = product.stock;
    }

    this.cart.update(currentCart => {
      const existingItemIndex = currentCart.items.findIndex(item => item.product.id === product.id);
      
      // Crear nuevo array de items (inmutabilidad)
      const newItems = [...currentCart.items];
      
      if (existingItemIndex >= 0) {
        const existingItem = newItems[existingItemIndex];
        const newQuantity = existingItem.quantity + quantity;
        
        if (newQuantity > product.stock) {
          this.notificationService.warning(`Solo puedes agregar ${product.stock} unidades de este producto`);
          return currentCart;
        }
        
        // Crear nuevo objeto item con cantidad actualizada
        newItems[existingItemIndex] = {
          ...existingItem,
          quantity: newQuantity,
          subtotal: product.price * newQuantity
        };
        
        this.notificationService.success(`Agregado al carrito: ${product.name} x${quantity}`);
      } else {
        // Crear nuevo item
        const newItem: CartItem = {
          product,
          quantity,
          subtotal: product.price * quantity
        };
        newItems.push(newItem);
        this.notificationService.success(`${product.name} agregado al carrito`);
      }

      // Crear nuevo objeto cart con items actualizados y recalcular totales
      return this.calculateTotals({
        ...currentCart,
        items: newItems
      });
    });

    this.saveCartToStorage();
  }

  removeItem(productId: number): void {
    this.cart.update(currentCart => {
      const item = currentCart.items.find(item => item.product.id === productId);
      if (item) {
        this.notificationService.info(`${item.product.name} eliminado del carrito`);
      }
      
      // Crear nuevo array sin el item eliminado (inmutabilidad)
      const newItems = currentCart.items.filter(item => item.product.id !== productId);
      
      // Crear nuevo objeto cart con items actualizados y recalcular totales
      return this.calculateTotals({
        ...currentCart,
        items: newItems
      });
    });

    this.saveCartToStorage();
  }

  updateQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.removeItem(productId);
      return;
    }

    this.cart.update(currentCart => {
      const itemIndex = currentCart.items.findIndex(item => item.product.id === productId);
      
      if (itemIndex < 0) {
        return currentCart;
      }

      const item = currentCart.items[itemIndex];

      // Validar stock
      if (quantity > item.product.stock) {
        this.notificationService.warning(`Solo hay ${item.product.stock} unidades disponibles`);
        quantity = item.product.stock;
      }

      // Crear nuevo array con item actualizado (inmutabilidad)
      const newItems = [...currentCart.items];
      newItems[itemIndex] = {
        ...item,
        quantity: quantity,
        subtotal: item.product.price * quantity
      };
      
      // Crear nuevo objeto cart con items actualizados y recalcular totales
      return this.calculateTotals({
        ...currentCart,
        items: newItems
      });
    });

    this.saveCartToStorage();
  }

  clear(): void {
    this.cart.set({
      items: [],
      totalItems: 0,
      totalPrice: 0
    });
    this.saveCartToStorage();
    this.notificationService.info('Carrito vaciado');
  }

  private calculateTotals(cart: Cart): Cart {
    // Crear nuevos items con subtotales recalculados (inmutabilidad)
    const itemsWithSubtotals = cart.items.map(item => ({
      ...item,
      subtotal: item.product.price * item.quantity
    }));
    
    // Calcular totales
    const totalItems = itemsWithSubtotals.reduce((sum, item) => sum + item.quantity, 0);
    const totalPrice = itemsWithSubtotals.reduce((sum, item) => {
      return sum + item.subtotal;
    }, 0);
    
    // Retornar nuevo objeto cart (inmutabilidad)
    return {
      ...cart,
      items: itemsWithSubtotals,
      totalItems,
      totalPrice
    };
  }

  private saveCartToStorage(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        const cartData = {
          items: this.cart().items.map(item => ({
            productId: item.product.id,
            quantity: item.quantity
          }))
        };
        localStorage.setItem('cart', JSON.stringify(cartData));
      } catch (error) {
        console.error('Error saving cart to storage:', error);
      }
    }
  }

  private loadCartFromStorage(): void {
    if (typeof window !== 'undefined' && window.localStorage) {
      try {
        const cartData = localStorage.getItem('cart');
        if (cartData) {
          // Note: We'll need to reload products from the service
          // For now, just clear the cart if we can't load products
          // This will be improved when integrating with product service
        }
      } catch (error) {
        console.error('Error loading cart from storage:', error);
      }
    }
  }

  getCart(): Cart {
    return this.cart();
  }

  isEmpty(): boolean {
    return this.cart().items.length === 0;
  }
}

