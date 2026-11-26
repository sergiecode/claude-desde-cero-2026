import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Sin token o con token vencido redirige a `/login` (frontend.md §4). */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isAuthenticated()) {
    return true;
  }
  auth.logout();
  return inject(Router).createUrlTree(['/login']);
};
