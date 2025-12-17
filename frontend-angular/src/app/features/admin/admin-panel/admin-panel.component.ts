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
import { CategoryManagementComponent } from '../category-management/category-management.component';
import { SubCategoryManagementComponent } from '../subcategory-management/subcategory-management.component';
import { RewardManagementComponent } from '../reward-management/reward-management.component';
import { ExpenseManagementComponent } from '../expense-management/expense-management.component';
import { FinancialDashboardComponent } from '../financial-dashboard/financial-dashboard.component';
import { BalanceManagementComponent } from '../balance-management/balance-management.component';
import { ProductResponse } from '../../../core/models/product.model';

type TabType = 'usuarios' | 'productos' | 'agregar-producto' | 'categorias' | 'subcategorias' | 'recompensas' | 'estadisticas' | 'gastos' | 'balance' | 'reservas' | 'pedidos' | 'empleados' | 'admins';

@Component({
  selector: 'app-admin-panel',
  standalone: true,
  imports: [
    CommonModule,
    UserManagementComponent,
    ProductManagementComponent,
    ProductFormComponent,
    CategoryManagementComponent,
    SubCategoryManagementComponent,
    RewardManagementComponent,
    ExpenseManagementComponent,
    FinancialDashboardComponent,
    BalanceManagementComponent,
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
    
    // Forzar recarga del componente de estadísticas cuando se cambia al tab
    // Esto asegura que los datos se actualicen después de crear pedidos/deliveries
    if (tab === 'estadisticas') {
      // El key cambia cada vez, forzando que el componente se reinicialice
      console.log('Tab de estadísticas seleccionado, forzando recarga de datos...');
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
