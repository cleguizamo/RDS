import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { UserManagementComponent } from '../user-management/user-management.component';
import { ProductManagementComponent } from '../product-management/product-management.component';
import { ProductFormComponent } from '../product-form/product-form.component';
import { EmployeeManagementComponent } from '../employee-management/employee-management.component';
import { ReservationManagementComponent } from '../reservation-management/reservation-management.component';
import { OrderManagementComponent } from '../order-management/order-management.component';
import { AdminManagementComponent } from '../admin-management/admin-management.component';
import { ProductResponse } from '../../../core/models/product.model';

type TabType = 'usuarios' | 'productos' | 'agregar-producto' | 'reservas' | 'pedidos' | 'empleados' | 'admins';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [
    CommonModule,
    UserManagementComponent,
    ProductManagementComponent,
    ProductFormComponent,
    EmployeeManagementComponent,
    ReservationManagementComponent,
    OrderManagementComponent,
    AdminManagementComponent
  ],
  templateUrl: './admin-panel.component.html',
  styleUrl: './admin-panel.component.css'
})
export class AdminPanelComponent {
  activeTab = signal<TabType>('usuarios');
  editingProduct = signal<ProductResponse | null>(null);

  constructor(public authService: AuthService) {}

  setTab(tab: TabType): void {
    this.activeTab.set(tab);
    if (tab !== 'agregar-producto') {
      this.editingProduct.set(null);
    }
  }

  onProductSaved(product: ProductResponse): void {
    // Recargar productos y volver a la vista de productos
    this.setTab('productos');
    this.editingProduct.set(null);
  }

  onProductEdit(product: ProductResponse): void {
    this.editingProduct.set(product);
    this.setTab('agregar-producto');
  }

  onCancel(): void {
    this.editingProduct.set(null);
    this.setTab('productos');
  }

  logout(): void {
    this.authService.logout();
  }
}
