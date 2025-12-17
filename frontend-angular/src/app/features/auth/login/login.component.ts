import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { LoginRequest } from '../../../core/models/user.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage = signal<string | null>(null);
  loading = signal<boolean>(false);
  returnUrl: string = '/dashboard';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });

    // Obtener returnUrl de query params o usar default
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      this.markFormGroupTouched(this.loginForm);
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    const loginRequest: LoginRequest = this.loginForm.value;

    this.authService.login(loginRequest).subscribe({
      next: (response) => {
        this.loading.set(false);
        console.log('Login exitoso:', response);
        console.log('Rol del usuario:', response.role);
        console.log('Redirect to:', response.redirectTo);
        
        // Verificar que el usuario tiene el rol correcto antes de redirigir
        if (!response.role) {
          console.error('Error: No se recibió el rol del usuario en la respuesta de login');
          this.errorMessage.set('Error: No se pudo determinar tu rol. Por favor, contacta al administrador.');
          return;
        }
        
        // Redirigir según el rol o returnUrl
        const redirectTo = response.redirectTo || this.returnUrl;
        console.log('Redirigiendo a:', redirectTo);
        this.router.navigate([redirectTo]);
      },
      error: (error) => {
        this.loading.set(false);
        console.error('Error en login:', error);
        console.error('Error status:', error.status);
        console.error('Error error:', error.error);
        console.error('Error completo:', JSON.stringify(error, null, 2));
        
        // Manejar diferentes formatos de error
        let errorMsg = 'Error al iniciar sesión. Verifica tus credenciales.';
        
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
        
        // Mensajes específicos según el código de estado
        if (error.status === 401 || error.status === 403) {
          errorMsg = 'Credenciales inválidas. Verifica tu email y contraseña.';
        } else if (error.status === 0 || !error.status) {
          errorMsg = `Error de conexión. Verifica que el servidor esté corriendo en ${environment.apiUrl.replace('/api', '')}.`;
        } else if (error.status >= 500) {
          errorMsg = 'Error del servidor. Por favor, intenta más tarde.';
        }
        
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
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }
}

