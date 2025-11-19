import { Component, OnInit, signal, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductService } from '../../../core/services/product.service';
import { ProductResponse } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './product-management.component.html',
  styleUrl: './product-management.component.css'
})
export class ProductManagementComponent implements OnInit {
  products = signal<ProductResponse[]>([]);
  filteredProducts = signal<ProductResponse[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  searchTerm = signal<string>('');
  selectedCategory = signal<string>('');

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.productService.getAllProducts().subscribe({
      next: (products) => {
        this.products.set(products);
        this.filteredProducts.set(products);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar productos. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.applyFilters();
  }

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.applyFilters();
  }

  applyFilters(): void {
    let filtered = [...this.products()];

    if (this.searchTerm()) {
      const term = this.searchTerm().toLowerCase();
      filtered = filtered.filter(p => 
        p.name.toLowerCase().includes(term) ||
        p.description.toLowerCase().includes(term)
      );
    }

    if (this.selectedCategory()) {
      filtered = filtered.filter(p => p.category === this.selectedCategory());
    }

    this.filteredProducts.set(filtered);
  }

  deleteProduct(id: number): void {
    if (!confirm('¿Estás seguro de eliminar este producto?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.productService.deleteProduct(id).subscribe({
      next: () => {
        this.successMessage.set('Producto eliminado exitosamente');
        this.loadProducts();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al eliminar producto');
        this.loading.set(false);
      }
    });
  }

  getCategories(): string[] {
    const categories = new Set(this.products().map(p => p.category));
    return Array.from(categories);
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set('');
    this.filteredProducts.set(this.products());
  }

  @Output() editProduct = new EventEmitter<ProductResponse>();

  editProductHandler(product: ProductResponse): void {
    this.editProduct.emit(product);
  }
}

