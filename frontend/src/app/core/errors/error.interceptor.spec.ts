import { HttpClient, HttpContext, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { ProblemDetails } from '../models/problem-details.model';
import { ErrorMessageService } from './error-message.service';
import { CONNECTION_ERROR_MESSAGE, errorInterceptor } from './error.interceptor';
import { HANDLED_ERROR_CODES } from './handled-error-codes';

function problem(status: number, code: string, detail: string): ProblemDetails {
  return { type: 'about:blank', title: 'Error', status, code, detail, errors: [] };
}

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let shown: string[];
  let navigated: string[];

  beforeEach(() => {
    sessionStorage.clear();
    shown = [];
    navigated = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
      ],
    });
    const router = TestBed.inject(Router);
    vi.spyOn(router, 'navigateByUrl').mockImplementation(async (url) => {
      navigated.push(String(url));
      return true;
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('401 fuera del login: hace logout, redirige a /login y muestra el detail', () => {
    sessionStorage.setItem('auth.token', 'abc');
    let received = false;
    http.get('/api/quotes').subscribe({ error: () => (received = true) });

    httpTesting
      .expectOne('/api/quotes')
      .flush(problem(401, 'UNAUTHORIZED', 'La sesión expiró. Ingresá nuevamente'), {
        status: 401,
        statusText: 'Unauthorized',
      });

    expect(sessionStorage.getItem('auth.token')).toBeNull();
    expect(navigated).toEqual(['/login']);
    expect(shown).toEqual(['La sesión expiró. Ingresá nuevamente']);
    expect(received).toBe(true);
  });

  it('401 del login manejado por el componente: no redirige ni muestra snackbar', () => {
    let received = false;
    http
      .post('/api/auth/login', {}, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, ['INVALID_CREDENTIALS']),
      })
      .subscribe({ error: () => (received = true) });

    httpTesting
      .expectOne('/api/auth/login')
      .flush(problem(401, 'INVALID_CREDENTIALS', 'Usuario o contraseña incorrectos'), {
        status: 401,
        statusText: 'Unauthorized',
      });

    expect(navigated).toEqual([]);
    expect(shown).toEqual([]);
    expect(received).toBe(true);
  });

  it('sin respuesta del servidor muestra "No se pudo conectar con el servidor"', () => {
    http.get('/api/quotes').subscribe({ error: () => undefined });

    httpTesting.expectOne('/api/quotes').error(new ProgressEvent('error'), { status: 0 });

    expect(shown).toEqual([CONNECTION_ERROR_MESSAGE]);
  });

  it('error no manejado por el componente: muestra el detail en el snackbar', () => {
    http.get('/api/quotes/99').subscribe({ error: () => undefined });

    httpTesting
      .expectOne('/api/quotes/99')
      .flush(problem(404, 'QUOTE_NOT_FOUND', 'Cotización no encontrada'), {
        status: 404,
        statusText: 'Not Found',
      });

    expect(shown).toEqual(['Cotización no encontrada']);
  });

  it('error manejado por el componente: no muestra el snackbar y lo propaga', () => {
    let received: unknown;
    http
      .get('/api/quotes/99', {
        context: new HttpContext().set(HANDLED_ERROR_CODES, ['QUOTE_NOT_FOUND']),
      })
      .subscribe({ error: (e) => (received = e) });

    httpTesting
      .expectOne('/api/quotes/99')
      .flush(problem(404, 'QUOTE_NOT_FOUND', 'Cotización no encontrada'), {
        status: 404,
        statusText: 'Not Found',
      });

    expect(shown).toEqual([]);
    expect(received).toBeDefined();
  });
});
