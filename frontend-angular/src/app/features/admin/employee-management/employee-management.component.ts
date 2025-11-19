import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmployeeService } from '../../../core/services/employee.service';
import { Employee, EmployeeRequest } from '../../../core/models/employee.model';
import { DocumentType } from '../../../core/models/user.model';

@Component({
  selector: 'app-employee-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './employee-management.component.html',
  styleUrl: './employee-management.component.css'
})
export class EmployeeManagementComponent implements OnInit {
  employees = signal<Employee[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  documentTypes = Object.values(DocumentType);

  employeeForm: EmployeeRequest = {
    name: '',
    lastName: '',
    documentType: DocumentType.CC,
    documentNumber: '',
    email: '',
    password: '',
    phone: ''
  };

  constructor(private employeeService: EmployeeService) {}

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.employeeService.getAllEmployees().subscribe({
      next: (employees) => {
        this.employees.set(employees);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading employees:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar empleados. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  showAddForm(): void {
    this.showForm.set(true);
    this.resetForm();
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.resetForm();
  }

  resetForm(): void {
    this.employeeForm = {
      name: '',
      lastName: '',
      documentType: DocumentType.CC,
      documentNumber: '',
      email: '',
      password: '',
      phone: ''
    };
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  createEmployee(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.employeeService.createEmployee(this.employeeForm).subscribe({
      next: () => {
        this.successMessage.set('Empleado creado exitosamente');
        this.loadEmployees();
        this.showForm.set(false);
        this.resetForm();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al crear empleado');
        this.loading.set(false);
        console.error('Error creating employee:', error);
      }
    });
  }

  deleteEmployee(id: number): void {
    if (!confirm('¿Estás seguro de eliminar este empleado?')) return;

    this.loading.set(true);
    this.errorMessage.set(null);

    this.employeeService.deleteEmployee(id).subscribe({
      next: () => {
        this.successMessage.set('Empleado eliminado exitosamente');
        this.loadEmployees();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al eliminar empleado');
        this.loading.set(false);
        console.error('Error deleting employee:', error);
      }
    });
  }
}

