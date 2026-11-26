import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { InsurancePlan } from '../models/plan.model';

export const INSURANCE_PLANS_URL = '/api/insurance-plans';

/** Servicio HTTP de planes (api.md §6). Los errores los muestra el errorInterceptor. */
@Injectable({ providedIn: 'root' })
export class PlanApi {
  private readonly http = inject(HttpClient);

  /** Los planes, ordenados por el backend. Son los mismos para todos los tipos de vehículo (P2). */
  list(): Promise<InsurancePlan[]> {
    return firstValueFrom(this.http.get<InsurancePlan[]>(INSURANCE_PLANS_URL));
  }
}
