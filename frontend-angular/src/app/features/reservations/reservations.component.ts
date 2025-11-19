import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ReservationService } from '../../core/services/reservation.service';
import { Reservation, ReservationRequest } from '../../core/models/reservation.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-reservations',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './reservations.component.html',
  styleUrl: './reservations.component.css'
})
export class ReservationsComponent implements OnInit {
  reservationForm: FormGroup;
  reservations = signal<Reservation[]>([]);
  loading = signal<boolean>(false);
  submitting = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(true);

  constructor(
    private fb: FormBuilder,
    private reservationService: ReservationService,
    public authService: AuthService
  ) {
    this.reservationForm = this.fb.group({
      date: ['', [Validators.required]],
      time: ['', [Validators.required]],
      numberOfPeople: ['', [Validators.required, Validators.min(1), Validators.max(20)]],
      notes: ['']
    });

    // Establecer fecha mínima como hoy
    const today = new Date().toISOString().split('T')[0];
    this.reservationForm.get('date')?.setValidators([
      Validators.required,
      this.minDateValidator(today)
    ]);
  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.loadReservations();
    }
  }

  minDateValidator(minDate: string) {
    return (control: any) => {
      if (!control.value) return null;
      const selectedDate = new Date(control.value);
      const today = new Date(minDate);
      today.setHours(0, 0, 0, 0);
      return selectedDate >= today ? null : { minDate: true };
    };
  }

  loadReservations(): void {
    if (!this.authService.isAuthenticated()) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getClientReservations().subscribe({
      next: (reservations) => {
        this.reservations.set(reservations);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading reservations:', error);
        this.errorMessage.set('Error al cargar las reservas');
        this.loading.set(false);
      }
    });
  }

  onSubmit(): void {
    if (!this.authService.isAuthenticated()) {
      this.errorMessage.set('Debes iniciar sesión para hacer una reserva');
      return;
    }

    if (this.reservationForm.invalid) {
      this.markFormGroupTouched(this.reservationForm);
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const formValue = this.reservationForm.value;
    const reservationRequest: ReservationRequest = {
      date: formValue.date,
      time: formValue.time,
      numberOfPeople: parseInt(formValue.numberOfPeople),
      notes: formValue.notes || undefined
    };

    this.reservationService.createReservation(reservationRequest).subscribe({
      next: (reservation) => {
        this.successMessage.set('¡Reserva creada exitosamente! Te contactaremos pronto para confirmarla.');
        this.reservationForm.reset();
        this.submitting.set(false);
        this.loadReservations();
        
        // Ocultar mensaje de éxito después de 5 segundos
        setTimeout(() => {
          this.successMessage.set(null);
        }, 5000);
      },
      error: (error) => {
        console.error('Error creating reservation:', error);
        this.errorMessage.set(error.error?.message || 'Error al crear la reserva. Por favor, intenta nuevamente.');
        this.submitting.set(false);
      }
    });
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('es-ES', {
      weekday: 'long',
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  formatTime(time: string): string {
    const [hours, minutes] = time.split(':');
    return `${hours}:${minutes}`;
  }

  toggleForm(): void {
    this.showForm.set(!this.showForm());
  }

  getErrorMessage(field: string): string {
    const control = this.reservationForm.get(field);
    if (control?.hasError('required')) {
      return 'Este campo es obligatorio';
    }
    if (control?.hasError('min')) {
      return `El mínimo es ${control.errors?.['min'].min}`;
    }
    if (control?.hasError('max')) {
      return `El máximo es ${control.errors?.['max'].max}`;
    }
    if (control?.hasError('minDate')) {
      return 'La fecha debe ser futura';
    }
    return '';
  }
}

