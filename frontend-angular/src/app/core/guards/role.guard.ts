import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/user.model';

export const roleGuard = (allowedRoles: Role[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);

    const isAuth = authService.isAuthenticated();
    if (!isAuth) {
      router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
      return false;
    }

    if (authService.hasAnyRole(allowedRoles)) {
      return true;
    }

    // Si no tiene el rol adecuado, redirigir al dashboard según su rol
    const user = authService.currentUser();
    if (user?.role === Role.ADMIN) {
      router.navigate(['/admin']);
    } else if (user?.role === Role.EMPLOYEE) {
      router.navigate(['/employee']);
    } else {
      router.navigate(['/dashboard']);
    }
    return false;
  };
};

