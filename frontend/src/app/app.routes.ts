import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

// Rutas de frontend.md §3. Las pantallas de cotización y póliza se agregan en sus etapas.
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
      { path: '', pathMatch: 'full', redirectTo: 'quotes' },
    ],
  },
  { path: '**', redirectTo: 'quotes' },
];
