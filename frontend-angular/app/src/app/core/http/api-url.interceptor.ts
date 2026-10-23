import { HttpInterceptorFn } from '@angular/common/http';
import { environment } from '../../../environments/environment';

/**
 * Interceptor que antepone la URL base del backend cuando environment.apiUrl está configurada.
 * Si environment.apiUrl está vacía (por defecto en local o al usar proxy de Vercel), la URL relativa /api/... se preserva.
 */
export const apiUrlInterceptor: HttpInterceptorFn = (request, next) => {
  if (environment.apiUrl && request.url.startsWith('/api')) {
    const baseUrl = environment.apiUrl.replace(/\/+$/, '');
    const apiRequest = request.clone({
      url: `${baseUrl}${request.url}`,
    });
    return next(apiRequest);
  }
  return next(request);
};
