import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { QuoteCalculation, QuoteRequest } from '../models/quote.model';

export const QUOTE_CALCULATE_URL = '/api/quotes/calculate';

/** Servicio HTTP de cotizaciones (api.md §8). */
@Injectable({ providedIn: 'root' })
export class QuoteApi {
  private readonly http = inject(HttpClient);

  /**
   * Calcula el precio sin guardar nada (api.md §8.2).
   * La pantalla maneja todos los errores de esta llamada (`'*'`): los que traen `errors` los
   * muestra en los campos y el resto en el snackbar (frontend.md §5.4). Sin conexión y 401 los
   * sigue resolviendo el errorInterceptor.
   */
  calculate(request: QuoteRequest): Promise<QuoteCalculation> {
    return firstValueFrom(
      this.http.post<QuoteCalculation>(QUOTE_CALCULATE_URL, request, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, ['*']),
      }),
    );
  }
}
