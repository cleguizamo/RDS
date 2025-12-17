import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserResponse } from '../models/user.model';
import { Employee, EmployeeRequest, SalaryUpdateRequest, SalaryPayment } from '../models/employee.model';

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

  updateEmployeeSalary(id: number, salaryRequest: SalaryUpdateRequest): Observable<Employee> {
    return this.http.put<Employee>(`${this.adminEmployeeApiUrl}/${id}/salary`, salaryRequest);
  }

  getEmployeeSalaryPayments(id: number): Observable<SalaryPayment[]> {
    return this.http.get<SalaryPayment[]>(`${this.adminEmployeeApiUrl}/${id}/salary-payments`);
  }

  getAllSalaryPayments(startDate?: string, endDate?: string): Observable<SalaryPayment[]> {
    let params = new HttpParams();
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }
    return this.http.get<SalaryPayment[]>(`${this.adminEmployeeApiUrl}/salary-payments`, { params });
  }

  deleteEmployee(id: number): Observable<any> {
    return this.http.delete(`${this.adminEmployeeApiUrl}/${id}`);
  }
}
