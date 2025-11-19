import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface AdminRequest {
  name: string;
  lastName: string;
  documentType: string;
  documentNumber: string;
  email: string;
  password: string;
  phone: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  createAdmin(admin: AdminRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/admins`, admin);
  }
}

