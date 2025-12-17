import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExpenseRequest, ExpenseResponse, ExpenseSearchRequest } from '../models/expense.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  private apiUrl = `${environment.apiUrl}/admin/expenses`;

  constructor(private http: HttpClient) {}

  getAllExpenses(startDate?: string, endDate?: string): Observable<ExpenseResponse[]> {
    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<ExpenseResponse[]>(this.apiUrl, { params });
  }

  getExpensesByCategory(category: string): Observable<ExpenseResponse[]> {
    return this.http.get<ExpenseResponse[]>(`${this.apiUrl}/category/${category}`);
  }

  getExpenseById(id: number): Observable<ExpenseResponse> {
    return this.http.get<ExpenseResponse>(`${this.apiUrl}/${id}`);
  }

  createExpense(expense: ExpenseRequest): Observable<ExpenseResponse> {
    return this.http.post<ExpenseResponse>(this.apiUrl, expense);
  }

  updateExpense(id: number, expense: ExpenseRequest): Observable<ExpenseResponse> {
    return this.http.put<ExpenseResponse>(`${this.apiUrl}/${id}`, expense);
  }

  deleteExpense(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Búsqueda avanzada con paginación
  searchExpenses(searchRequest: ExpenseSearchRequest, page: number = 0, size: number = 20, sortBy: string = 'expenseDate', sortDirection: string = 'DESC'): Observable<PageResponse<ExpenseResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);
    
    return this.http.post<PageResponse<ExpenseResponse>>(`${this.apiUrl}/search`, searchRequest, { params });
  }

  // Paginación simple
  getAllExpensesPaginated(page: number = 0, size: number = 20, startDate?: string, endDate?: string): Observable<PageResponse<ExpenseResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<PageResponse<ExpenseResponse>>(this.apiUrl, { params });
  }

  // Exportación
  exportExpensesToExcel(searchRequest: ExpenseSearchRequest): Observable<Blob> {
    return this.http.post(`${environment.apiUrl}/admin/export/expenses/excel`, searchRequest, {
      responseType: 'blob'
    });
  }
}

