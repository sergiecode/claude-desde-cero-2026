import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from '../app.routes';
import { ShellComponent } from './shell.component';

describe('ShellComponent', () => {
  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('auth.token', 'jwt-token');
    sessionStorage.setItem('auth.expiresAt', new Date(Date.now() + 3_600_000).toISOString());
    sessionStorage.setItem(
      'auth.producer',
      JSON.stringify({ id: 1, firstName: 'Ana', lastName: 'Torres', email: 'ana.torres@aseguradora.test' }),
    );
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => sessionStorage.clear());

  it('muestra el título, el nombre del productor y Salir; la raíz va a /quotes', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/', ShellComponent);

    const toolbar = harness.fixture.nativeElement.querySelector('mat-toolbar') as HTMLElement;
    expect(TestBed.inject(Router).url).toBe('/quotes');
    expect(toolbar.textContent).toContain('Cotizador de seguros');
    expect(toolbar.textContent).toContain('Ana Torres');
    expect(toolbar.textContent).toContain('Salir');
  });

  it('Salir hace logout y navega a /login', async () => {
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/quotes', ShellComponent);

    const logoutButton = Array.from(
      (harness.fixture.nativeElement as HTMLElement).querySelectorAll('mat-toolbar button'),
    ).find((b) => b.textContent?.trim() === 'Salir') as HTMLButtonElement;
    logoutButton.click();
    await harness.fixture.whenStable();

    expect(sessionStorage.getItem('auth.token')).toBeNull();
    expect(TestBed.inject(Router).url).toBe('/login');
  });
});
