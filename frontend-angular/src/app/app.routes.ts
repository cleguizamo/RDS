import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { Role } from './core/models/user.model';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./features/auth/signup/signup.component').then(m => m.SignupComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/client/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    loadComponent: () => import('./features/admin/admin-panel/admin-panel.component').then(m => m.AdminPanelComponent),
    canActivate: [roleGuard([Role.ADMIN])]
  },
  {
    path: 'employee',
    loadComponent: () => import('./features/employee/employee-panel/employee-panel.component').then(m => m.EmployeePanelComponent),
    canActivate: [roleGuard([Role.EMPLOYEE])]
  },
  {
    path: 'menu',
    loadComponent: () => import('./features/menu/menu.component').then(m => m.MenuComponent)
  },
  {
    path: 'reservations',
    loadComponent: () => import('./features/reservations/reservations.component').then(m => m.ReservationsComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
