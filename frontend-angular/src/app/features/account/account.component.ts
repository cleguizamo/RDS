import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ClientService } from '../../core/services/client.service';
import { PasswordService } from '../../core/services/password.service';
import { UserResponse, DocumentType } from '../../core/models/user.model';
import { FormatCurrencyPipe } from '../../shared/pipes/format-currency.pipe';

@Component({
  selector: 'app-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormatCurrencyPipe],
  templateUrl: './account.component.html',
  styleUrl: './account.component.css'
})
export class AccountComponent implements OnInit {
  userInfo = signal<UserResponse | null>(null);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  
  // Cambio de contraseña
  showChangePassword = signal<boolean>(false);
  changePasswordForm: FormGroup;
  changingPassword = signal<boolean>(false);

  constructor(
    private clientService: ClientService,
    private passwordService: PasswordService,
    private fb: FormBuilder
  ) {
    this.changePasswordForm = this.fb.group({
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    this.loadUserInfo();
  }

  loadUserInfo(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.clientService.getProfile().subscribe({
      next: (user) => {
        this.userInfo.set(user);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading user profile:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar información del usuario');
        this.loading.set(false);
      }
    });
  }

  toggleChangePassword(): void {
    this.showChangePassword.set(!this.showChangePassword());
    if (this.showChangePassword()) {
      this.changePasswordForm.reset();
      this.errorMessage.set(null);
      this.successMessage.set(null);
    }
  }

  passwordMatchValidator(form: FormGroup) {
    const newPassword = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (newPassword && confirmPassword && newPassword.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
    } else if (confirmPassword && confirmPassword.value) {
      confirmPassword.setErrors(null);
    }
    
    return null;
  }

  onChangePassword(): void {
    if (this.changePasswordForm.invalid) {
      this.markFormGroupTouched(this.changePasswordForm);
      return;
    }

    this.changingPassword.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { currentPassword, newPassword } = this.changePasswordForm.value;

    this.passwordService.changePassword({ currentPassword, newPassword }).subscribe({
      next: (response) => {
        this.changingPassword.set(false);
        this.successMessage.set(response.message || 'Contraseña actualizada exitosamente');
        this.changePasswordForm.reset();
        setTimeout(() => {
          this.showChangePassword.set(false);
          this.successMessage.set(null);
        }, 3000);
      },
      error: (error) => {
        this.changingPassword.set(false);
        const errorMsg = error.error?.message || 'Error al cambiar la contraseña';
        this.errorMessage.set(errorMsg);
      }
    });
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  getDocumentTypeLabel(type: DocumentType): string {
    const labels: Record<DocumentType, string> = {
      [DocumentType.CC]: 'Cédula de Ciudadanía',
      [DocumentType.CE]: 'Cédula de Extranjería',
      [DocumentType.TI]: 'Tarjeta de Identidad'
    };
    return labels[type] || type;
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('es-CO', { year: 'numeric', month: 'long', day: 'numeric' });
  }

  get currentPassword() {
    return this.changePasswordForm.get('currentPassword');
  }

  get newPassword() {
    return this.changePasswordForm.get('newPassword');
  }

  get confirmPassword() {
    return this.changePasswordForm.get('confirmPassword');
  }
}

