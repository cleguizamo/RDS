import { Component, OnInit, OnDestroy, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { ExpenseService } from '../../../core/services/expense.service';
import { ExpenseResponse, ExpenseRequest, ExpenseSearchRequest, EXPENSE_CATEGORIES, PAYMENT_METHODS } from '../../../core/models/expense.model';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency.pipe';
import { PageResponse } from '../../../core/models/page.model';

@Component({
  selector: 'app-expense-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, FormatCurrencyPipe],
  templateUrl: './expense-management.component.html',
  styleUrl: './expense-management.component.css'
})
export class ExpenseManagementComponent implements OnInit, OnDestroy {
  expensesPage = signal<PageResponse<ExpenseResponse> | null>(null);
  expenses = signal<ExpenseResponse[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  editingExpense = signal<ExpenseResponse | null>(null);
  
  // Paginación
  currentPage = signal<number>(0);
  pageSize = signal<number>(20);
  totalPages = signal<number>(0);
  totalElements = signal<number>(0);
  
  // Búsqueda avanzada
  showAdvancedSearch = signal<boolean>(false);
  searchRequest = signal<ExpenseSearchRequest>({});
  sortBy = signal<string>('expenseDate');
  sortDirection = signal<string>('DESC');
  
  // Filtros simples
  searchTerm = signal<string>('');
  selectedCategory = signal<string>('');
  startDate = signal<string>('');
  endDate = signal<string>('');

  // Debounce para búsqueda
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  categories = EXPENSE_CATEGORIES;
  paymentMethods = PAYMENT_METHODS;

  expenseForm: FormGroup;

  constructor(
    private expenseService: ExpenseService,
    private fb: FormBuilder
  ) {
    this.expenseForm = this.fb.group({
      description: ['', [Validators.required, Validators.minLength(3)]],
      category: ['', [Validators.required]],
      amount: [0, [Validators.required, Validators.min(0.01)]],
      expenseDate: [new Date().toISOString().split('T')[0], [Validators.required]],
      paymentMethod: [''],
      notes: [''],
      receiptUrl: ['']
    });
  }

  ngOnInit(): void {
    this.loadExpenses();
    this.setupSearchDebounce();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private setupSearchDebounce(): void {
    // Debounce la búsqueda por 400ms después de que el usuario deje de escribir
    this.searchSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(term => {
      this.searchTerm.set(term);
      this.currentPage.set(0);
      this.loadExpenses();
    });
  }

  loadExpenses(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const searchReq = this.buildSearchRequest();
    
    this.expenseService.searchExpenses(
      searchReq,
      this.currentPage(),
      this.pageSize(),
      this.sortBy(),
      this.sortDirection()
    ).subscribe({
      next: (page) => {
        this.expensesPage.set(page);
        this.expenses.set(page.content);
        this.totalPages.set(page.totalPages);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading expenses:', error);
        this.errorMessage.set('Error al cargar los gastos. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  buildSearchRequest(): ExpenseSearchRequest {
    const request: ExpenseSearchRequest = {};
    
    if (this.searchTerm()) {
      request.description = this.searchTerm();
    }
    
    if (this.selectedCategory()) {
      request.category = this.selectedCategory();
    }
    
    if (this.startDate()) {
      request.startDate = this.startDate();
    }
    
    if (this.endDate()) {
      request.endDate = this.endDate();
    }
    
    // Copiar filtros avanzados
    const advanced = this.searchRequest();
    if (advanced.paymentMethod) request.paymentMethod = advanced.paymentMethod;
    if (advanced.minAmount !== undefined) request.minAmount = advanced.minAmount;
    if (advanced.maxAmount !== undefined) request.maxAmount = advanced.maxAmount;
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
    // Usar debounce para búsqueda más dinámica
    this.searchSubject.next(term);
  }

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.currentPage.set(0);
    this.loadExpenses();
  }

  onDateRangeChange(): void {
    this.currentPage.set(0);
    this.loadExpenses();
  }

  onAdvancedSearchChange(): void {
    // Ya no se busca automáticamente, solo cuando se hace clic en "Aplicar"
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.selectedCategory.set('');
    this.startDate.set('');
    this.endDate.set('');
    this.searchRequest.set({});
    this.currentPage.set(0);
    this.loadExpenses();
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
    this.loadExpenses();
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
      this.loadExpenses();
    }
  }

  previousPage(): void {
    if (this.currentPage() > 0) {
      this.currentPage.set(this.currentPage() - 1);
      this.loadExpenses();
    }
  }

  nextPage(): void {
    if (this.currentPage() < this.totalPages() - 1) {
      this.currentPage.set(this.currentPage() + 1);
      this.loadExpenses();
    }
  }

  onPageSizeChange(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.loadExpenses();
  }

  // Exportación
  exportToExcel(): void {
    this.loading.set(true);
    const searchReq = this.buildSearchRequest();
    
    this.expenseService.exportExpensesToExcel(searchReq).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `gastos_${new Date().toISOString().split('T')[0]}.xlsx`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.successMessage.set('Gastos exportados exitosamente');
        this.loading.set(false);
        setTimeout(() => this.successMessage.set(null), 3000);
      },
      error: (error) => {
        console.error('Error exporting expenses:', error);
        this.errorMessage.set('Error al exportar los gastos');
        this.loading.set(false);
      }
    });
  }

  toggleForm(expense?: ExpenseResponse): void {
    if (expense) {
      this.editingExpense.set(expense);
      this.expenseForm.patchValue({
        description: expense.description,
        category: expense.category,
        amount: expense.amount,
        expenseDate: expense.expenseDate.split('T')[0],
        paymentMethod: expense.paymentMethod || '',
        notes: expense.notes || '',
        receiptUrl: expense.receiptUrl || ''
      });
    } else {
      this.editingExpense.set(null);
      this.expenseForm.reset({
        expenseDate: new Date().toISOString().split('T')[0],
        amount: 0
      });
    }
    this.showForm.set(!this.showForm());
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  onSubmit(): void {
    if (this.expenseForm.invalid) {
      this.markFormGroupTouched(this.expenseForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const expenseData: ExpenseRequest = this.expenseForm.value;

    if (this.editingExpense()) {
      this.expenseService.updateExpense(this.editingExpense()!.id, expenseData).subscribe({
        next: (updatedExpense) => {
          this.successMessage.set('Gasto actualizado exitosamente');
          this.loadExpenses();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error updating expense:', error);
          this.errorMessage.set(error.error?.message || 'Error al actualizar el gasto');
          this.loading.set(false);
        }
      });
    } else {
      this.expenseService.createExpense(expenseData).subscribe({
        next: (newExpense) => {
          this.successMessage.set('Gasto registrado exitosamente');
          this.loadExpenses();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error creating expense:', error);
          this.errorMessage.set(error.error?.message || 'Error al registrar el gasto');
          this.loading.set(false);
        }
      });
    }
  }

  deleteExpense(id: number): void {
    if (!confirm('¿Estás seguro de eliminar este gasto?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.expenseService.deleteExpense(id).subscribe({
      next: () => {
        this.successMessage.set('Gasto eliminado exitosamente');
        this.loadExpenses();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error deleting expense:', error);
        this.errorMessage.set(error.error?.message || 'Error al eliminar el gasto');
        this.loading.set(false);
      }
    });
  }

  hideFormAfterDelay(): void {
    setTimeout(() => {
      this.successMessage.set(null);
      this.showForm.set(false);
      this.editingExpense.set(null);
      this.expenseForm.reset({
        expenseDate: new Date().toISOString().split('T')[0],
        amount: 0
      });
      this.loading.set(false);
    }, 2000);
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  getErrorMessage(field: string): string {
    const control = this.expenseForm.get(field);
    if (control?.hasError('required')) {
      return 'Este campo es obligatorio';
    }
    if (control?.hasError('minlength')) {
      return `Mínimo ${control.errors?.['minlength'].requiredLength} caracteres`;
    }
    if (control?.hasError('min')) {
      return `El valor mínimo es ${control.errors?.['min'].min}`;
    }
    return '';
  }

  getTotalExpenses(): number {
    return this.expenses().reduce((sum, expense) => sum + expense.amount, 0);
  }

  // Métodos auxiliares para actualizar searchRequest
  updateSearchField(field: keyof ExpenseSearchRequest, value: any): void {
    let processedValue: any = value;
    
    // Procesar valores numéricos
    if (field === 'minAmount' || field === 'maxAmount') {
      processedValue = value && value !== '' ? parseFloat(value) : undefined;
    }
    
    this.searchRequest.update(r => ({ ...r, [field]: processedValue }));
  }
}
