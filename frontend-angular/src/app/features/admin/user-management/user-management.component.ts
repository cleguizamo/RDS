import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { UserResponse, UserSearchRequest } from '../../../core/models/user.model';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency.pipe';
import { PageResponse } from '../../../core/models/page.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule, FormatCurrencyPipe],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit {
  usersPage = signal<PageResponse<UserResponse> | null>(null);
  users = signal<UserResponse[]>([]);
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
  searchRequest = signal<UserSearchRequest>({});
  sortBy = signal<string>('id');
  sortDirection = signal<string>('ASC');

  // Filtros simples
  searchTerm = signal<string>('');

  constructor(private userService: AdminUserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    
    const searchReq = this.buildSearchRequest();
    
    this.userService.searchUsers(
      searchReq,
      this.currentPage(),
      this.pageSize(),
      this.sortBy(),
      this.sortDirection()
    ).subscribe({
      next: (page) => {
        this.usersPage.set(page);
        this.users.set(page.content);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar usuarios. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  buildSearchRequest(): UserSearchRequest {
    const request: UserSearchRequest = {};
    
    if (this.searchTerm()) {
      request.name = this.searchTerm();
    }
    
    // Copiar filtros avanzados
    const advanced = this.searchRequest();
    if (advanced.email) request.email = advanced.email;
    if (advanced.documentNumber) request.documentNumber = advanced.documentNumber;
    if (advanced.phone) request.phone = advanced.phone;
    if (advanced.minPoints !== undefined) request.minPoints = advanced.minPoints;
    if (advanced.maxPoints !== undefined) request.maxPoints = advanced.maxPoints;
    if (advanced.minOrders !== undefined) request.minOrders = advanced.minOrders;
    if (advanced.maxOrders !== undefined) request.maxOrders = advanced.maxOrders;
    if (advanced.minSpent !== undefined) request.minSpent = advanced.minSpent;
    if (advanced.maxSpent !== undefined) request.maxSpent = advanced.maxSpent;
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
    this.loadUsers();
  }

  onAdvancedSearchChange(): void {
    // Ya no se busca automáticamente, solo cuando se hace clic en "Aplicar"
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.searchRequest.set({});
    this.currentPage.set(0);
    this.loadUsers();
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
    this.loadUsers();
    this.closeAdvancedSearch();
  }

  // Paginación
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages()) {
      this.currentPage.set(page);
      this.loadUsers();
    }
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadUsers();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadUsers();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadUsers();
  }


  deleteUser(id: number): void {
    if (!confirm('¿Estás seguro de eliminar este usuario?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.userService.deleteUser(id).subscribe({
      next: () => {
        this.successMessage.set('Usuario eliminado exitosamente');
        this.loadUsers();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al eliminar usuario');
        this.loading.set(false);
      }
    });
  }


  // Métodos auxiliares para actualizar searchRequest
  updateSearchField(field: keyof UserSearchRequest, value: any): void {
    this.searchRequest.update(r => ({ ...r, [field]: value }));
    this.onAdvancedSearchChange();
  }
}
