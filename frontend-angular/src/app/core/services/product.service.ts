import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ProductRequest, ProductResponse, ProductSearchRequest } from '../models/product.model';
import { PageResponse } from '../models/page.model';

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

  getPublicProductsByCategory(categoryId: number): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${this.publicApiUrl}/category/${categoryId}`);
  }

  // Métodos privados (requieren autenticación ADMIN)
  getAllProducts(): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(this.apiUrl);
  }

  getProductById(id: number): Observable<ProductResponse> {
    return this.http.get<ProductResponse>(`${this.apiUrl}/${id}`);
  }

  getProductsByCategory(categoryId: number): Observable<ProductResponse[]> {
    return this.http.get<ProductResponse[]>(`${this.apiUrl}/category/${categoryId}`);
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

  // Búsqueda avanzada con paginación
  searchProducts(searchRequest: ProductSearchRequest, page: number = 0, size: number = 20, sortBy: string = 'id', sortDirection: string = 'ASC'): Observable<PageResponse<ProductResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);
    
    return this.http.post<PageResponse<ProductResponse>>(`${this.apiUrl}/search`, searchRequest, { params });
  }

  // Paginación simple
  getAllProductsPaginated(page: number = 0, size: number = 20): Observable<PageResponse<ProductResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<ProductResponse>>(this.apiUrl, { params });
  }
}

