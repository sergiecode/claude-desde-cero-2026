import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

/**
 * Placeholder de "Mis cotizaciones" (frontend.md §5.3).
 * La tabla se implementa en la etapa 10; el botón "Nueva cotización" ya navega a /quotes/new.
 */
@Component({
  selector: 'app-quote-list-page',
  imports: [MatButtonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h1>Mis cotizaciones</h1>
    <a matButton="filled" routerLink="/quotes/new">Nueva cotización</a>
  `,
})
export class QuoteListPageComponent {}
