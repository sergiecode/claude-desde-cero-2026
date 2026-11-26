import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { LoginPageComponent } from './features/login/login-page.component';
import { QuoteListPageComponent } from './features/quotes/quote-list-page.component';
import { ShellComponent } from './layout/shell.component';

// Rutas de frontend.md §3. Las pantallas de cotización y póliza se agregan en sus etapas.
export const routes: Routes = [
  { path: 'login', component: LoginPageComponent, title: 'Ingresar' },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'quotes', component: QuoteListPageComponent, title: 'Mis cotizaciones' },
      { path: '', pathMatch: 'full', redirectTo: 'quotes' },
    ],
  },
  { path: '**', redirectTo: 'quotes' },
];
