import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FinancialStatsResponse, BusinessStatsResponse } from '../models/statistics.model';

@Injectable({
  providedIn: 'root'
})
export class StatisticsService {
  private apiUrl = `${environment.apiUrl}/admin/statistics`;

  constructor(private http: HttpClient) {}

  getFinancialStats(startDate?: string, endDate?: string): Observable<FinancialStatsResponse> {
    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    // Agregar timestamp para evitar cache del navegador
    params = params.set('_t', Date.now().toString());
    return this.http.get<FinancialStatsResponse>(`${this.apiUrl}/financial`, { params });
  }

  getBusinessStats(): Observable<BusinessStatsResponse> {
    // Agregar timestamp para evitar cache del navegador
    const params = new HttpParams().set('_t', Date.now().toString());
    return this.http.get<BusinessStatsResponse>(`${this.apiUrl}/business`, { params });
  }

  // Exportación de estadísticas
  exportFinancialStatsToExcel(startDate: string, endDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get(`${environment.apiUrl}/admin/export/statistics/excel`, {
      params,
      responseType: 'blob'
    });
  }

  exportFinancialStatsToPdf(startDate: string, endDate: string): Observable<Blob> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get(`${environment.apiUrl}/admin/export/statistics/pdf`, {
      params,
      responseType: 'blob'
    });
  }
}

