import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { tap } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  // Excluir rutas públicas y de autenticación
  const publicUrls = ['/auth/login', '/auth/signup', '/public'];
  const isPublicUrl = publicUrls.some(url => req.url.includes(url));
  
  // Si es una ruta pública, no agregar el token
  if (isPublicUrl) {
    return next(req);
  }

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
          console.error(`[JWT Interceptor] Error completo:`, error);
          
          // Si es 401 o 403, podría ser que el token expiró o es inválido
          if (error.status === 401 || error.status === 403) {
            // No hacer logout automático, dejar que el usuario vea el error
            console.warn(`[JWT Interceptor] Error ${error.status} - Token puede estar expirado o inválido`);
          }
        }
      }
    })
  );
};

