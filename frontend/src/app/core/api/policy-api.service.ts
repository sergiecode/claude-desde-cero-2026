import { HttpClient, HttpContext } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { PolicyDetail } from '../models/policy.model';

export const POLICIES_URL = '/api/policies';

/** Errores del detalle de póliza que maneja la pantalla (frontend.md §5.8). */
export const POLICY_DETAIL_ERROR_CODES = ['POLICY_NOT_FOUND'] as const;

/** Servicio HTTP de pólizas (api.md §9). La emisión está en `QuoteApi.issue` (api.md §8.6). */
@Injectable({ providedIn: 'root' })
export class PolicyApi {
  private readonly http = inject(HttpClient);

  /**
   * Detalle de una póliza (api.md §9.1). POLICY_NOT_FOUND lo muestra la pantalla
   * ("Póliza no encontrada"); el resto, el errorInterceptor.
   */
  get(id: number): Promise<PolicyDetail> {
    return firstValueFrom(
      this.http.get<PolicyDetail>(`${POLICIES_URL}/${id}`, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, POLICY_DETAIL_ERROR_CODES),
      }),
    );
  }
}
