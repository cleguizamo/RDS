import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { PasswordService } from '../../../core/services/password.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm: FormGroup;
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  loading = signal<boolean>(false);
  emailVerified = signal<boolean>(false);
  codeVerified = signal<boolean>(false);

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.resetPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      code: ['', [Validators.required, Validators.pattern(/^\d{8}$/)]],
      newPassword: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    // Resetear estados al iniciar
    this.emailVerified.set(false);
    this.codeVerified.set(false);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    
    // Permitir solo números en el campo de código
    this.resetPasswordForm.get('code')?.valueChanges.subscribe(value => {
      if (value && !/^\d*$/.test(value)) {
        this.resetPasswordForm.get('code')?.setValue(value.replace(/[^\d]/g, ''), { emitEvent: false });
      }
    });
  }

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
    } else if (confirmPassword) {
      confirmPassword.setErrors(null);
    }
    
    return null;
  }

  checkEmail(): void {
    if (!this.email?.valid) {
      this.markFormGroupTouched(this.resetPasswordForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const email = this.resetPasswordForm.value.email;

    this.passwordService.checkEmail({ email }).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.exists) {
          this.emailVerified.set(true);
          this.errorMessage.set(null); // Limpiar errores anteriores
          this.successMessage.set('✓ Email verificado. Se ha enviado un código de 8 dígitos a tu correo electrónico.');
          // Limpiar el campo de código para que pueda ingresar uno nuevo
          this.resetPasswordForm.get('code')?.reset();
        } else {
          this.emailVerified.set(false);
          this.errorMessage.set('El email no está registrado en nuestro sistema.');
        }
      },
      error: (error) => {
        this.loading.set(false);
        this.emailVerified.set(false);
        const errorMsg = error.error?.message || 'Error al verificar el email';
        this.errorMessage.set(errorMsg);
      }
    });
  }

  verifyCode(): void {
    if (!this.emailVerified()) {
      // Si el email no está verificado, verificar primero
      this.checkEmail();
      return;
    }

    if (!this.code?.valid) {
      this.markFormGroupTouched(this.resetPasswordForm);
      if (!this.code?.value) {
        this.errorMessage.set('Por favor ingresa el código de 8 dígitos');
      }
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const { email, code } = this.resetPasswordForm.value;

    this.passwordService.verifyResetCode({ email, code }).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.valid) {
          this.codeVerified.set(true);
          this.errorMessage.set(null);
          this.successMessage.set('✓ Código verificado correctamente. Ahora puedes cambiar tu contraseña.');
          // Limpiar campos de contraseña para que pueda ingresar uno nuevo
          this.resetPasswordForm.get('newPassword')?.reset();
          this.resetPasswordForm.get('confirmPassword')?.reset();
        } else {
          this.codeVerified.set(false);
          this.errorMessage.set(response.message || 'Código inválido o expirado');
        }
      },
      error: (error) => {
        this.loading.set(false);
        this.codeVerified.set(false);
        const errorMsg = error.error?.message || 'Error al verificar el código';
        this.errorMessage.set(errorMsg);
      }
    });
  }

  onSubmit(): void {
    if (!this.emailVerified()) {
      this.checkEmail();
      return;
    }

    if (!this.codeVerified()) {
      this.verifyCode();
      return;
    }

    if (this.resetPasswordForm.invalid) {
      this.markFormGroupTouched(this.resetPasswordForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { email, code, newPassword } = this.resetPasswordForm.value;

    this.passwordService.resetPassword({ email, code, newPassword }).subscribe({
      next: (response) => {
        this.loading.set(false);
        this.successMessage.set(response.message || 'Contraseña restablecida exitosamente');
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        this.loading.set(false);
        const errorMsg = error.error?.message || 'Error al restablecer la contraseña. El código puede haber expirado.';
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

  get email() {
    return this.resetPasswordForm.get('email');
  }

  get code() {
    return this.resetPasswordForm.get('code');
  }

  get newPassword() {
    return this.resetPasswordForm.get('newPassword');
  }

  get confirmPassword() {
    return this.resetPasswordForm.get('confirmPassword');
  }
}

