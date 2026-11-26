import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { InsuredValue } from '../models/insured-value.model';

export const INSURED_VALUES_URL = '/api/insured-values';

/** Errores que muestra la pantalla en el campo "Suma asegurada" (frontend.md §5.4, Bloque 3). */
export const INSURED_VALUE_ERROR_CODES = ['VALUATION_NOT_FOUND'] as const;

/** Servicio HTTP de sumas aseguradas (api.md §7). */
@Injectable({ providedIn: 'root' })
export class InsuredValueApi {
  private readonly http = inject(HttpClient);

  /**
   * Rango y sugerido de la suma asegurada para una versión y un año (S2, S3).
   * VALUATION_NOT_FOUND (S6) lo maneja el componente: el errorInterceptor no lo muestra.
   */
  get(versionId: number, year: number): Promise<InsuredValue> {
    return firstValueFrom(
      this.http.get<InsuredValue>(INSURED_VALUES_URL, {
        params: new HttpParams().set('versionId', versionId).set('year', year),
        context: new HttpContext().set(HANDLED_ERROR_CODES, INSURED_VALUE_ERROR_CODES),
      }),
    );
  }
}
