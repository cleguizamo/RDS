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
    return this.http.get<FinancialStatsResponse>(`${this.apiUrl}/financial`, { params });
  }

  getBusinessStats(): Observable<BusinessStatsResponse> {
    return this.http.get<BusinessStatsResponse>(`${this.apiUrl}/business`);
  }
}

