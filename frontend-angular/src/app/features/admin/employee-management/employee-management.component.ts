import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmployeeService } from '../../../core/services/employee.service';
import { Employee, EmployeeRequest, SalaryUpdateRequest, SalaryPayment, PaymentFrequency } from '../../../core/models/employee.model';
import { DocumentType } from '../../../core/models/user.model';

interface EmployeeSearchRequest {
  name?: string;
  email?: string;
  documentNumber?: string;
  documentType?: DocumentType;
}

@Component({
  selector: 'app-employee-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './employee-management.component.html',
  styleUrl: './employee-management.component.css'
})
export class EmployeeManagementComponent implements OnInit {
  allEmployees = signal<Employee[]>([]);
  loading = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);
  showForm = signal<boolean>(false);
  documentTypes = Object.values(DocumentType);

  // Búsqueda avanzada
  showAdvancedSearch = signal<boolean>(false);
  searchRequest = signal<EmployeeSearchRequest>({});
  searchTerm = signal<string>('');

  // Sueldos
  showSalaryModal = signal<boolean>(false);
  showPaymentHistoryModal = signal<boolean>(false);
  selectedEmployee = signal<Employee | null>(null);
  salaryPayments = signal<SalaryPayment[]>([]);
  paymentFrequencies = Object.values(PaymentFrequency);
  
  salaryForm: SalaryUpdateRequest = {
    salary: 0,
    paymentFrequency: PaymentFrequency.MONTHLY,
    paymentDay: 1
  };

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
        this.allEmployees.set(employees);
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error loading employees:', error);
        this.errorMessage.set(error.error?.message || 'Error al cargar empleados. Verifica tu conexión o permisos.');
        this.loading.set(false);
      }
    });
  }

  getFilteredEmployees(): Employee[] {
    let filtered = [...this.allEmployees()];
    const search = this.searchRequest();
    const term = this.searchTerm().toLowerCase().trim();

    // Búsqueda simple por término
    if (term) {
      filtered = filtered.filter(emp =>
        emp.name.toLowerCase().includes(term) ||
        emp.lastName.toLowerCase().includes(term) ||
        emp.email.toLowerCase().includes(term) ||
        emp.documentNumber.includes(term) ||
        emp.id.toString().includes(term)
      );
    }

    // Búsqueda avanzada
    if (search.name) {
      const name = search.name.toLowerCase().trim();
      filtered = filtered.filter(emp =>
        `${emp.name} ${emp.lastName}`.toLowerCase().includes(name) ||
        emp.name.toLowerCase().includes(name) ||
        emp.lastName.toLowerCase().includes(name)
      );
    }

    if (search.email) {
      const email = search.email.toLowerCase().trim();
      filtered = filtered.filter(emp =>
        emp.email.toLowerCase().includes(email)
      );
    }

    if (search.documentNumber) {
      filtered = filtered.filter(emp =>
        emp.documentNumber.includes(search.documentNumber!)
      );
    }

    if (search.documentType) {
      filtered = filtered.filter(emp =>
        emp.documentType === search.documentType
      );
    }

    return filtered;
  }

  onSearchChange(term: string): void {
    this.searchTerm.set(term);
  }

  toggleAdvancedSearch(): void {
    this.showAdvancedSearch.set(!this.showAdvancedSearch());
  }

  closeAdvancedSearch(): void {
    this.showAdvancedSearch.set(false);
  }

  clearAdvancedFilters(): void {
    this.searchRequest.set({});
  }

  applyAdvancedSearch(): void {
    this.closeAdvancedSearch();
  }

  clearFilters(): void {
    this.searchTerm.set('');
    this.searchRequest.set({});
  }

  updateSearchField(field: keyof EmployeeSearchRequest, value: any): void {
    this.searchRequest.update(r => ({ ...r, [field]: value }));
  }

  hasActiveFilters(): boolean {
    const search = this.searchRequest();
    return Object.keys(search).length > 0;
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
    this.salaryForm = {
      salary: 0,
      paymentFrequency: PaymentFrequency.MONTHLY,
      paymentDay: 1
    };
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  createEmployee(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.employeeService.createEmployee(this.employeeForm).subscribe({
      next: (response: any) => {
        // Si se configuró sueldo al crear, actualizarlo
        if (this.salaryForm.salary > 0 && this.salaryForm.paymentDay >= 1 && this.salaryForm.paymentDay <= 31) {
          const employeeId = response?.userId || response?.id || response?.employee?.id;
          if (employeeId) {
            this.updateEmployeeSalaryAfterCreation(employeeId);
          } else {
            this.successMessage.set('Empleado creado exitosamente');
            this.loadEmployees();
            this.showForm.set(false);
            this.resetForm();
            this.loading.set(false);
          }
        } else {
          this.successMessage.set('Empleado creado exitosamente');
          this.loadEmployees();
          this.showForm.set(false);
          this.resetForm();
          this.loading.set(false);
        }
      },
      error: (error) => {
        let errorMsg = 'Error al crear empleado';
        if (error.error) {
          if (typeof error.error === 'string') {
            errorMsg = error.error;
          } else if (error.error.validationFailed && error.error.errors) {
            // Manejar errores de validación
            const errors = error.error.errors;
            const errorMessages = Object.entries(errors).map(([field, msg]) => {
              const fieldName = this.translateFieldName(field);
              return `${fieldName}: ${msg}`;
            });
            errorMsg = errorMessages.join('\n');
          } else if (error.error.message) {
            errorMsg = error.error.message;
          } else if (error.error.error) {
            errorMsg = error.error.error;
          } else {
            // Si error.error es un objeto, intentar extraer un mensaje
            try {
              errorMsg = JSON.stringify(error.error);
            } catch {
              errorMsg = 'Error al crear empleado';
            }
          }
        } else if (error.message) {
          errorMsg = error.message;
        }
        this.errorMessage.set(errorMsg);
        this.loading.set(false);
        console.error('Error creating employee:', error);
      }
    });
  }

  private updateEmployeeSalaryAfterCreation(employeeId: number): void {
    this.employeeService.updateEmployeeSalary(employeeId, this.salaryForm).subscribe({
      next: () => {
        this.successMessage.set('Empleado creado y sueldo configurado exitosamente');
        this.loadEmployees();
        this.showForm.set(false);
        this.resetForm();
        this.loading.set(false);
      },
      error: (error) => {
        console.error('Error updating salary after creation:', error);
        this.successMessage.set('Empleado creado, pero hubo un error al configurar el sueldo. Puedes configurarlo manualmente.');
        this.loadEmployees();
        this.showForm.set(false);
        this.resetForm();
        this.loading.set(false);
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

  openSalaryModal(employee: Employee): void {
    this.selectedEmployee.set(employee);
    this.salaryForm = {
      salary: employee.salary || 0,
      paymentFrequency: employee.paymentFrequency || PaymentFrequency.MONTHLY,
      paymentDay: employee.paymentDay || 1
    };
    this.showSalaryModal.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  closeSalaryModal(): void {
    this.showSalaryModal.set(false);
    this.selectedEmployee.set(null);
    this.salaryForm = {
      salary: 0,
      paymentFrequency: PaymentFrequency.MONTHLY,
      paymentDay: 1
    };
  }

  updateSalary(): void {
    if (!this.selectedEmployee()) return;

    this.loading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    this.employeeService.updateEmployeeSalary(this.selectedEmployee()!.id, this.salaryForm).subscribe({
      next: (updatedEmployee) => {
        this.successMessage.set('Sueldo actualizado exitosamente');
        this.loadEmployees();
        this.closeSalaryModal();
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error?.message || error.error || 'Error al actualizar sueldo');
        this.loading.set(false);
        console.error('Error updating salary:', error);
      }
    });
  }

  openPaymentHistoryModal(employee: Employee): void {
    this.selectedEmployee.set(employee);
    this.loading.set(true);
    this.errorMessage.set(null);
    this.salaryPayments.set([]);

    this.employeeService.getEmployeeSalaryPayments(employee.id).subscribe({
      next: (payments) => {
        this.salaryPayments.set(payments);
        this.showPaymentHistoryModal.set(true);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(error.error || 'Error al cargar historial de pagos');
        this.loading.set(false);
        console.error('Error loading payment history:', error);
      }
    });
  }

  closePaymentHistoryModal(): void {
    this.showPaymentHistoryModal.set(false);
    this.selectedEmployee.set(null);
    this.salaryPayments.set([]);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0
    }).format(amount);
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('es-CO', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }).format(date);
  }

  getPaymentFrequencyLabel(frequency: PaymentFrequency): string {
    return frequency === PaymentFrequency.MONTHLY ? 'Mensual' : 'Quincenal';
  }

  private translateFieldName(field: string): string {
    const translations: { [key: string]: string } = {
      'name': 'Nombre',
      'lastName': 'Apellido',
      'documentType': 'Tipo de Documento',
      'documentNumber': 'Número de Documento',
      'email': 'Email',
      'password': 'Contraseña',
      'phone': 'Teléfono'
    };
    return translations[field] || field;
  }
}

