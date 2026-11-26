import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

// Rutas de frontend.md §3.
// Todas las pantallas se cargan en diferido con loadComponent.
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login-page.component').then((m) => m.LoginPageComponent),
    title: 'Ingresar',
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      {
        path: 'quotes',
        loadComponent: () =>
          import('./features/quotes/quote-list-page.component').then(
            (m) => m.QuoteListPageComponent,
          ),
        title: 'Mis cotizaciones',
      },
      {
        path: 'quotes/new',
        loadComponent: () =>
          import('./features/quotes/quote-new-page.component').then(
            (m) => m.QuoteNewPageComponent,
          ),
        title: 'Nueva cotización',
      },
      {
        path: 'quotes/:id',
        loadComponent: () =>
          import('./features/quotes/quote-detail-page.component').then(
            (m) => m.QuoteDetailPageComponent,
          ),
        title: 'Detalle de cotización',
      },
      {
        path: 'policies/:id',
        loadComponent: () =>
          import('./features/policies/policy-detail-page.component').then(
            (m) => m.PolicyDetailPageComponent,
          ),
        title: 'Detalle de póliza',
      },
      { path: '', pathMatch: 'full', redirectTo: 'quotes' },
    ],
  },
  { path: '**', redirectTo: 'quotes' },
];
