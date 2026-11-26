import { Routes } from '@angular/router';

import { LoginPageComponent } from './features/login/login-page.component';

// Rutas provisionales de la etapa 1.
// En la etapa 3, '' y '**' pasan a redirigir a /quotes (frontend.md §3).
export const routes: Routes = [
  { path: 'login', component: LoginPageComponent, title: 'Ingresar' },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
