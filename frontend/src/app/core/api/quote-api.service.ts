import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { IssueRequest, PolicyDetail } from '../models/policy.model';
import { QuoteCalculation, QuoteDetail, QuoteRequest, QuoteSummary } from '../models/quote.model';

export const QUOTES_URL = '/api/quotes';
export const QUOTE_CALCULATE_URL = `${QUOTES_URL}/calculate`;

/** Errores del detalle que maneja la pantalla (frontend.md §5.5). */
export const QUOTE_DETAIL_ERROR_CODES = ['QUOTE_NOT_FOUND'] as const;

/** Errores de la emisión que maneja el diálogo "Emitir póliza" (frontend.md §5.6). */
export const ISSUE_ERROR_CODES = [
  'QUOTE_EXPIRED',
  'QUOTE_ALREADY_ISSUED',
  'INVALID_START_DATE',
] as const;

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

  /**
   * Guarda la cotización; el backend la recalcula con los mismos datos (api.md §8.3).
   * Igual que `calculate`, la pantalla maneja todos los errores (campo o snackbar).
   */
  create(request: QuoteRequest): Promise<QuoteDetail> {
    return firstValueFrom(
      this.http.post<QuoteDetail>(QUOTES_URL, request, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, ['*']),
      }),
    );
  }

  /** Cotizaciones del productor, la más reciente primero (api.md §8.4). Errores: errorInterceptor. */
  list(): Promise<QuoteSummary[]> {
    return firstValueFrom(this.http.get<QuoteSummary[]>(QUOTES_URL));
  }

  /**
   * Detalle de una cotización (api.md §8.5). QUOTE_NOT_FOUND lo muestra la pantalla
   * ("Cotización no encontrada"); el resto, el errorInterceptor.
   */
  get(id: number): Promise<QuoteDetail> {
    return firstValueFrom(
      this.http.get<QuoteDetail>(`${QUOTES_URL}/${id}`, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, QUOTE_DETAIL_ERROR_CODES),
      }),
    );
  }

  /**
   * Emite la póliza de una cotización (api.md §8.6) con la fecha de inicio en `yyyy-MM-dd`.
   * QUOTE_EXPIRED, QUOTE_ALREADY_ISSUED e INVALID_START_DATE los maneja el diálogo; el resto,
   * el errorInterceptor.
   */
  issue(quoteId: number, startDate: string): Promise<PolicyDetail> {
    const body: IssueRequest = { startDate };
    return firstValueFrom(
      this.http.post<PolicyDetail>(`${QUOTES_URL}/${quoteId}/issue`, body, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, ISSUE_ERROR_CODES),
      }),
    );
  }
}
