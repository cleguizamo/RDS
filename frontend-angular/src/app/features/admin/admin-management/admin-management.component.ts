import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../../../core/services/admin.service';
import { AdminRequest } from '../../../core/services/admin.service';
import { DocumentType } from '../../../core/models/user.model';

@Component({
  selector: 'app-admin-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-management.component.html',
  styleUrl: './admin-management.component.css'
})
export class AdminManagementComponent {
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  documentTypes = Object.values(DocumentType);

  adminForm: AdminRequest = {
    name: '',
    lastName: '',
    documentType: DocumentType.CC,
    documentNumber: '',
    email: '',
    password: '',
    phone: ''
  };

  constructor(private adminService: AdminService) {}

  createAdmin(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.adminService.createAdmin(this.adminForm).subscribe({
      next: () => {
        this.successMessage.set('Administrador creado exitosamente');
        this.resetForm();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al crear administrador');
        this.loading.set(false);
        console.error('Error creating admin:', error);
      }
    });
  }

  resetForm(): void {
    this.adminForm = {
      name: '',
      lastName: '',
      documentType: DocumentType.CC,
      documentNumber: '',
      email: '',
      password: '',
      phone: ''
    };
  }
}

