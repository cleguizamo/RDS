import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ProductService } from '../../core/services/product.service';
import { ProductResponse } from '../../core/models/product.model';
import { AuthService } from '../../core/services/auth.service';
import { CartService } from '../../core/services/cart.service';

@Component({
  selector: 'app-menu',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './menu.component.html',
  styleUrl: './menu.component.css'
})
export class MenuComponent implements OnInit {
  products = signal<ProductResponse[]>([]);
  filteredProducts = signal<ProductResponse[]>([]);
  categories = signal<string[]>([]);
  selectedCategory = signal<string>('all');
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);

  constructor(
    private productService: ProductService,
    public authService: AuthService,
    private cartService: CartService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    
    this.productService.getAllPublicProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.filteredProducts.set(products);
        
        // Obtener categorías únicas
        const uniqueCategories = [...new Set(products.map(p => p.categoryName))];
        this.categories.set(uniqueCategories);
        
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.errorMessage.set('Error al cargar el menú. Por favor, intenta nuevamente.');
        this.loading.set(false);
      }
    });
  }

  filterByCategory(category: string): void {
    this.selectedCategory.set(category);
    if (category === 'all') {
      this.filteredProducts.set(this.products());
    } else {
      const filtered = this.products().filter(p => p.categoryName === category);
      this.filteredProducts.set(filtered);
    }
  }

  getProductsByCategory(category: string): ProductResponse[] {
    return this.filteredProducts().filter(p => p.categoryName === category);
  }

  addToCart(product: ProductResponse): void {
    this.cartService.addItem(product, 1);
  }
}

