import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExpenseRequest, ExpenseResponse } from '../models/expense.model';

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
}

