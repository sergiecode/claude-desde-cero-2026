import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ErrorMessageService } from '../errors/error-message.service';
import { errorInterceptor } from '../errors/error.interceptor';
import { PolicyDetail } from '../models/policy.model';
import { PolicyApi } from './policy-api.service';

describe('PolicyApi', () => {
  let api: PolicyApi;
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
    api = TestBed.inject(PolicyApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  function problem(status: number, code: string, detail: string): object {
    return { type: 'about:blank', title: 'Error', status, detail, code, errors: [] };
  }

  it('get: GET /api/policies/{id} devuelve el PolicyDetail tal como llega', async () => {
    const policy = { id: 4, policyNumber: 'POL-2026-000004' } as PolicyDetail;
    const result = api.get(4);

    const req = httpTesting.expectOne('/api/policies/4');
    expect(req.request.method).toBe('GET');
    req.flush(policy);
    await expect(result).resolves.toEqual(policy);
  });

  it('get: POLICY_NOT_FOUND lo maneja la pantalla, sin snackbar', async () => {
    const result = api.get(99);
    httpTesting
      .expectOne('/api/policies/99')
      .flush(problem(404, 'POLICY_NOT_FOUND', 'Póliza no encontrada'), {
        status: 404,
        statusText: 'Not Found',
      });

    await expect(result).rejects.toBeDefined();
    expect(shown).toEqual([]);
  });

  it('get: otros errores van al snackbar', async () => {
    const result = api.get(4);
    httpTesting
      .expectOne('/api/policies/4')
      .flush(problem(500, 'INTERNAL_ERROR', 'Error interno'), { status: 500, statusText: 'Error' });

    await expect(result).rejects.toBeDefined();
    expect(shown).toEqual(['Error interno']);
  });
});
