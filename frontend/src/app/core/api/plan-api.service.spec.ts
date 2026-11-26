import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { InsurancePlan } from '../models/plan.model';
import { INSURANCE_PLANS_URL, PlanApi } from './plan-api.service';

const PLANS: InsurancePlan[] = [
  { id: 1, code: 'RC', name: 'RC — Responsabilidad civil', description: 'Daños a terceros' },
  {
    id: 2,
    code: 'TERCEROS_COMPLETO',
    name: 'Terceros completo',
    description: 'RC + robo e incendio totales y parciales + destrucción total + granizo + cristales',
  },
  {
    id: 3,
    code: 'TODO_RIESGO',
    name: 'Todo riesgo',
    description: 'Terceros completo + daños parciales al propio vehículo',
  },
];

describe('PlanApi', () => {
  let api: PlanApi;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(PlanApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('list: GET /api/insurance-plans sin parámetros y devuelve los planes en el orden recibido', async () => {
    const result = api.list();
    const req = httpTesting.expectOne((r) => r.url === INSURANCE_PLANS_URL);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush(PLANS);
    await expect(result).resolves.toEqual(PLANS);
  });
});
