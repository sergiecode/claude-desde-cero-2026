import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('agrega el header Authorization con el token en las llamadas a /api', () => {
    sessionStorage.setItem('auth.token', 'abc123');

    http.get('/api/quotes').subscribe();

    const req = httpTesting.expectOne('/api/quotes');
    expect(req.request.headers.get('Authorization')).toBe('Bearer abc123');
    req.flush([]);
  });

  it('no agrega el header en el login', () => {
    sessionStorage.setItem('auth.token', 'abc123');

    http.post('/api/auth/login', { email: 'a@b.test', password: 'x' }).subscribe();

    const req = httpTesting.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('no agrega el header si no hay token', () => {
    http.get('/api/auth/me').subscribe();

    const req = httpTesting.expectOne('/api/auth/me');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('no agrega el header en llamadas fuera de /api', () => {
    sessionStorage.setItem('auth.token', 'abc123');

    http.get('/assets/data.json').subscribe();

    const req = httpTesting.expectOne('/assets/data.json');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });
});
