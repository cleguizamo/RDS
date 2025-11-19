import { Component, OnInit, OnChanges, SimpleChanges, signal, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductService } from '../../../core/services/product.service';
import { ProductRequest, ProductResponse } from '../../../core/models/product.model';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './product-form.component.html',
  styleUrl: './product-form.component.css'
})
export class ProductFormComponent implements OnInit, OnChanges {
  @Input() product: ProductResponse | null = null;
  @Output() save = new EventEmitter<ProductResponse>();
  @Output() cancel = new EventEmitter<void>();

  formData = signal<ProductRequest>({
    name: '',
    description: '',
    imageUrl: '',
    price: 0,
    category: '',
    stock: 0
  });

  selectedFile = signal<File | null>(null);
  imagePreview = signal<string | null>(null);
  uploading = signal<boolean>(false);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  categories = ['Bebidas', 'Comida', 'Postres', 'Entradas', 'Platos Principales'];

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProductData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['product'] && !changes['product'].firstChange) {
      this.loadProductData();
    }
  }

  private loadProductData(): void {
    if (this.product) {
      this.formData.set({
        name: this.product.name,
        description: this.product.description,
        imageUrl: this.product.imageUrl,
        price: this.product.price,
        category: this.product.category,
        stock: this.product.stock
      });
      this.imagePreview.set(this.product.imageUrl);
    } else {
      this.resetForm();
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.selectedFile.set(file);
      this.errorMessage.set(null);
      this.successMessage.set(null);

      // Validar tipo de archivo
      if (!file.type.startsWith('image/')) {
        this.errorMessage.set('El archivo debe ser una imagen');
        this.selectedFile.set(null);
        input.value = '';
        return;
      }

      // Validar tamaño (max 10MB)
      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        this.errorMessage.set('El archivo no puede ser mayor a 10MB');
        this.selectedFile.set(null);
        input.value = '';
        return;
      }

      // Crear preview
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

    try {
      const imageUrl = await this.uploadImage();
      // Limpiar el archivo seleccionado después de subir exitosamente
      this.selectedFile.set(null);
      // El formData y preview ya están actualizados en uploadImage()
    } catch (error) {
      // El error ya se estableció en uploadImage()
      console.error('Error al subir imagen:', error);
    }
  }

  uploadImage(): Promise<string> {
    const file = this.selectedFile();
    if (!file) {
      return Promise.resolve(this.formData().imageUrl || '');
    }

    // Validar tipo de archivo
    if (!file.type.startsWith('image/')) {
      const errorMsg = 'El archivo debe ser una imagen';
      this.errorMessage.set(errorMsg);
      return Promise.reject(new Error(errorMsg));
    }

    // Validar tamaño (max 10MB)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      const errorMsg = 'El archivo no puede ser mayor a 10MB';
      this.errorMessage.set(errorMsg);
      return Promise.reject(new Error(errorMsg));
    }

    this.uploading.set(true);
    this.errorMessage.set(null);
    
    return new Promise((resolve, reject) => {
      this.productService.uploadImage(file).subscribe({
        next: (response) => {
          this.uploading.set(false);
          console.log('Respuesta de subida de imagen:', response);
          
          if (response && response.imageUrl) {
            // Actualizar el formData con la nueva URL
            const newFormData = {
              ...this.formData(),
              imageUrl: response.imageUrl
            };
            this.formData.set(newFormData);
            // Actualizar el preview con la URL de Cloudinary
            this.imagePreview.set(response.imageUrl);
            this.successMessage.set('Imagen subida exitosamente');
            // Limpiar mensaje de éxito después de 3 segundos
            setTimeout(() => {
              if (this.successMessage() === 'Imagen subida exitosamente') {
                this.successMessage.set(null);
              }
            }, 3000);
            resolve(response.imageUrl);
          } else {
            const errorMsg = 'No se recibió la URL de la imagen en la respuesta del servidor';
            console.error('Respuesta inválida:', response);
            this.errorMessage.set(errorMsg);
            reject(new Error(errorMsg));
          }
        },
        error: (error) => {
          this.uploading.set(false);
          console.error('Error completo al subir imagen:', error);
          console.error('Error status:', error.status);
          console.error('Error error:', error.error);
          
          // Manejar diferentes formatos de error
          let errorMsg = 'Error al subir la imagen. Verifica tu conexión e intenta nuevamente.';
          
          if (error.error) {
            if (typeof error.error === 'string') {
              errorMsg = error.error;
            } else if (error.error.message) {
              errorMsg = error.error.message;
            } else if (error.error.error) {
              errorMsg = error.error.error;
            }
          } else if (error.message) {
            errorMsg = error.message;
          }
          
          // Si es un error de autenticación
          if (error.status === 401 || error.status === 403) {
            errorMsg = 'No tienes permiso para subir imágenes. Por favor, inicia sesión nuevamente.';
          } else if (error.status === 0) {
            errorMsg = 'Error de conexión. Verifica que el servidor esté corriendo.';
          }
          
          this.errorMessage.set(errorMsg);
          reject(new Error(errorMsg));
        }
      });
    });
  }

  async onSubmit(): Promise<void> {
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.loading.set(true);

    try {
      // Si hay archivo seleccionado, subirlo primero
      let imageUrl = this.formData().imageUrl;
      if (this.selectedFile()) {
        try {
          console.log('Subiendo imagen antes de guardar producto...');
          imageUrl = await this.uploadImage();
          console.log('Imagen subida exitosamente. URL:', imageUrl);
          // El formData ya se actualizó en uploadImage, pero aseguramos que imageUrl esté actualizado
          this.formData.set({
            ...this.formData(),
            imageUrl: imageUrl
          });
        } catch (error: any) {
          console.error('Error al subir imagen en onSubmit:', error);
          // El error ya se estableció en uploadImage, solo mantenemos el loading en false
          this.loading.set(false);
          return;
        }
      }

      // Validar que tengamos una URL de imagen
      if (!imageUrl || imageUrl.trim() === '') {
        this.errorMessage.set('Debes subir una imagen o proporcionar una URL válida');
        this.loading.set(false);
        return;
      }

      const productData: ProductRequest = {
        ...this.formData(),
        imageUrl: imageUrl
      };
      
      console.log('Guardando producto con datos:', { ...productData, imageUrl: imageUrl.substring(0, 50) + '...' });

      if (this.product) {
        // Actualizar producto existente
        this.productService.updateProduct(this.product.id, productData).subscribe({
          next: (updatedProduct) => {
            this.successMessage.set('Producto actualizado exitosamente');
            this.loading.set(false);
            this.save.emit(updatedProduct);
          },
          error: (error) => {
            this.errorMessage.set(error.error || 'Error al actualizar producto');
            this.loading.set(false);
          }
        });
      } else {
        // Crear nuevo producto
        if (!imageUrl) {
          this.errorMessage.set('Debes subir una imagen o proporcionar una URL');
          this.loading.set(false);
          return;
        }

        this.productService.createProduct(productData).subscribe({
          next: (newProduct) => {
            this.successMessage.set('Producto creado exitosamente');
            this.loading.set(false);
            this.resetForm();
            this.save.emit(newProduct);
          },
          error: (error) => {
            this.errorMessage.set(error.error || 'Error al crear producto');
            this.loading.set(false);
          }
        });
      }
    } catch (error) {
      this.errorMessage.set('Error al procesar el formulario');
      this.loading.set(false);
    }
  }

  resetForm(): void {
    this.formData.set({
      name: '',
      description: '',
      imageUrl: '',
      price: 0,
      category: '',
      stock: 0
    });
    this.selectedFile.set(null);
    this.imagePreview.set(null);
  }

  onCancel(): void {
    this.resetForm();
    this.cancel.emit();
  }
}

