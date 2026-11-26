import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { isLoginUrl } from '../auth/auth.interceptor';
import { AuthService } from '../auth/auth.service';
import { ProblemDetails } from '../models/problem-details.model';
import { ErrorMessageService } from './error-message.service';
import { HANDLED_ERROR_CODES } from './handled-error-codes';

export const CONNECTION_ERROR_MESSAGE = 'No se pudo conectar con el servidor';
const SESSION_EXPIRED_MESSAGE = 'La sesión expiró. Ingresá nuevamente';
export const UNEXPECTED_ERROR_MESSAGE = 'Ocurrió un error inesperado. Intentá nuevamente';

/**
 * Manejo global de errores HTTP (frontend.md §4):
 * - Sin respuesta del servidor: "No se pudo conectar con el servidor".
 * - 401 (salvo en el login): logout, redirige a /login y muestra el `detail`.
 * - Otros: si la llamada no declaró su `code` en HANDLED_ERROR_CODES, muestra el `detail`.
 * En todos los casos el error se propaga al componente.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const messages = inject(ErrorMessageService);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }
      const problem = asProblemDetails(error.error);

      if (error.status === 0) {
        messages.show(CONNECTION_ERROR_MESSAGE);
      } else if (error.status === 401 && !isLoginUrl(req.url)) {
        auth.logout();
        void router.navigateByUrl('/login');
        messages.show(problem?.detail ?? SESSION_EXPIRED_MESSAGE);
      } else {
        const handled = req.context.get(HANDLED_ERROR_CODES);
        const isHandled = handled.includes('*') || (!!problem && handled.includes(problem.code));
        if (!isHandled) {
          messages.show(problem?.detail ?? UNEXPECTED_ERROR_MESSAGE);
        }
      }
      return throwError(() => error);
    }),
  );
};

/** Devuelve el cuerpo como ProblemDetails si trae `detail`; si no, `null`. */
export function asProblemDetails(body: unknown): ProblemDetails | null {
  if (body && typeof body === 'object' && typeof (body as ProblemDetails).detail === 'string') {
    return body as ProblemDetails;
  }
  return null;
}
