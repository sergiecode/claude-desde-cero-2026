import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ErrorMessageService } from '../errors/error-message.service';
import { errorInterceptor } from '../errors/error.interceptor';
import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { QuoteCalculation, QuoteRequest } from '../models/quote.model';
import { QUOTE_CALCULATE_URL, QuoteApi } from './quote-api.service';

const REQUEST: QuoteRequest = {
  clientId: 1,
  vehicleType: 'AUTO',
  manufacturingYear: 2018,
  brandId: 2,
  modelId: 3,
  versionId: 5,
  usageType: 'PARTICULAR',
  patent: 'ABC123',
  planId: 2,
  insuredAmount: 10800000,
};

const CALCULATION: QuoteCalculation = {
  insuredAmount: 10800000.0,
  planRate: 0.0025,
  basePremium: 27000.0,
  zone: { code: 'ZONA_1', name: 'CABA', factor: 1.2 },
  usageFactor: 1.0,
  vehicleAgeYears: 8,
  vehicleAgeFactor: 1.1,
  driverAge: 22,
  driverAgeFactor: 1.3,
  premium: 46332.0,
  taxRate: 0.21,
  taxes: 9729.72,
  totalPrice: 56061.72,
  currency: 'ARS',
};

describe('QuoteApi', () => {
  let api: QuoteApi;
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
    api = TestBed.inject(QuoteApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('calculate: POST /api/quotes/calculate con el QuoteRequest y devuelve el cálculo tal como llega', async () => {
    const result = api.calculate(REQUEST);

    const req = httpTesting.expectOne(QUOTE_CALCULATE_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(REQUEST);
    expect(req.request.context.get(HANDLED_ERROR_CODES)).toEqual(['*']);
    req.flush(CALCULATION);
    await expect(result).resolves.toEqual(CALCULATION);
  });

  it('los errores de negocio los maneja la pantalla: el interceptor no muestra snackbar', async () => {
    const result = api.calculate(REQUEST);
    httpTesting.expectOne(QUOTE_CALCULATE_URL).flush(
      {
        type: 'about:blank',
        title: 'Bad Request',
        status: 400,
        detail: 'La patente debe tener el formato ABC123',
        code: 'INVALID_PATENT',
        errors: [{ field: 'patent', message: 'La patente debe tener el formato ABC123' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );
    await expect(result).rejects.toBeDefined();
    expect(shown).toEqual([]);
  });
});
