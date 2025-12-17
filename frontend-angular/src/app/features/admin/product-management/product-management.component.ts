import { Component, OnInit, signal, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { CategoryService } from '../../../core/services/category.service';
import { ProductResponse, ProductSearchRequest } from '../../../core/models/product.model';
import { CategoryResponse } from '../../../core/models/category.model';
import { PageResponse } from '../../../core/models/page.model';

@Component({
  selector: 'app-product-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-management.component.html',
  styleUrl: './product-management.component.css'
})
export class ProductManagementComponent implements OnInit {
  productsPage = signal<PageResponse<ProductResponse> | null>(null);
  products = signal<ProductResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  
  // Paginación
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  
  // Búsqueda avanzada
  showAdvancedSearch = signal<boolean>(false);
  searchRequest = signal<ProductSearchRequest>({});
  sortBy = signal<string>('id');
  sortDirection = signal<string>('ASC');
  
  // Filtros simples (para compatibilidad)
  searchTerm = signal<string>('');
  selectedCategory = signal<string>('');

  constructor(
    private productService: ProductService,
    private categoryService: CategoryService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error) => console.error('Error loading categories:', error)
    });
  }

  loadProducts(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    
    const searchReq = this.buildSearchRequest();
    
    this.productService.searchProducts(
      searchReq,
      this.currentPage(),
      this.pageSize(),
      this.sortBy(),
      this.sortDirection()
    ).subscribe({
      next: (page) => {
        this.productsPage.set(page);
        this.products.set(page.content);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading products:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar productos. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  buildSearchRequest(): ProductSearchRequest {
    const request: ProductSearchRequest = {};
    
    if (this.searchTerm()) {
      request.name = this.searchTerm();
    }
    
    if (this.selectedCategory()) {
      const categoryId = parseInt(this.selectedCategory());
      if (!isNaN(categoryId)) {
        request.categoryId = categoryId;
      }
    }
    
    // Copiar filtros avanzados si están activos
    const advanced = this.searchRequest();
    if (advanced.minPrice !== undefined) request.minPrice = advanced.minPrice;
    if (advanced.maxPrice !== undefined) request.maxPrice = advanced.maxPrice;
    if (advanced.minStock !== undefined) request.minStock = advanced.minStock;
    if (advanced.subCategoryId !== undefined) request.subCategoryId = advanced.subCategoryId;
    if (advanced.sortBy) {
      this.sortBy.set(advanced.sortBy);
      request.sortBy = advanced.sortBy;
    }
    if (advanced.sortDirection) {
      this.sortDirection.set(advanced.sortDirection);
      request.sortDirection = advanced.sortDirection;
    }
    
    return request;
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.currentPage.set(0);
    this.loadProducts();
  }

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.currentPage.set(0);
    this.loadProducts();
  }

  onAdvancedSearchChange(): void {
    // Ya no se busca automáticamente, solo cuando se hace clic en "Aplicar"
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set('');
    this.searchRequest.set({});
    this.currentPage.set(0);
    this.loadProducts();
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
    this.currentPage.set(0);
    this.loadProducts();
    this.closeAdvancedSearch();
  }

  hasActiveFilters(): boolean {
    const search = this.searchRequest();
    return Object.keys(search).length > 0;
  }

  // Paginación
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadProducts();
    }
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadProducts();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadProducts();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadProducts();
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

  @Output() editProduct = new EventEmitter<ProductResponse>();

  editProductHandler(product: ProductResponse): void {
    this.editProduct.emit(product);
  }

  // Métodos auxiliares para actualizar searchRequest
  updateSearchField(field: keyof ProductSearchRequest, value: any): void {
    this.searchRequest.update(r => ({ ...r, [field]: value }));
  }
}
