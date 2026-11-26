import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Placeholder de "Mis cotizaciones" (frontend.md §5.3).
 * La pantalla real (tabla y "Nueva cotización") se implementa en la etapa 10.
 */
@Component({
  selector: 'app-quote-list-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<h1>Mis cotizaciones</h1>`,
})
export class QuoteListPageComponent {}
