import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse, UserUpdateRequest, UserSearchRequest } from '../models/user.model';
import { PageResponse } from '../models/page.model';

@Injectable({
  providedIn: 'root'
})
export class AdminUserService {
  private readonly apiUrl = `${environment.apiUrl}/admin/users`;

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(this.apiUrl);
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.apiUrl}/${id}`);
  }

  updateUser(id: number, user: UserUpdateRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/${id}`, user);
  }

  deleteUser(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // Búsqueda avanzada con paginación
  searchUsers(searchRequest: UserSearchRequest, page: number = 0, size: number = 20, sortBy: string = 'id', sortDirection: string = 'ASC'): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('sortDirection', sortDirection);
    
    return this.http.post<PageResponse<UserResponse>>(`${this.apiUrl}/search`, searchRequest, { params });
  }

  // Paginación simple
  getAllUsersPaginated(page: number = 0, size: number = 20): Observable<PageResponse<UserResponse>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<UserResponse>>(this.apiUrl, { params });
  }
}

