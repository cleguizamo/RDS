import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { ExpenseService } from '../../../core/services/expense.service';
import { ExpenseResponse, ExpenseRequest, EXPENSE_CATEGORIES, PAYMENT_METHODS } from '../../../core/models/expense.model';
import { FormatCurrencyPipe } from '../../../shared/pipes/format-currency.pipe';

@Component({
  selector: 'app-expense-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, FormatCurrencyPipe],
  templateUrl: './expense-management.component.html',
  styleUrl: './expense-management.component.css'
})
export class ExpenseManagementComponent implements OnInit {
  expenses = signal<ExpenseResponse[]>([]);
  filteredExpenses = signal<ExpenseResponse[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  editingExpense = signal<ExpenseResponse | null>(null);
  searchTerm = signal<string>('');
  selectedCategory = signal<string>('');
  startDate = signal<string>('');
  endDate = signal<string>('');

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
  }

  loadExpenses(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    const startDate = this.startDate() || undefined;
    const endDate = this.endDate() || undefined;

    this.expenseService.getAllExpenses(startDate, endDate).subscribe({
      next: (expenses) => {
        this.expenses.set(expenses);
        this.applyFilters();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading expenses:', error);
        this.errorMessage.set('Error al cargar los gastos. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.expenses()];
    const term = this.searchTerm().toLowerCase();
    const category = this.selectedCategory();

    if (term) {
      filtered = filtered.filter(e =>
        e.description.toLowerCase().includes(term) ||
        (e.notes && e.notes.toLowerCase().includes(term))
      );
    }

    if (category) {
      filtered = filtered.filter(e => e.category === category);
    }

    this.filteredExpenses.set(filtered);
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.applyFilters();
  }

  onCategoryChange(category: string): void {
    this.selectedCategory.set(category);
    this.applyFilters();
  }

  onDateRangeChange(): void {
    this.loadExpenses();
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
    return this.filteredExpenses().reduce((sum, expense) => sum + expense.amount, 0);
  }
}

