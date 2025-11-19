import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { SubCategoryService } from '../../../core/services/subcategory.service';
import { CategoryService } from '../../../core/services/category.service';
import { SubCategoryResponse, SubCategoryRequest } from '../../../core/models/subcategory.model';
import { CategoryResponse } from '../../../core/models/category.model';

@Component({
  selector: 'app-subcategory-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './subcategory-management.component.html',
  styleUrl: './subcategory-management.component.css'
})
export class SubCategoryManagementComponent implements OnInit {
  subCategories = signal<SubCategoryResponse[]>([]);
  filteredSubCategories = signal<SubCategoryResponse[]>([]);
  categories = signal<CategoryResponse[]>([]);
  loading = signal<boolean>(false);
  loadingCategories = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  editingSubCategory = signal<SubCategoryResponse | null>(null);
  searchTerm = signal<string>('');
  selectedCategoryFilter = signal<number>(0);

  // Exponer parseInt al template
  parseInt = parseInt;

  subCategoryForm: FormGroup;

  constructor(
    private subCategoryService: SubCategoryService,
    private categoryService: CategoryService,
    private fb: FormBuilder
  ) {
    this.subCategoryForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      categoryId: [0, [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadSubCategories();
  }

  loadCategories(): void {
    this.loadingCategories.set(true);
    this.categoryService.getAllCategories().subscribe({
      next: (categories) => {
        this.categories.set(categories);
        this.loadingCategories.set(false);
      },
      error: (error) => {
        console.error('Error loading categories:', error);
        this.errorMessage.set('Error al cargar las categorías.');
        this.loadingCategories.set(false);
      }
    });
  }

  loadSubCategories(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.subCategoryService.getAllSubCategories().subscribe({
      next: (subCategories) => {
        this.subCategories.set(subCategories);
        this.applyFilters();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading subcategories:', error);
        this.errorMessage.set('Error al cargar las subcategorías. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.subCategories()];
    const term = this.searchTerm().toLowerCase();
    const categoryFilter = this.selectedCategoryFilter();

    if (term) {
      filtered = filtered.filter(sc =>
        sc.name.toLowerCase().includes(term) ||
        (sc.description && sc.description.toLowerCase().includes(term)) ||
        (sc.categoryName && sc.categoryName.toLowerCase().includes(term))
      );
    }

    if (categoryFilter > 0) {
      filtered = filtered.filter(sc => sc.categoryId === categoryFilter);
    }

    this.filteredSubCategories.set(filtered);
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.applyFilters();
  }

  onCategoryFilterChange(categoryId: number | string): void {
    const id = typeof categoryId === 'string' ? parseInt(categoryId, 10) : categoryId;
    this.selectedCategoryFilter.set(id || 0);
    this.applyFilters();
  }

  toggleForm(subCategory?: SubCategoryResponse): void {
    if (subCategory) {
      this.editingSubCategory.set(subCategory);
      this.subCategoryForm.patchValue({
        name: subCategory.name,
        description: subCategory.description || '',
        categoryId: subCategory.categoryId
      });
    } else {
      this.editingSubCategory.set(null);
      this.subCategoryForm.reset({
        categoryId: 0
      });
    }
    this.showForm.set(!this.showForm());
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  onSubmit(): void {
    if (this.subCategoryForm.invalid) {
      this.markFormGroupTouched(this.subCategoryForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const subCategoryData: SubCategoryRequest = this.subCategoryForm.value;

    if (this.editingSubCategory()) {
      // Actualizar subcategoría existente
      this.subCategoryService.updateSubCategory(this.editingSubCategory()!.id, subCategoryData).subscribe({
        next: (updatedSubCategory) => {
          this.successMessage.set('Subcategoría actualizada exitosamente');
          this.loadSubCategories();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error updating subcategory:', error);
          this.errorMessage.set(error.error?.message || 'Error al actualizar la subcategoría');
          this.loading.set(false);
        }
      });
    } else {
      // Crear nueva subcategoría
      this.subCategoryService.createSubCategory(subCategoryData).subscribe({
        next: (newSubCategory) => {
          this.successMessage.set('Subcategoría creada exitosamente');
          this.loadSubCategories();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error creating subcategory:', error);
          this.errorMessage.set(error.error?.message || 'Error al crear la subcategoría');
          this.loading.set(false);
        }
      });
    }
  }

  deleteSubCategory(id: number): void {
    const subCategory = this.subCategories().find(sc => sc.id === id);
    const productCount = subCategory?.productCount || 0;

    if (productCount > 0) {
      alert(`No se puede eliminar esta subcategoría porque tiene ${productCount} producto(s) asociado(s). Primero elimine o reasigne los productos de esta subcategoría.`);
      return;
    }

    if (!confirm('¿Estás seguro de eliminar esta subcategoría?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.subCategoryService.deleteSubCategory(id).subscribe({
      next: () => {
        this.successMessage.set('Subcategoría eliminada exitosamente');
        this.loadSubCategories();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error deleting subcategory:', error);
        this.errorMessage.set(error.error?.message || 'Error al eliminar la subcategoría');
        this.loading.set(false);
      }
    });
  }

  hideFormAfterDelay(): void {
    setTimeout(() => {
      this.successMessage.set(null);
      this.showForm.set(false);
      this.editingSubCategory.set(null);
      this.subCategoryForm.reset({
        categoryId: 0
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
    const control = this.subCategoryForm.get(field);
    if (control?.hasError('required')) {
      return 'Este campo es obligatorio';
    }
    if (control?.hasError('minlength')) {
      return `Mínimo ${control.errors?.['minlength'].requiredLength} caracteres`;
    }
    if (control?.hasError('min')) {
      return 'Debes seleccionar una categoría';
    }
    return '';
  }

  getCategoryName(categoryId: number): string {
    const category = this.categories().find(c => c.id === categoryId);
    return category ? category.name : '';
  }
}

