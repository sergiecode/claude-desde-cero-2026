import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from '../../app.routes';
import { authInterceptor } from '../../core/auth/auth.interceptor';
import { ErrorMessageService } from '../../core/errors/error-message.service';
import { errorInterceptor } from '../../core/errors/error.interceptor';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  let shown: string[];

  beforeEach(() => {
    sessionStorage.clear();
    shown = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
      ],
    });
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    sessionStorage.clear();
  });

  async function openLogin(url = '/login'): Promise<{ harness: RouterTestingHarness; root: HTMLElement }> {
    const harness = await RouterTestingHarness.create();
    const component = await harness.navigateByUrl(url, LoginPageComponent);
    expect(component).toBeInstanceOf(LoginPageComponent);
    return { harness, root: harness.routeNativeElement as HTMLElement };
  }

  function submitButton(root: HTMLElement): HTMLButtonElement {
    const button = Array.from(root.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Ingresar',
    );
    expect(button).toBeDefined();
    return button as HTMLButtonElement;
  }

  async function fillForm(
    harness: RouterTestingHarness,
    root: HTMLElement,
    emailValue: string,
    passwordValue: string,
  ): Promise<void> {
    const emailInput = root.querySelector<HTMLInputElement>('input[type="email"]')!;
    const passwordInput = root.querySelector<HTMLInputElement>('input[type="password"]')!;
    emailInput.value = emailValue;
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = passwordValue;
    passwordInput.dispatchEvent(new Event('input'));
    await harness.fixture.whenStable();
  }

  it('AC-SETUP-01 la ruta /login muestra la pantalla de login con Email, Contraseña e Ingresar', async () => {
    const { root } = await openLogin();

    const labels = Array.from(root.querySelectorAll('mat-label')).map((l) => l.textContent?.trim());
    expect(labels).toEqual(['Email', 'Contraseña']);
    expect(root.querySelector('input[type="email"]')).not.toBeNull();
    expect(root.querySelector('input[type="password"]')).not.toBeNull();
    submitButton(root);
  });

  it('AC-SETUP-01 la ruta raíz sin sesión termina en la pantalla de login', async () => {
    const { root } = await openLogin('/');
    expect(submitButton(root)).toBeTruthy();
  });

  it('AC-SETUP-01 Ingresar está deshabilitado con el formulario vacío', async () => {
    const { root } = await openLogin();
    expect(submitButton(root).disabled).toBe(true);
  });

  it('Ingresar sigue deshabilitado con un email con formato inválido', async () => {
    const { harness, root } = await openLogin();
    await fillForm(harness, root, 'no-es-email', 'secreto');
    expect(submitButton(root).disabled).toBe(true);
  });

  it('habilita Ingresar con email y contraseña válidos', async () => {
    const { harness, root } = await openLogin();
    await fillForm(harness, root, 'productor@demo.com', 'secreto');
    expect(submitButton(root).disabled).toBe(false);
  });

  it('login correcto: deshabilita Ingresar durante la llamada y navega a /quotes', async () => {
    const { harness, root } = await openLogin();
    await fillForm(harness, root, 'ana.torres@aseguradora.test', 'Demo2026!');

    submitButton(root).click();
    harness.detectChanges();
    expect(submitButton(root).disabled).toBe(true);

    const req = TestBed.inject(HttpTestingController).expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'ana.torres@aseguradora.test', password: 'Demo2026!' });
    req.flush({
      token: 'jwt-token',
      expiresAt: new Date(Date.now() + 3_600_000).toISOString(),
      producer: { id: 1, firstName: 'Ana', lastName: 'Torres', email: 'ana.torres@aseguradora.test' },
    });
    await vi.waitFor(() => expect(TestBed.inject(Router).url).toBe('/quotes'));
    expect(sessionStorage.getItem('auth.token')).toBe('jwt-token');

    // "Mis cotizaciones" pide el listado al abrirse.
    const httpTesting = TestBed.inject(HttpTestingController);
    const list = await vi.waitFor(() => httpTesting.expectOne('/api/quotes'));
    list.flush([]);
  });

  it('INVALID_CREDENTIALS: muestra el mensaje debajo del formulario y no usa el snackbar', async () => {
    const { harness, root } = await openLogin();
    await fillForm(harness, root, 'ana.torres@aseguradora.test', 'incorrecta');

    submitButton(root).click();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/auth/login')
      .flush(
        {
          type: 'about:blank',
          title: 'Unauthorized',
          status: 401,
          detail: 'Usuario o contraseña incorrectos',
          code: 'INVALID_CREDENTIALS',
          errors: [],
        },
        { status: 401, statusText: 'Unauthorized' },
      );
    await harness.fixture.whenStable();

    expect(root.querySelector('.login__error')?.textContent?.trim()).toBe(
      'Usuario o contraseña incorrectos',
    );
    expect(shown).toEqual([]);
    expect(TestBed.inject(Router).url).toBe('/login');
    expect(submitButton(root).disabled).toBe(false);
  });
});
