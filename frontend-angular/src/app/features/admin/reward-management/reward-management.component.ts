import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormsModule } from '@angular/forms';
import { RewardService } from '../../../core/services/reward.service';
import { ProductService } from '../../../core/services/product.service';
import { RewardProductResponse, RewardProductRequest } from '../../../core/models/reward.model';

@Component({
  selector: 'app-reward-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './reward-management.component.html',
  styleUrl: './reward-management.component.css'
})
export class RewardManagementComponent implements OnInit {
  rewardProducts = signal<RewardProductResponse[]>([]);
  filteredRewards = signal<RewardProductResponse[]>([]);
  loading = signal<boolean>(false);
  loadingImage = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  editingReward = signal<RewardProductResponse | null>(null);
  searchTerm = signal<string>('');
  selectedFile = signal<File | null>(null);
  imagePreview = signal<string | null>(null);

  rewardForm: FormGroup;

  constructor(
    private rewardService: RewardService,
    private productService: ProductService,
    private fb: FormBuilder
  ) {
    this.rewardForm = this.fb.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      imageUrl: [''],
      pointsRequired: [0, [Validators.required, Validators.min(1)]],
      stock: [0, [Validators.required, Validators.min(0)]],
      isActive: [true, [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.loadRewards();
  }

  loadRewards(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.rewardService.getAllRewardProducts().subscribe({
      next: (rewards) => {
        this.rewardProducts.set(rewards);
        this.applyFilters();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading rewards:', error);
        this.errorMessage.set('Error al cargar las recompensas. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  applyFilters(): void {
    let filtered = [...this.rewardProducts()];
    const term = this.searchTerm().toLowerCase();

    if (term) {
      filtered = filtered.filter(r =>
        r.name.toLowerCase().includes(term) ||
        (r.description && r.description.toLowerCase().includes(term))
      );
    }

    this.filteredRewards.set(filtered);
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
    this.applyFilters();
  }

  toggleForm(reward?: RewardProductResponse): void {
    if (reward) {
      this.editingReward.set(reward);
      this.rewardForm.patchValue({
        name: reward.name,
        description: reward.description || '',
        imageUrl: reward.imageUrl || '',
        pointsRequired: reward.pointsRequired,
        stock: reward.stock,
        isActive: reward.isActive
      });
      this.imagePreview.set(reward.imageUrl || null);
    } else {
      this.editingReward.set(null);
      this.rewardForm.reset({
        pointsRequired: 0,
        stock: 0,
        isActive: true
      });
      this.imagePreview.set(null);
      this.selectedFile.set(null);
    }
    this.showForm.set(!this.showForm());
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.selectedFile.set(file);

      if (!file.type.startsWith('image/')) {
        this.errorMessage.set('El archivo debe ser una imagen');
        this.selectedFile.set(null);
        input.value = '';
        return;
      }

      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.imagePreview.set(e.target.result);
      };
      reader.readAsDataURL(file);
    }
  }

  async uploadImageNow(): Promise<void> {
    if (!this.selectedFile()) {
      this.errorMessage.set('Por favor, selecciona una imagen primero');
      return;
    }

    const file = this.selectedFile();
    if (!file) {
      return;
    }

    // Validar tipo de archivo
    if (!file.type.startsWith('image/')) {
      const errorMsg = 'El archivo debe ser una imagen';
      this.errorMessage.set(errorMsg);
      return;
    }

    // Validar tamaño (max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      const errorMsg = 'El archivo no puede ser mayor a 10MB';
      this.errorMessage.set(errorMsg);
      return;
    }

    this.loadingImage.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    try {
      const response = await this.productService.uploadImage(file).toPromise();
      if (response && response.imageUrl) {
        this.rewardForm.patchValue({ imageUrl: response.imageUrl });
        this.imagePreview.set(response.imageUrl);
        this.selectedFile.set(null);
        
        // Solo mostrar mensaje si no estamos en proceso de envío del formulario
        if (!this.loading()) {
          this.successMessage.set('Imagen subida exitosamente');
          setTimeout(() => {
            if (this.successMessage() === 'Imagen subida exitosamente') {
              this.successMessage.set(null);
            }
          }, 3000);
        }
      } else {
        const errorMsg = 'No se recibió la URL de la imagen en la respuesta del servidor';
        this.errorMessage.set(errorMsg);
        throw new Error(errorMsg);
      }
    } catch (error: any) {
      console.error('Error uploading image:', error);
      let errorMsg = 'Error al subir la imagen. Verifica tu conexión e intenta nuevamente.';
      
      if (error?.error) {
        if (typeof error.error === 'string') {
          errorMsg = error.error;
        } else if (error.error.message) {
          errorMsg = error.error.message;
        }
      } else if (error?.message) {
        errorMsg = error.message;
      }
      
      this.errorMessage.set(errorMsg);
      throw error; // Re-lanzar el error para que onSubmit pueda manejarlo
    } finally {
      this.loadingImage.set(false);
    }
  }

  async onSubmit(): Promise<void> {
    if (this.rewardForm.invalid) {
      this.markFormGroupTouched(this.rewardForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    // Si hay un archivo seleccionado, subirlo primero a Cloudinary
    // Esto asegura que siempre tengamos una URL de Cloudinary antes de guardar
    if (this.selectedFile()) {
      try {
        console.log('Subiendo imagen antes de crear/actualizar recompensa...');
        await this.uploadImageNow();
        
        // Verificar que la URL se actualizó correctamente
        const imageUrl = this.rewardForm.get('imageUrl')?.value;
        if (!imageUrl) {
          const errorMsg = 'No se pudo obtener la URL de la imagen después de subirla. Por favor, intenta nuevamente.';
          this.errorMessage.set(errorMsg);
          this.loading.set(false);
          console.error(errorMsg);
          return;
        }
        
        console.log('Imagen subida exitosamente. URL:', imageUrl);
      } catch (error) {
        console.error('Error al subir la imagen:', error);
        const errorMsg = error instanceof Error ? error.message : 'Error al subir la imagen. Por favor, intenta nuevamente.';
        this.errorMessage.set(errorMsg);
        this.loading.set(false);
        return;
      }
    }

    const rewardData: RewardProductRequest = this.rewardForm.value;

    if (this.editingReward()) {
      // Actualizar recompensa existente
      this.rewardService.updateRewardProduct(this.editingReward()!.id, rewardData).subscribe({
        next: (updatedReward) => {
          this.successMessage.set('Recompensa actualizada exitosamente');
          this.loadRewards();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error updating reward:', error);
          this.errorMessage.set(error.error?.message || 'Error al actualizar la recompensa');
          this.loading.set(false);
        }
      });
    } else {
      // Crear nueva recompensa
      this.rewardService.createRewardProduct(rewardData).subscribe({
        next: (newReward) => {
          this.successMessage.set('Recompensa creada exitosamente');
          this.loadRewards();
          this.hideFormAfterDelay();
        },
        error: (error) => {
          console.error('Error creating reward:', error);
          this.errorMessage.set(error.error?.message || 'Error al crear la recompensa');
          this.loading.set(false);
        }
      });
    }
  }

  deleteReward(id: number): void {
    if (!confirm('¿Estás seguro de eliminar esta recompensa?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.rewardService.deleteRewardProduct(id).subscribe({
      next: () => {
        this.successMessage.set('Recompensa eliminada exitosamente');
        this.loadRewards();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error deleting reward:', error);
        this.errorMessage.set(error.error?.message || 'Error al eliminar la recompensa');
        this.loading.set(false);
      }
    });
  }

  hideFormAfterDelay(): void {
    setTimeout(() => {
      this.successMessage.set(null);
      this.showForm.set(false);
      this.editingReward.set(null);
      this.rewardForm.reset({
        pointsRequired: 0,
        stock: 0,
        isActive: true
      });
      this.imagePreview.set(null);
      this.selectedFile.set(null);
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
    const control = this.rewardForm.get(field);
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
}

