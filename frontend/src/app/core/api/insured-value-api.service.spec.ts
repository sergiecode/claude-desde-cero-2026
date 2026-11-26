import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ErrorMessageService } from '../errors/error-message.service';
import { errorInterceptor } from '../errors/error.interceptor';
import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { InsuredValue } from '../models/insured-value.model';
import { INSURED_VALUES_URL, InsuredValueApi } from './insured-value-api.service';

const VALUE: InsuredValue = {
  minimumAmount: 8640000.0,
  suggestedAmount: 10800000.0,
  maximumAmount: 12960000.0,
  currency: 'ARS',
};

function problem(status: number, code: string, detail: string): object {
  return { type: 'about:blank', title: 'Error', status, code, detail, errors: [] };
}

describe('InsuredValueApi', () => {
  let api: InsuredValueApi;
  let httpTesting: HttpTestingController;
  let shown: string[];

  beforeEach(() => {
    shown = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
      ],
    });
    api = TestBed.inject(InsuredValueApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('get: GET /api/insured-values?versionId&year y devuelve el rango tal como llega', async () => {
    const result = api.get(5, 2018);

    const req = httpTesting.expectOne((r) => r.url === INSURED_VALUES_URL);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('versionId')).toBe('5');
    expect(req.request.params.get('year')).toBe('2018');
    expect(req.request.context.get(HANDLED_ERROR_CODES)).toEqual(['VALUATION_NOT_FOUND']);
    req.flush(VALUE);
    await expect(result).resolves.toEqual(VALUE);
  });

  it('S6 VALUATION_NOT_FOUND: lo maneja la pantalla, sin snackbar', async () => {
    const result = api.get(99, 2018);
    httpTesting
      .expectOne((r) => r.url === INSURED_VALUES_URL)
      .flush(problem(404, 'VALUATION_NOT_FOUND', 'No hay valuación disponible para este vehículo'), {
        status: 404,
        statusText: 'Not Found',
      });
    await expect(result).rejects.toBeDefined();
    expect(shown).toEqual([]);
  });

  it('INVALID_YEAR no lo maneja la pantalla: se muestra en el snackbar', async () => {
    const result = api.get(5, 2009);
    httpTesting
      .expectOne((r) => r.url === INSURED_VALUES_URL)
      .flush(problem(400, 'INVALID_YEAR', 'El año debe estar entre 2010 y 2026'), {
        status: 400,
        statusText: 'Bad Request',
      });
    await expect(result).rejects.toBeDefined();
    expect(shown).toEqual(['El año debe estar entre 2010 y 2026']);
  });
});
