import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { authGuard } from './auth.guard';

@Component({ template: 'login' })
class FakeLogin {}

@Component({ template: 'protegida' })
class FakeProtected {}

describe('authGuard', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([
          { path: 'login', component: FakeLogin },
          { path: 'quotes', component: FakeProtected, canActivate: [authGuard] },
        ]),
      ],
    });
  });

  afterEach(() => sessionStorage.clear());

  it('sin token redirige a /login', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/quotes');

    expect(TestBed.inject(Router).url).toBe('/login');
  });

  it('con token vencido redirige a /login y borra la sesión', async () => {
    sessionStorage.setItem('auth.token', 'abc123');
    sessionStorage.setItem('auth.expiresAt', new Date(Date.now() - 60_000).toISOString());

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/quotes');

    expect(TestBed.inject(Router).url).toBe('/login');
    expect(sessionStorage.getItem('auth.token')).toBeNull();
  });

  it('con token vigente permite entrar', async () => {
    sessionStorage.setItem('auth.token', 'abc123');
    sessionStorage.setItem('auth.expiresAt', new Date(Date.now() + 60_000).toISOString());

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/quotes', FakeProtected);

    expect(TestBed.inject(Router).url).toBe('/quotes');
  });
});
