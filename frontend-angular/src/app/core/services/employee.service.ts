import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models/user.model';
import { Employee, EmployeeRequest } from '../models/employee.model';

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {
  private readonly employeeApiUrl = `${environment.apiUrl}/employee`;
  private readonly adminEmployeeApiUrl = `${environment.apiUrl}/admin/employees`;

  constructor(private http: HttpClient) {}

  // Métodos para empleados (obtener usuarios/clientes)
  getAllUsers(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>(`${this.employeeApiUrl}/users`);
  }

  getUserById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.employeeApiUrl}/users/${id}`);
  }

  // Métodos para administradores (gestión de empleados)
  getAllEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(this.adminEmployeeApiUrl);
  }

  getEmployeeById(id: number): Observable<Employee> {
    return this.http.get<Employee>(`${this.adminEmployeeApiUrl}/${id}`);
  }

  createEmployee(employee: EmployeeRequest): Observable<any> {
    return this.http.post(this.adminEmployeeApiUrl, employee);
  }

  deleteEmployee(id: number): Observable<any> {
    return this.http.delete(`${this.adminEmployeeApiUrl}/${id}`);
  }
}
