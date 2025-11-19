import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReservationService } from '../../../core/services/reservation.service';
import { OrderService } from '../../../core/services/order.service';
import { Reservation } from '../../../core/models/reservation.model';
import { OrderRequest, OrderType } from '../../../core/models/order.model';

@Component({
  selector: 'app-employee-reservation-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './reservation-management.component.html',
  styleUrl: './reservation-management.component.css'
})
export class EmployeeReservationManagementComponent implements OnInit {
  reservations = signal<Reservation[]>([]);
  filteredReservations = signal<Reservation[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  filterStatus = signal<'all' | 'pending' | 'confirmed'>('all');
  showOrderForm = signal<boolean>(false);
  selectedReservation = signal<Reservation | null>(null);
  tableNumber = signal<string>('');
  totalPrice = signal<string>('');

  constructor(
    private reservationService: ReservationService,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    this.loadReservations();
  }

  loadReservations(): void {
    this.loading.set(true);
    this.errorMessage.set(null);

    this.reservationService.getAllReservationsForEmployee().subscribe({
      next: (reservations) => {
        this.reservations.set(reservations);
        this.applyFilter();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading reservations:', error);
        this.errorMessage.set('Error al cargar las reservas. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  applyFilter(): void {
    const status = this.filterStatus();
    let filtered = this.reservations();

    if (status === 'pending') {
      filtered = filtered.filter(r => !r.status);
    } else if (status === 'confirmed') {
      filtered = filtered.filter(r => r.status);
    }

    this.filteredReservations.set(filtered);
  }

  setFilter(status: 'all' | 'pending' | 'confirmed'): void {
    this.filterStatus.set(status);
    this.applyFilter();
  }

  confirmReservation(reservation: Reservation): void {
    if (!confirm(`¿Confirmar la reserva #${reservation.id}?`)) {
      return;
    }

    this.reservationService.confirmReservation(reservation.id).subscribe({
      next: () => {
        this.loadReservations();
      },
      error: (error) => {
        console.error('Error confirming reservation:', error);
        this.errorMessage.set('Error al confirmar la reserva');
        setTimeout(() => this.errorMessage.set(null), 3000);
      }
    });
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('es-ES', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      weekday: 'long'
    });
  }

  formatTime(time: string): string {
    const [hours, minutes] = time.split(':');
    return `${hours}:${minutes}`;
  }

  parseFloat = parseFloat; // Expose parseFloat to template
  isNaN = isNaN; // Expose isNaN to template

  isValidPrice(): boolean {
    const price = parseFloat(this.totalPrice() || '0');
    return !isNaN(price) && price >= 0.01;
  }

  isValidForm(): boolean {
    return !this.loading() && 
           !!this.tableNumber() && 
           parseInt(this.tableNumber() || '0') >= 1 && 
           this.isValidPrice();
  }

  openOrderForm(reservation: Reservation): void {
    this.selectedReservation.set(reservation);
    this.showOrderForm.set(true);
    this.tableNumber.set('');
    this.totalPrice.set('');
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  cancelOrderForm(): void {
    this.showOrderForm.set(false);
    this.selectedReservation.set(null);
    this.tableNumber.set('');
    this.totalPrice.set('');
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  createOrderFromReservation(): void {
    const reservation = this.selectedReservation();
    if (!reservation) return;

    const tableNum = parseInt(this.tableNumber());
    const price = parseFloat(this.totalPrice());

    if (!tableNum || tableNum < 1) {
      this.errorMessage.set('El número de mesa es obligatorio y debe ser mayor a 0');
      return;
    }

    if (!price || price < 0.01) {
      this.errorMessage.set('El precio total es obligatorio y debe ser mayor a 0');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    // Crear un item básico para el pedido desde la reserva
    // Nota: En un caso real, aquí deberías poder seleccionar productos específicos
    // Por ahora, usamos un producto genérico o el sistema debería manejar esto diferente
    
    // Para esta funcionalidad, necesitamos obtener un producto válido o cambiar la lógica
    // Por ahora, esto fallará porque no hay items. Necesitamos una solución diferente.
    
    // Nota: Para crear un pedido desde una reserva, necesitamos productos.
    // Por ahora, redirigimos al usuario a usar la sección de pedidos con selección de productos.
    this.errorMessage.set('Para crear un pedido desde una reserva, por favor usa la sección de "Pedidos" en el panel de empleado y selecciona los productos necesarios. El cliente ya está asociado a la reserva #' + reservation.id + '.');
    this.loading.set(false);
  }
}

