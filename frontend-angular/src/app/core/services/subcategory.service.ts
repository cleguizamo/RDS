import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SubCategoryRequest, SubCategoryResponse } from '../models/subcategory.model';

@Injectable({
  providedIn: 'root'
})
export class SubCategoryService {
  private apiUrl = `${environment.apiUrl}/admin/subcategories`;
  private publicApiUrl = `${environment.apiUrl}/public/subcategories`;

  constructor(private http: HttpClient) {}

  // Métodos para administradores
  getAllSubCategories(): Observable<SubCategoryResponse[]> {
    return this.http.get<SubCategoryResponse[]>(this.apiUrl);
  }

  getSubCategoriesByCategory(categoryId: number): Observable<SubCategoryResponse[]> {
    return this.http.get<SubCategoryResponse[]>(`${this.apiUrl}/category/${categoryId}`);
  }

  getSubCategoryById(id: number): Observable<SubCategoryResponse> {
    return this.http.get<SubCategoryResponse>(`${this.apiUrl}/${id}`);
  }

  createSubCategory(subCategory: SubCategoryRequest): Observable<SubCategoryResponse> {
    return this.http.post<SubCategoryResponse>(this.apiUrl, subCategory);
  }

  updateSubCategory(id: number, subCategory: SubCategoryRequest): Observable<SubCategoryResponse> {
    return this.http.put<SubCategoryResponse>(`${this.apiUrl}/${id}`, subCategory);
  }

  deleteSubCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // Métodos públicos (para el menú)
  getAllPublicSubCategories(): Observable<SubCategoryResponse[]> {
    return this.http.get<SubCategoryResponse[]>(this.publicApiUrl);
  }

  getPublicSubCategoriesByCategory(categoryId: number): Observable<SubCategoryResponse[]> {
    return this.http.get<SubCategoryResponse[]>(`${this.publicApiUrl}/category/${categoryId}`);
  }
}

