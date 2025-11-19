import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductRequest, ProductResponse } from '../models/product.model';

@Injectable({
  providedIn: 'root'
})
export class ProductService {
  private readonly apiUrl = `${environment.apiUrl}/admin/products`;
  private readonly publicApiUrl = `${environment.apiUrl}/public/products`;

  constructor(private http: HttpClient) {}

  // Métodos públicos (sin autenticación)
  getAllPublicProducts(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(this.publicApiUrl);
  }

  getPublicProductById(id: number): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.publicApiUrl}/${id}`);
  }

  getPublicProductsByCategory(category: string): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${this.publicApiUrl}/category/${category}`);
  }

  // Métodos privados (requieren autenticación ADMIN)
  getAllProducts(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(this.apiUrl);
  }

  getProductById(id: number): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.apiUrl}/${id}`);
  }

  getProductsByCategory(category: string): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${this.apiUrl}/category/${category}`);
  }

  createProduct(product: ProductRequest): Observable<ProductResponse> {
    return this.http.post<ProductResponse>(this.apiUrl, product);
  }

  updateProduct(id: number, product: ProductRequest): Observable<ProductResponse> {
    return this.http.put<ProductResponse>(`${this.apiUrl}/${id}`, product);
  }

  deleteProduct(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  uploadImage(file: File): Observable<{ imageUrl: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ imageUrl: string }>(`${this.apiUrl}/upload-image`, formData);
  }
}

