import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { CategoryService } from '../../../core/services/category.service';
import { CategoryResponse, CategoryRequest } from '../../../core/models/category.model';

@Component({
  selector: 'app-category-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './category-management.component.html',
  styleUrl: './category-management.component.css'
})
export class CategoryManagementComponent implements OnInit {
  categories = signal<CategoryResponse[]>([]);
  filteredCategories = signal<CategoryResponse[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  editingCategory = signal<CategoryResponse | null>(null);
  searchTerm = signal<string>('');

  categoryForm: FormGroup;

  constructor(
    private categoryService: CategoryService,
    private fb: FormBuilder
  ) {
    this.categoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.categoryService.getAllCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.applyFilters();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading categories:', error);
        this.errorMessage.set('Error al cargar las categorías. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.categories()];
    const term = this.searchTerm().toLowerCase();

    if (term) {
      filtered = filtered.filter(c =>
        c.name.toLowerCase().includes(term) ||
        (c.description && c.description.toLowerCase().includes(term))
      );
    }

    this.filteredCategories.set(filtered);
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.applyFilters();
  }

  toggleForm(category?: CategoryResponse): void {
    if (category) {
      this.editingCategory.set(category);
      this.categoryForm.patchValue({
        name: category.name,
        description: category.description || ''
      });
    } else {
      this.editingCategory.set(null);
      this.categoryForm.reset();
    }
    this.showForm.set(!this.showForm());
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  onSubmit(): void {
    if (this.categoryForm.invalid) {
      this.markFormGroupTouched(this.categoryForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const categoryData: CategoryRequest = this.categoryForm.value;

    if (this.editingCategory()) {
      // Actualizar categoría existente
      this.categoryService.updateCategory(this.editingCategory()!.id, categoryData).subscribe({
        next: (updatedCategory) => {
          this.successMessage.set('Categoría actualizada exitosamente');
          this.loadCategories();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error updating category:', error);
          this.errorMessage.set(error.error?.message || 'Error al actualizar la categoría');
          this.loading.set(false);
        }
      });
    } else {
      // Crear nueva categoría
      this.categoryService.createCategory(categoryData).subscribe({
        next: (newCategory) => {
          this.successMessage.set('Categoría creada exitosamente');
          this.loadCategories();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error creating category:', error);
          this.errorMessage.set(error.error?.message || 'Error al crear la categoría');
          this.loading.set(false);
        }
      });
    }
  }

  deleteCategory(id: number): void {
    const category = this.categories().find(c => c.id === id);
    const productCount = category?.productCount || 0;

    if (productCount > 0) {
      alert(`No se puede eliminar esta categoría porque tiene ${productCount} producto(s) asociado(s). Primero elimine o reasigne los productos de esta categoría.`);
      return;
    }

    if (!confirm('¿Estás seguro de eliminar esta categoría?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.categoryService.deleteCategory(id).subscribe({
      next: () => {
        this.successMessage.set('Categoría eliminada exitosamente');
        this.loadCategories();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error deleting category:', error);
        this.errorMessage.set(error.error?.message || 'Error al eliminar la categoría');
        this.loading.set(false);
      }
    });
  }

  hideFormAfterDelay(): void {
    setTimeout(() => {
      this.successMessage.set(null);
      this.showForm.set(false);
      this.editingCategory.set(null);
      this.categoryForm.reset();
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
    const control = this.categoryForm.get(field);
    if (control?.hasError('required')) {
      return 'Este campo es obligatorio';
    }
    if (control?.hasError('minlength')) {
      return `Mínimo ${control.errors?.['minlength'].requiredLength} caracteres`;
    }
    return '';
  }
}

