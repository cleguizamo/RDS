import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Reservation, ReservationRequest } from '../models/reservation.model';

@Injectable({
  providedIn: 'root'
})
export class ReservationService {
  private readonly apiUrl = `${environment.apiUrl}/admin/reservations`;
  private readonly clientApiUrl = `${environment.apiUrl}/client/reservations`;

  constructor(private http: HttpClient) {}

  // Métodos para clientes
  createReservation(reservation: ReservationRequest): Observable<Reservation> {
    return this.http.post<Reservation>(this.clientApiUrl, reservation);
  }

  getClientReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(this.clientApiUrl);
  }

  // Métodos para empleados
  getAllReservationsForEmployee(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${environment.apiUrl}/employee/reservations`);
  }

  getReservationsByStatusForEmployee(status: boolean): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${environment.apiUrl}/employee/reservations/status/${status}`);
  }

  confirmReservation(id: number): Observable<Reservation> {
    return this.http.put<Reservation>(`${environment.apiUrl}/employee/reservations/${id}/confirm`, {});
  }

  // Métodos para administradores
  getAllReservations(): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(this.apiUrl);
  }

  getReservationById(id: number): Observable<Reservation> {
    return this.http.get<Reservation>(`${this.apiUrl}/${id}`);
  }

  getReservationsByDate(date: string): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/date/${date}`);
  }

  getReservationsByStatus(status: boolean): Observable<Reservation[]> {
    return this.http.get<Reservation[]>(`${this.apiUrl}/status/${status}`);
  }

  deleteReservation(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}

