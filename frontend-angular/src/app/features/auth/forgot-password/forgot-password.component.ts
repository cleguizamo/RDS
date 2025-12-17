import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, FormArray, FormControl } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PasswordService } from '../../../core/services/password.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent implements OnInit {
  forgotPasswordForm: FormGroup;
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  loading = signal<boolean>(false);
  emailVerified = signal<boolean>(false);
  codeVerified = signal<boolean>(false);
  
  @ViewChild('codeInputsContainer', { static: false }) codeInputsContainer!: ElementRef;

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordService,
    private router: Router
  ) {
    this.forgotPasswordForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      code: this.fb.array([]),
      newPassword: [''], // No requerido inicialmente
      confirmPassword: [''] // No requerido inicialmente
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    // Resetear estados al iniciar
    this.emailVerified.set(false);
    this.codeVerified.set(false);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    
    // Inicializar array de código con 8 campos vacíos
    this.initializeCodeInputs();
  }

  private updatePasswordValidators(): void {
    const verified = this.codeVerified();
    if (verified) {
      // Agregar validadores cuando el código esté verificado
      this.forgotPasswordForm.get('newPassword')?.setValidators([Validators.required, Validators.minLength(6)]);
      this.forgotPasswordForm.get('confirmPassword')?.setValidators([Validators.required]);
    } else {
      // Remover validadores cuando no esté verificado
      this.forgotPasswordForm.get('newPassword')?.clearValidators();
      this.forgotPasswordForm.get('confirmPassword')?.clearValidators();
    }
    this.forgotPasswordForm.get('newPassword')?.updateValueAndValidity({ emitEvent: false });
    this.forgotPasswordForm.get('confirmPassword')?.updateValueAndValidity({ emitEvent: false });
  }

  initializeCodeInputs(): void {
    const codeArray = this.fb.array<FormControl<string | null>>([]);
    for (let i = 0; i < 8; i++) {
      codeArray.push(this.fb.control<string | null>('', [Validators.required, Validators.pattern(/^\d$/)]));
    }
    this.forgotPasswordForm.setControl('code', codeArray);
  }

  get codeArray(): FormArray<FormControl<string | null>> {
    return this.forgotPasswordForm.get('code') as FormArray<FormControl<string | null>>;
  }
  
  getCodeControl(index: number): FormControl<string | null> {
    return this.codeArray.at(index) as FormControl<string | null>;
  }

  getCodeValue(): string {
    return this.codeArray.controls.map(control => control.value || '').join('');
  }

  onCodeInput(event: any, index: number): void {
    const input = event.target;
    const value = input.value.replace(/[^\d]/g, ''); // Solo números
    
    if (value.length > 1) {
      input.value = value.charAt(0);
      this.codeArray.at(index).setValue(value.charAt(0), { emitEvent: false });
    } else {
      this.codeArray.at(index).setValue(value, { emitEvent: false });
    }
    
    // Auto-avanzar al siguiente campo si se ingresó un dígito
    if (value && index < 7) {
      const nextInput = this.codeInputsContainer?.nativeElement?.querySelector(`input:nth-child(${index + 2})`);
      if (nextInput) {
        nextInput.focus();
      }
    }
    
    // Auto-retroceder si se borró y no hay valor
    if (!value && index > 0) {
      const prevInput = this.codeInputsContainer?.nativeElement?.querySelector(`input:nth-child(${index})`);
      if (prevInput) {
        prevInput.focus();
      }
    }
  }

  onCodeKeyDown(event: KeyboardEvent, index: number): void {
    const input = event.target as HTMLInputElement;
    
    // Si se presiona Backspace y el campo está vacío, retroceder
    if (event.key === 'Backspace' && !input.value && index > 0) {
      event.preventDefault();
      const prevInput = this.codeInputsContainer?.nativeElement?.querySelector(`input:nth-child(${index})`);
      if (prevInput) {
        prevInput.focus();
        prevInput.select();
      }
    }
    
    // Si se presiona ArrowLeft, retroceder
    if (event.key === 'ArrowLeft' && index > 0) {
      event.preventDefault();
      const prevInput = this.codeInputsContainer?.nativeElement?.querySelector(`input:nth-child(${index})`);
      if (prevInput) {
        prevInput.focus();
      }
    }
    
    // Si se presiona ArrowRight, avanzar
    if (event.key === 'ArrowRight' && index < 7) {
      event.preventDefault();
      const nextInput = this.codeInputsContainer?.nativeElement?.querySelector(`input:nth-child(${index + 2})`);
      if (nextInput) {
        nextInput.focus();
      }
    }
  }

  onCodePaste(event: ClipboardEvent): void {
    event.preventDefault();
    const pastedData = event.clipboardData?.getData('text').replace(/[^\d]/g, '').slice(0, 8) || '';
    
    if (pastedData.length > 0) {
      for (let i = 0; i < 8; i++) {
        if (i < pastedData.length) {
          this.codeArray.at(i).setValue(pastedData[i], { emitEvent: false });
        } else {
          this.codeArray.at(i).setValue('', { emitEvent: false });
        }
      }
      
      // Enfocar el último campo con valor o el último campo
      const focusIndex = Math.min(pastedData.length, 7);
      setTimeout(() => {
        const input = this.codeInputsContainer?.nativeElement?.querySelector(`input:nth-child(${focusIndex + 1})`);
        if (input) {
          input.focus();
        }
      }, 0);
    }
  }

  checkEmail(): void {
    if (!this.email?.valid) {
      this.markFormGroupTouched(this.forgotPasswordForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const email = this.forgotPasswordForm.value.email;

    this.passwordService.checkEmail({ email }).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.exists) {
          this.emailVerified.set(true);
          this.errorMessage.set(null);
          this.successMessage.set('✓ Email verificado. Se ha enviado un código de 8 dígitos a tu correo electrónico.');
          // Enfocar el primer campo de código después de un breve delay
          setTimeout(() => {
            const firstInput = this.codeInputsContainer?.nativeElement?.querySelector('input');
            if (firstInput) {
              firstInput.focus();
            }
          }, 100);
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
      this.checkEmail();
      return;
    }

    const codeValue = this.getCodeValue();
    
    if (codeValue.length !== 8) {
      this.errorMessage.set('Por favor ingresa el código completo de 8 dígitos');
      this.markCodeArrayTouched();
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const email = this.forgotPasswordForm.value.email;

    this.passwordService.verifyResetCode({ email, code: codeValue }).subscribe({
      next: (response) => {
        this.loading.set(false);
        if (response.valid) {
          this.codeVerified.set(true);
          this.errorMessage.set(null);
          this.successMessage.set('✓ Código verificado correctamente. Ahora puedes cambiar tu contraseña.');
          // Actualizar validadores de contraseña
          this.updatePasswordValidators();
          // Limpiar campos de contraseña
          this.forgotPasswordForm.get('newPassword')?.reset();
          this.forgotPasswordForm.get('confirmPassword')?.reset();
        } else {
          this.codeVerified.set(false);
          this.updatePasswordValidators();
          this.errorMessage.set(response.message || 'Código inválido o expirado');
          // Limpiar los campos de código
          this.codeArray.controls.forEach(control => control.setValue(''));
        }
      },
      error: (error) => {
        this.loading.set(false);
        this.codeVerified.set(false);
        this.updatePasswordValidators();
        const errorMsg = error.error?.message || 'Error al verificar el código';
        this.errorMessage.set(errorMsg);
        // Limpiar los campos de código
        this.codeArray.controls.forEach(control => control.setValue(''));
        const firstInput = this.codeInputsContainer?.nativeElement?.querySelector('input');
        if (firstInput) {
          firstInput.focus();
        }
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

    if (this.forgotPasswordForm.invalid) {
      this.markFormGroupTouched(this.forgotPasswordForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const { email, newPassword } = this.forgotPasswordForm.value;
    const codeValue = this.getCodeValue();

    this.passwordService.resetPassword({ email, code: codeValue, newPassword }).subscribe({
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

  passwordMatchValidator(form: FormGroup) {
    const password = form.get('newPassword');
    const confirmPassword = form.get('confirmPassword');
    
    // Solo validar si ambos campos tienen valor
    if (password && confirmPassword && password.value && confirmPassword.value) {
      if (password.value !== confirmPassword.value) {
        confirmPassword.setErrors({ passwordMismatch: true });
      } else {
        // Remover el error de mismatch si las contraseñas coinciden
        if (confirmPassword.hasError('passwordMismatch')) {
          const errors = { ...confirmPassword.errors };
          delete errors['passwordMismatch'];
          confirmPassword.setErrors(Object.keys(errors).length > 0 ? errors : null);
        }
      }
    }
    
    return null;
  }

  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
      
      if (control instanceof FormArray) {
        control.controls.forEach(arrayControl => {
          arrayControl.markAsTouched();
        });
      }
    });
  }

  private markCodeArrayTouched(): void {
    this.codeArray.controls.forEach(control => {
      control.markAsTouched();
    });
  }

  get email() {
    return this.forgotPasswordForm.get('email');
  }

  get newPassword() {
    return this.forgotPasswordForm.get('newPassword');
  }

  get confirmPassword() {
    return this.forgotPasswordForm.get('confirmPassword');
  }
}

