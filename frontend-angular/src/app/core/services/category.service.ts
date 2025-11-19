import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CategoryRequest, CategoryResponse } from '../models/category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoryService {
  private apiUrl = `${environment.apiUrl}/admin/categories`;
  private publicApiUrl = `${environment.apiUrl}/public/categories`;

  constructor(private http: HttpClient) {}

  // Métodos para administradores
  getAllCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(this.apiUrl);
  }

  getCategoryById(id: number): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.apiUrl}/${id}`);
  }

  createCategory(category: CategoryRequest): Observable<CategoryResponse> {
    return this.http.post<CategoryResponse>(this.apiUrl, category);
  }

  updateCategory(id: number, category: CategoryRequest): Observable<CategoryResponse> {
    return this.http.put<CategoryResponse>(`${this.apiUrl}/${id}`, category);
  }

  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Métodos públicos (para el menú)
  getAllPublicCategories(): Observable<CategoryResponse[]> {
    return this.http.get<CategoryResponse[]>(this.publicApiUrl);
  }

  getPublicCategoryById(id: number): Observable<CategoryResponse> {
    return this.http.get<CategoryResponse>(`${this.publicApiUrl}/${id}`);
  }
}

