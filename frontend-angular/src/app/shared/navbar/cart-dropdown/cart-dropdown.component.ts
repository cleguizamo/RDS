import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CartService } from '../../../core/services/cart.service';
import { CartItem } from '../../../core/models/cart.model';

@Component({
  selector: 'app-cart-dropdown',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './cart-dropdown.component.html',
  styleUrl: './cart-dropdown.component.css'
})
export class CartDropdownComponent {
  items = computed(() => this.cartService.items$());
  totalItems = computed(() => this.cartService.totalItems$());
  totalPrice = computed(() => this.cartService.totalPrice$());
  isOpen = false;

  constructor(public cartService: CartService) {}

  toggle(): void {
    this.isOpen = !this.isOpen;
  }

  close(): void {
    this.isOpen = false;
  }

  removeItem(productId: number): void {
    this.cartService.removeItem(productId);
  }

  updateQuantity(productId: number, quantity: number): void {
    this.cartService.updateQuantity(productId, quantity);
  }

  decreaseQuantity(item: CartItem): void {
    if (item.quantity > 1) {
      this.updateQuantity(item.product.id!, item.quantity - 1);
    } else {
      this.removeItem(item.product.id!);
    }
  }

  increaseQuantity(item: CartItem): void {
    this.updateQuantity(item.product.id!, item.quantity + 1);
  }
}

