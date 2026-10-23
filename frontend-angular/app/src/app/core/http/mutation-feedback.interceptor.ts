import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../notifications/notification.service';

export const mutationFeedbackInterceptor: HttpInterceptorFn = (request, next) => {
  const notifications = inject(NotificationService);
  return next(request).pipe(catchError(error => {
    // Las consultas y las sincronizaciones en segundo plano conservan su estado local de error.
    if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)
      && request.url.includes('/api/v1/') && !/\/vincular|\/sincronizar/.test(request.url)) {
      notifications.show(error.status === 0
        ? 'Sin conexión. No se pudo completar la acción. Comprueba tu conexión antes de reintentar.'
        : 'No se pudo completar la acción. Revisa los detalles en la página e inténtalo nuevamente.', 'error');
    }
    return throwError(() => error);
  }));
};
