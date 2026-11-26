import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { routes } from '../../app.routes';
import { LoginPageComponent } from './login-page.component';

describe('LoginPageComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
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

  it('AC-SETUP-01 la ruta /login muestra la pantalla de login con Email, Contraseña e Ingresar', async () => {
    const { root } = await openLogin();

    const labels = Array.from(root.querySelectorAll('mat-label')).map((l) => l.textContent?.trim());
    expect(labels).toEqual(['Email', 'Contraseña']);
    expect(root.querySelector('input[type="email"]')).not.toBeNull();
    expect(root.querySelector('input[type="password"]')).not.toBeNull();
    submitButton(root);
  });

  it('AC-SETUP-01 la ruta raíz redirige a la pantalla de login', async () => {
    const { root } = await openLogin('/');
    expect(submitButton(root)).toBeTruthy();
  });

  it('AC-SETUP-01 Ingresar está deshabilitado con el formulario vacío', async () => {
    const { root } = await openLogin();
    expect(submitButton(root).disabled).toBe(true);
  });

  it('habilita Ingresar con email y contraseña válidos', async () => {
    const { harness, root } = await openLogin();

    const emailInput = root.querySelector<HTMLInputElement>('input[type="email"]')!;
    const passwordInput = root.querySelector<HTMLInputElement>('input[type="password"]')!;
    emailInput.value = 'productor@demo.com';
    emailInput.dispatchEvent(new Event('input'));
    passwordInput.value = 'secreto';
    passwordInput.dispatchEvent(new Event('input'));
    await harness.fixture.whenStable();

    expect(submitButton(root).disabled).toBe(false);
  });
});
