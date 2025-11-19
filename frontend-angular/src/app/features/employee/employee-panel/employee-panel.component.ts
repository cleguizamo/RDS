import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../../core/services/auth.service';
import { OrderManagementComponent } from '../order-management/order-management.component';
import { EmployeeReservationManagementComponent } from '../reservation-management/reservation-management.component';

type TabType = 'pedidos' | 'reservas';

@Component({
  selector: 'app-employee-panel',
  standalone: true,
  imports: [
    CommonModule,
    OrderManagementComponent,
    EmployeeReservationManagementComponent
  ],
  templateUrl: './employee-panel.component.html',
  styleUrl: './employee-panel.component.css'
})
export class EmployeePanelComponent {
  activeTab = signal<TabType>('pedidos');

  constructor(public authService: AuthService) {}

  setTab(tab: TabType): void {
    this.activeTab.set(tab);
  }

  logout(): void {
    this.authService.logout();
  }
}
