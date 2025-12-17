import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  BalanceResponse,
  Transaction,
  TransactionType,
  Alert,
  BalanceInitializationRequest,
  BalanceAdjustmentRequest,
  MigrationResult
} from '../models/balance.model';
import { SalaryPayment } from '../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class BalanceService {
  private readonly apiUrl = `${environment.apiUrl}/admin/balance`;

  constructor(private http: HttpClient) {}

  getCurrentBalance(): Observable<BalanceResponse> {
    return this.http.get<BalanceResponse>(`${this.apiUrl}`);
  }

  initializeBalance(request: BalanceInitializationRequest): Observable<BalanceResponse> {
    return this.http.post<BalanceResponse>(`${this.apiUrl}/initialize`, request);
  }

  updateLowBalanceThreshold(threshold: number): Observable<BalanceResponse> {
    return this.http.put<BalanceResponse>(`${this.apiUrl}/threshold`, { threshold });
  }

  adjustBalance(request: BalanceAdjustmentRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.apiUrl}/adjust`, request);
  }

  getAllTransactions(
    type?: TransactionType,
    page: number = 0,
    size: number = 50
  ): Observable<Transaction[]> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    
    if (type) {
      params = params.set('type', type);
    }

    return this.http.get<Transaction[]>(`${this.apiUrl}/transactions`, { params });
  }

  getPendingPayments(): Observable<SalaryPayment[]> {
    return this.http.get<SalaryPayment[]>(`${this.apiUrl}/pending-payments`);
  }

  processPendingPayments(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/process-pending`, {});
  }

  processSalaryPayments(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/process-salary-payments`, {});
  }

  getAlerts(activeOnly: boolean = false): Observable<Alert[]> {
    const params = new HttpParams().set('activeOnly', activeOnly.toString());
    return this.http.get<Alert[]>(`${this.apiUrl}/alerts`, { params });
  }

  resolveAlert(alertId: number): Observable<Alert> {
    return this.http.put<Alert>(`${this.apiUrl}/alerts/${alertId}/resolve`, {});
  }

  migrateHistoricalData(): Observable<MigrationResult> {
    return this.http.post<MigrationResult>(`${this.apiUrl}/migrate-historical-data`, {});
  }
}

