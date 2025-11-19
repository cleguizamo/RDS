import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const token = authService.getToken();

  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    console.log(`[JWT Interceptor] Token agregado a petición: ${req.url}`);
  } else {
    console.warn(`[JWT Interceptor] No hay token disponible para: ${req.url}`);
  }

  return next(req).pipe(
    tap({
      error: (error) => {
        if (error.status === 401 || error.status === 403) {
          console.error(`[JWT Interceptor] Error de autenticación (${error.status}) para: ${req.url}`);
          console.error(`[JWT Interceptor] Token actual: ${token ? token.substring(0, 20) + '...' : 'null'}`);
        }
      }
    })
  );
};

