import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReservationService } from '../../../core/services/reservation.service';
import { Reservation } from '../../../core/models/reservation.model';

@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reservation-management.component.html',
  styleUrl: './reservation-management.component.css'
})
export class ReservationManagementComponent implements OnInit {
  reservations = signal<Reservation[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  filterStatus = signal<string>('all'); // 'all', 'pending', 'confirmed'

  constructor(private reservationService: ReservationService) {}

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reservationService.getAllReservations().subscribe({
      next: (reservations) => {
        this.reservations.set(reservations);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading reservations:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar reservas. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  getFilteredReservations(): Reservation[] {
    const reservations = this.reservations();
    const filter = this.filterStatus();
    
    if (filter === 'all') {
      return reservations;
    } else if (filter === 'pending') {
      return reservations.filter(r => !r.status);
    } else {
      return reservations.filter(r => r.status);
    }
  }

  deleteReservation(id: number): void {
    if (!confirm('¿Estás seguro de eliminar esta reserva?')) return;

    this.loading.set(true);
    this.reservationService.deleteReservation(id).subscribe({
      next: () => {
        this.loadReservations();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set('Error al eliminar reserva');
        this.loading.set(false);
        console.error('Error deleting reservation:', error);
      }
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }

  formatTime(time: string): string {
    return time.substring(0, 5); // HH:MM
  }
}

