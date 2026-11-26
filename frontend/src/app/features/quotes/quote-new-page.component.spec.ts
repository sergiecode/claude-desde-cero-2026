import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { provideRouter } from '@angular/router';

import { CLIENT_SEARCH_URL } from '../../core/api/client-api.service';
import { ErrorMessageService } from '../../core/errors/error-message.service';
import { errorInterceptor } from '../../core/errors/error.interceptor';
import { Client } from '../../core/models/client.model';
import { QuoteNewPageComponent } from './quote-new-page.component';

const PERSON: Client = {
  id: 1,
  documentType: 'DNI',
  documentNumber: '30123456',
  firstName: 'Lucía',
  lastName: 'Fernández',
  businessName: null,
  birthDate: '2004-03-15',
  age: 22,
  email: 'lucia.fernandez@mail.test',
  phone: '11 5555-0101',
  address: 'Av. Rivadavia 5000',
  postalCode: 'C1406',
  city: 'CABA',
  province: 'CABA',
};

const COMPANY: Client = {
  ...PERSON,
  id: 2,
  documentType: 'CUIT',
  documentNumber: '30712345671',
  firstName: null,
  lastName: null,
  businessName: 'Transportes del Sur S.A.',
  birthDate: null,
  age: null,
  email: 'contacto@transportesdelsur.test',
};

describe('QuoteNewPageComponent', () => {
  let fixture: ComponentFixture<QuoteNewPageComponent>;
  let loader: HarnessLoader;
  let root: HTMLElement;
  let httpTesting: HttpTestingController;
  let shown: string[];

  beforeEach(async () => {
    shown = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
      ],
    });
    fixture = TestBed.createComponent(QuoteNewPageComponent);
    loader = TestbedHarnessEnvironment.loader(fixture);
    root = fixture.nativeElement as HTMLElement;
    httpTesting = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => httpTesting.verify());

  function block(name: 'client' | 'vehicle' | 'plan'): HTMLElement {
    return root.querySelector<HTMLElement>(`[data-block="${name}"]`)!;
  }

  function isDisabled(name: 'vehicle' | 'plan'): boolean {
    const card = block(name);
    return card.getAttribute('aria-disabled') === 'true' && card.hasAttribute('inert');
  }

  function searchButton(): HTMLButtonElement {
    return Array.from(root.querySelectorAll('button')).find(
      (b) => b.textContent?.trim() === 'Buscar',
    ) as HTMLButtonElement;
  }

  async function enterDocument(type: 'DNI' | 'CUIT', number: string): Promise<void> {
    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: type });
    const input = await loader.getHarness(MatInputHarness);
    await input.setValue(number);
    await fixture.whenStable();
  }

  async function search(): Promise<void> {
    searchButton().click();
    await fixture.whenStable();
  }

  async function respond(body: object, status = 200): Promise<void> {
    httpTesting
      .expectOne((r) => r.url === CLIENT_SEARCH_URL)
      .flush(body, { status, statusText: status === 200 ? 'OK' : 'Error' });
    await vi.waitFor(() =>
      status === 200
        ? expect(root.querySelector('.quote-new__client')).not.toBeNull()
        : expect(errorText()).toBeDefined(),
    );
    await fixture.whenStable();
  }

  function problem(status: number, code: string, detail: string): object {
    return { type: 'about:blank', title: 'Error', status, detail, code, errors: [] };
  }

  function errorText(): string | undefined {
    return block('client').querySelector('mat-error')?.textContent?.trim();
  }

  it('AC-FE-01 sin cliente encontrado, los bloques de vehículo y plan están deshabilitados', () => {
    const titles = Array.from(root.querySelectorAll('mat-card-title')).map((t) =>
      t.textContent?.trim(),
    );
    expect(titles).toEqual(['Cliente', 'Vehículo', 'Plan y suma asegurada']);
    expect(isDisabled('vehicle')).toBe(true);
    expect(isDisabled('plan')).toBe(true);
    expect(searchButton().disabled).toBe(true);
  });

  it('AC-FE-01 al encontrar el cliente muestra sus datos en solo lectura y habilita los bloques 2 y 3', async () => {
    await enterDocument('DNI', '30123456');
    expect(searchButton().disabled).toBe(false);

    await search();
    const req = httpTesting.expectOne((r) => r.url === CLIENT_SEARCH_URL);
    expect(req.request.params.get('documentType')).toBe('DNI');
    expect(req.request.params.get('documentNumber')).toBe('30123456');
    req.flush(PERSON);
    await vi.waitFor(() => expect(root.querySelector('.quote-new__client')).not.toBeNull());
    await fixture.whenStable();

    const data = root.querySelector('.quote-new__client')!.textContent!;
    for (const text of [
      'Lucía',
      'Fernández',
      '15/03/2004',
      '22 años',
      'lucia.fernandez@mail.test',
      '11 5555-0101',
      'Av. Rivadavia 5000',
      'CABA',
    ]) {
      expect(data).toContain(text);
    }
    expect(data).not.toContain('Razón social');
    expect(block('client').querySelectorAll('.quote-new__client input').length).toBe(0);
    expect(isDisabled('vehicle')).toBe(false);
    expect(isDisabled('plan')).toBe(false);
  });

  it('muestra la razón social de una empresa y acepta el CUIT con guiones', async () => {
    await enterDocument('CUIT', '30-71234567-1');
    await search();
    const req = httpTesting.expectOne((r) => r.url === CLIENT_SEARCH_URL);
    expect(req.request.params.get('documentNumber')).toBe('30-71234567-1');
    req.flush(COMPANY);
    await vi.waitFor(() => expect(root.querySelector('.quote-new__client')).not.toBeNull());

    const data = root.querySelector('.quote-new__client')!.textContent!;
    expect(data).toContain('Razón social');
    expect(data).toContain('Transportes del Sur S.A.');
    expect(data).not.toContain('Fecha de nacimiento');
    expect(isDisabled('vehicle')).toBe(false);
  });

  it.each([
    [404, 'CLIENT_NOT_FOUND', 'Cliente no encontrado'],
    [400, 'INVALID_DOCUMENT', 'El número de documento no es válido'],
    [422, 'CLIENT_UNDERAGE', 'El cliente debe ser mayor de 18 años'],
  ])(
    'AC-FE-01 %s %s: muestra el detail debajo del campo y deja los bloques 2 y 3 deshabilitados',
    async (status, code, detail) => {
      await enterDocument('DNI', '30123456');
      await search();
      await respond(problem(status, code, detail), status);

      expect(errorText()).toBe(detail);
      expect(shown).toEqual([]);
      expect(root.querySelector('.quote-new__client')).toBeNull();
      expect(isDisabled('vehicle')).toBe(true);
      expect(isDisabled('plan')).toBe(true);
    },
  );

  it('AC-FE-01 cambiar el número después de encontrar el cliente lo borra y vuelve a deshabilitar los bloques', async () => {
    await enterDocument('DNI', '30123456');
    await search();
    await respond(PERSON);
    expect(isDisabled('vehicle')).toBe(false);

    const input = await loader.getHarness(MatInputHarness);
    await input.setValue('30123457');
    await fixture.whenStable();

    expect(root.querySelector('.quote-new__client')).toBeNull();
    expect(isDisabled('vehicle')).toBe(true);
    expect(isDisabled('plan')).toBe(true);
  });

  it('AC-FE-01 cambiar el tipo de documento borra el cliente encontrado', async () => {
    await enterDocument('DNI', '30123456');
    await search();
    await respond(PERSON);

    const select = await loader.getHarness(MatSelectHarness);
    await select.open();
    await select.clickOptions({ text: 'CUIT' });
    await fixture.whenStable();

    expect(root.querySelector('.quote-new__client')).toBeNull();
    expect(isDisabled('vehicle')).toBe(true);
  });

  it('cambiar el número después de un error borra el mensaje y vuelve a habilitar Buscar', async () => {
    await enterDocument('DNI', '30123456');
    await search();
    await respond(problem(404, 'CLIENT_NOT_FOUND', 'Cliente no encontrado'), 404);
    expect(searchButton().disabled).toBe(true);

    const input = await loader.getHarness(MatInputHarness);
    await input.setValue('30123457');
    await fixture.whenStable();

    expect(errorText()).toBeUndefined();
    expect(searchButton().disabled).toBe(false);
  });

  it.each([
    ['DNI', '123456'],
    ['DNI', '123456789'],
    ['DNI', '12a45678'],
    ['CUIT', '3071234567'],
    ['CUIT', '30.71234567.1'],
  ] as const)('%s "%s" con formato inválido: Buscar deshabilitado y sin llamada', async (type, number) => {
    await enterDocument(type, number);
    expect(searchButton().disabled).toBe(true);
    const input = await loader.getHarness(MatInputHarness);
    await input.blur();
    await fixture.whenStable();
    expect(errorText()).toBe('El número de documento no es válido');
    httpTesting.expectNone((r) => r.url === CLIENT_SEARCH_URL);
  });

  it('DNI de 7 dígitos y CUIT sin guiones son válidos', async () => {
    await enterDocument('DNI', '1234567');
    expect(searchButton().disabled).toBe(false);
    await enterDocument('CUIT', '30712345671');
    expect(searchButton().disabled).toBe(false);
  });
});
