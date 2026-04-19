import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { UiFeedbackService } from '../services/ui-feedback.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const uiFeedback = inject(UiFeedbackService);
  const token = authService.getToken();

  // Clone request and add auth header if token exists
  let authReq = req;
  if (token) {
    authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        uiFeedback.warning('Session Expired', 'Please sign in again to continue.');
        authService.logout();
      } else if (error.status === 0) {
        uiFeedback.error('Network Error', 'Unable to reach the server. Check your connection and try again.');
      } else if (error.status >= 500) {
        uiFeedback.error('Server Error', 'The server had an issue. Please retry in a moment.');
      }

      return throwError(() => error);
    })
  );
};
