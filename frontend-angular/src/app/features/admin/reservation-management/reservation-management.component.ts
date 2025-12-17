import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationService } from '../../../core/services/reservation.service';
import { Reservation } from '../../../core/models/reservation.model';

interface ReservationSearchRequest {
  clientName?: string;
  clientEmail?: string;
  status?: boolean | 'all';
  minDate?: string;
  maxDate?: string;
  minPeople?: number;
  maxPeople?: number;
}

@Component({
  selector: 'app-reservation-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation-management.component.html',
  styleUrl: './reservation-management.component.css'
})
export class ReservationManagementComponent implements OnInit {
  allReservations = signal<Reservation[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  filterStatus = signal<string>('all'); // 'all', 'pending', 'confirmed'

  // Búsqueda avanzada
  showAdvancedSearch = signal<boolean>(false);
  searchRequest = signal<ReservationSearchRequest>({});
  searchTerm = signal<string>('');

  constructor(private reservationService: ReservationService) {}

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reservationService.getAllReservations().subscribe({
      next: (reservations) => {
        this.allReservations.set(reservations);
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
    let filtered = [...this.allReservations()];
    const search = this.searchRequest();
    const term = this.searchTerm().toLowerCase().trim();
    const filter = this.filterStatus();

    // Filtro básico por estado
    if (filter === 'pending') {
      filtered = filtered.filter(r => !r.status);
    } else if (filter === 'confirmed') {
      filtered = filtered.filter(r => r.status);
    }

    // Búsqueda simple por término
    if (term) {
      filtered = filtered.filter(res =>
        res.userName.toLowerCase().includes(term) ||
        res.userEmail.toLowerCase().includes(term) ||
        res.id.toString().includes(term)
      );
    }

    // Búsqueda avanzada
    if (search.clientName) {
      const name = search.clientName.toLowerCase().trim();
      filtered = filtered.filter(res =>
        res.userName.toLowerCase().includes(name)
      );
    }

    if (search.clientEmail) {
      const email = search.clientEmail.toLowerCase().trim();
      filtered = filtered.filter(res =>
        res.userEmail.toLowerCase().includes(email)
      );
    }

    if (search.status !== undefined && search.status !== 'all') {
      filtered = filtered.filter(res => res.status === search.status);
    }

    if (search.minDate) {
      filtered = filtered.filter(res => res.date >= search.minDate!);
    }

    if (search.maxDate) {
      filtered = filtered.filter(res => res.date <= search.maxDate!);
    }

    if (search.minPeople !== undefined) {
      filtered = filtered.filter(res => res.numberOfPeople >= search.minPeople!);
    }

    if (search.maxPeople !== undefined) {
      filtered = filtered.filter(res => res.numberOfPeople <= search.maxPeople!);
    }

    return filtered;
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
  }

  toggleAdvancedSearch(): void {
    this.showAdvancedSearch.set(!this.showAdvancedSearch());
  }

  closeAdvancedSearch(): void {
    this.showAdvancedSearch.set(false);
  }

  clearAdvancedFilters(): void {
    this.searchRequest.set({});
  }

  applyAdvancedSearch(): void {
    this.closeAdvancedSearch();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.searchRequest.set({});
  }

  updateSearchField(field: keyof ReservationSearchRequest, value: any): void {
    let processedValue: any = value;
    
    // Procesar valores numéricos
    if (field === 'minPeople' || field === 'maxPeople') {
      processedValue = value && value !== '' ? parseInt(value, 10) : undefined;
    }
    
    this.searchRequest.update(r => ({ ...r, [field]: processedValue }));
  }

  hasActiveFilters(): boolean {
    const search = this.searchRequest();
    return Object.keys(search).length > 0;
  }

  confirmReservation(id: number): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.reservationService.confirmReservationAsAdmin(id).subscribe({
      next: () => {
        this.loadReservations();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error?.message || 'Error al confirmar reserva');
        this.loading.set(false);
        console.error('Error confirming reservation:', error);
      }
    });
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

