import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LoginResponse } from '../models/auth.model';
import { AuthService } from './auth.service';

const RESPONSE: LoginResponse = {
  token: 'jwt-token',
  expiresAt: '2999-01-01T18:00:00-03:00',
  producer: { id: 1, firstName: 'Ana', lastName: 'Torres', email: 'ana.torres@aseguradora.test' },
};

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    sessionStorage.clear();
  });

  it('login guarda token, vencimiento y productor en sessionStorage y expone el productor', async () => {
    const result = service.login({ email: 'ana.torres@aseguradora.test', password: 'Demo2026!' });

    const req = httpTesting.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana.torres@aseguradora.test', password: 'Demo2026!' });
    req.flush(RESPONSE);
    await result;

    expect(sessionStorage.getItem('auth.token')).toBe('jwt-token');
    expect(sessionStorage.getItem('auth.expiresAt')).toBe(RESPONSE.expiresAt);
    expect(service.producer()).toEqual(RESPONSE.producer);
    expect(service.producerName()).toBe('Ana Torres');
    expect(service.isAuthenticated()).toBe(true);
  });

  it('logout borra la sesión y el productor', async () => {
    const result = service.login({ email: 'ana.torres@aseguradora.test', password: 'Demo2026!' });
    httpTesting.expectOne('/api/auth/login').flush(RESPONSE);
    await result;

    service.logout();

    expect(sessionStorage.getItem('auth.token')).toBeNull();
    expect(service.producer()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
