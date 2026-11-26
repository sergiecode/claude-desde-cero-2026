import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthService, LOGIN_URL } from './auth.service';

/** Agrega `Authorization: Bearer <token>` a las llamadas a `/api`, salvo el login (frontend.md §4). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (!isApiUrl(req.url) || isLoginUrl(req.url)) {
    return next(req);
  }
  const token = inject(AuthService).getToken();
  if (!token) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};

export function isApiUrl(url: string): boolean {
  return url === '/api' || url.startsWith('/api/');
}

export function isLoginUrl(url: string): boolean {
  return url.split('?')[0] === LOGIN_URL;
}
