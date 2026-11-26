import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HarnessLoader } from '@angular/cdk/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatInputHarness } from '@angular/material/input/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { provideRouter } from '@angular/router';

import { CatalogApi } from '../../core/api/catalog-api.service';
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

/**
 * CatalogApi simulado: responde al instante para que la cascada no deje pedidos HTTP abiertos
 * (los recursos pendientes no dejan estabilizar el fixture). Las URLs y los query params
 * se prueban en catalog-api.service.spec.ts.
 */
function catalogStub() {
  return {
    vehicleTypes: vi.fn(async () => ['AUTO', 'MOTO', 'CAMIONETA', 'CAMION']),
    years: vi.fn(async () => [2026, 2025, 2020]),
    brands: vi.fn(async () => [
      { id: 3, name: 'Fiat' },
      { id: 2, name: 'Volkswagen' },
    ]),
    models: vi.fn(async () => [
      { id: 3, name: 'Gol' },
      { id: 4, name: 'Polo' },
    ]),
    versions: vi.fn(async () => [
      { id: 5, name: 'Base' },
      { id: 6, name: 'Full' },
    ]),
    usageTypes: vi.fn(async (vehicleType: string) =>
      vehicleType === 'CAMION'
        ? ['COMERCIAL', 'TRANSPORTE_CARGA']
        : ['PARTICULAR', 'TAXI_REMIS', 'DELIVERY'],
    ),
  };
}

type VehicleField =
  | 'vehicleType'
  | 'manufacturingYear'
  | 'brandId'
  | 'modelId'
  | 'versionId'
  | 'usageType';

describe('QuoteNewPageComponent', () => {
  let fixture: ComponentFixture<QuoteNewPageComponent>;
  let loader: HarnessLoader;
  let root: HTMLElement;
  let httpTesting: HttpTestingController;
  let shown: string[];
  let catalog: ReturnType<typeof catalogStub>;

  beforeEach(async () => {
    shown = [];
    catalog = catalogStub();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
        { provide: CatalogApi, useValue: catalog },
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

  function vehicleSelect(field: VehicleField): Promise<MatSelectHarness> {
    return loader.getHarness(
      MatSelectHarness.with({ selector: `[data-field="${field}"] mat-select` }),
    );
  }

  function patentInput(): Promise<MatInputHarness> {
    return loader.getHarness(MatInputHarness.with({ selector: '[data-field="patent"] input' }));
  }

  async function choose(field: VehicleField, text: string): Promise<void> {
    const select = await vehicleSelect(field);
    await select.open();
    await select.clickOptions({ text });
    await fixture.whenStable();
  }

  async function disabledFields(): Promise<Record<string, boolean>> {
    const fields: VehicleField[] = [
      'vehicleType',
      'manufacturingYear',
      'brandId',
      'modelId',
      'versionId',
      'usageType',
    ];
    const result: Record<string, boolean> = {};
    for (const field of fields) {
      result[field] = await (await vehicleSelect(field)).isDisabled();
    }
    result['patent'] = await (await patentInput()).isDisabled();
    return result;
  }

  async function findClient(): Promise<void> {
    await enterDocument('DNI', '30123456');
    await search();
    await respond(PERSON);
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

  describe('Bloque 2 — Vehículo', () => {
    it('AC-FE-01 sin cliente, todos los campos del vehículo están deshabilitados', async () => {
      expect(await disabledFields()).toEqual({
        vehicleType: true,
        manufacturingYear: true,
        brandId: true,
        modelId: true,
        versionId: true,
        usageType: true,
        patent: true,
      });
    });

    it('carga los tipos de vehículo al iniciar y los muestra con las etiquetas en español', async () => {
      await findClient();
      const select = await vehicleSelect('vehicleType');
      await select.open();
      const texts = await Promise.all((await select.getOptions()).map((o) => o.getText()));
      expect(texts).toEqual(['Auto', 'Moto', 'Camioneta', 'Camión']);
      await select.close();
    });

    it('con cliente, cada select está deshabilitado hasta que el anterior tenga valor', async () => {
      await findClient();
      expect(await disabledFields()).toEqual({
        vehicleType: false,
        manufacturingYear: true,
        brandId: true,
        modelId: true,
        versionId: true,
        usageType: true,
        patent: false,
      });

      await choose('vehicleType', 'Auto');
      expect(await disabledFields()).toMatchObject({
        manufacturingYear: false,
        brandId: true,
        modelId: true,
        versionId: true,
        usageType: false,
      });

      await choose('manufacturingYear', '2020');
      expect(await disabledFields()).toMatchObject({ brandId: false, modelId: true, versionId: true });

      await choose('brandId', 'Volkswagen');
      expect(await disabledFields()).toMatchObject({ modelId: false, versionId: true });

      await choose('modelId', 'Gol');
      expect(await disabledFields()).toMatchObject({ versionId: false });
    });

    it('carga en cascada años, marcas, modelos y versiones con los parámetros correctos', async () => {
      await findClient();
      expect(catalog.vehicleTypes).toHaveBeenCalledTimes(1);
      // Sin tipo elegido no se pide ningún catálogo dependiente.
      for (const fn of [catalog.years, catalog.brands, catalog.models, catalog.versions]) {
        expect(fn).not.toHaveBeenCalled();
      }

      await choose('vehicleType', 'Auto');
      expect(catalog.years).toHaveBeenCalledExactlyOnceWith('AUTO');
      expect(catalog.brands).not.toHaveBeenCalled();

      await choose('manufacturingYear', '2020');
      expect(catalog.brands).toHaveBeenCalledExactlyOnceWith('AUTO', 2020);
      expect(catalog.models).not.toHaveBeenCalled();

      await choose('brandId', 'Volkswagen');
      expect(catalog.models).toHaveBeenCalledExactlyOnceWith('AUTO', 2020, 2);
      expect(catalog.versions).not.toHaveBeenCalled();

      await choose('modelId', 'Gol');
      expect(catalog.versions).toHaveBeenCalledExactlyOnceWith(3);

      await choose('versionId', 'Full');
      expect(await (await vehicleSelect('versionId')).getValueText()).toBe('Full');
    });

    it('pide los tipos de uso al elegir el tipo y los vuelve a pedir cuando el tipo cambia', async () => {
      await findClient();
      expect(catalog.usageTypes).not.toHaveBeenCalled();

      await choose('vehicleType', 'Auto');
      expect(catalog.usageTypes).toHaveBeenCalledExactlyOnceWith('AUTO');

      const usage = await vehicleSelect('usageType');
      await usage.open();
      const texts = await Promise.all((await usage.getOptions()).map((o) => o.getText()));
      expect(texts).toEqual(['Particular', 'Taxi / remís', 'Delivery']);
      await usage.close();

      await choose('vehicleType', 'Camión');
      expect(catalog.usageTypes).toHaveBeenCalledTimes(2);
      expect(catalog.usageTypes).toHaveBeenLastCalledWith('CAMION');
      await usage.open();
      const camion = await Promise.all((await usage.getOptions()).map((o) => o.getText()));
      expect(camion).toEqual(['Comercial', 'Transporte de carga']);
      await usage.close();
    });

    it.each([
      ['AB1234', 'La patente debe tener el formato ABC123'],
      ['ABC12', 'La patente debe tener el formato ABC123'],
      ['', 'Completá este campo'],
    ])('patente "%s": muestra "%s"', async (value, message) => {
      await findClient();
      const input = await patentInput();
      await input.setValue(value);
      await input.blur();
      await fixture.whenStable();
      const error = block('vehicle').querySelector('[data-field="patent"] mat-error');
      expect(error?.textContent?.trim()).toBe(message);
    });

    it('patente ABC123 es válida', async () => {
      await findClient();
      const input = await patentInput();
      await input.setValue('ABC123');
      await input.blur();
      await fixture.whenStable();
      expect(block('vehicle').querySelector('[data-field="patent"] mat-error')).toBeNull();
    });
  });

  describe('Reglas condicionales del vehículo', () => {
    /** Valores que muestran los selects y la patente. */
    async function vehicleValues(): Promise<Record<string, string>> {
      const fields: VehicleField[] = [
        'vehicleType',
        'manufacturingYear',
        'brandId',
        'modelId',
        'versionId',
        'usageType',
      ];
      const result: Record<string, string> = {};
      for (const field of fields) {
        result[field] = await (await vehicleSelect(field)).getValueText();
      }
      result['patent'] = await (await patentInput()).getValue();
      return result;
    }

    function formModel() {
      return fixture.componentInstance['model']();
    }

    async function fillVehicle(): Promise<void> {
      await findClient();
      await choose('vehicleType', 'Auto');
      await choose('manufacturingYear', '2020');
      await choose('brandId', 'Volkswagen');
      await choose('modelId', 'Gol');
      await choose('versionId', 'Full');
      await choose('usageType', 'Particular');
      await (await patentInput()).setValue('ABC123');
      await fixture.whenStable();
      expect(await vehicleValues()).toEqual({
        vehicleType: 'Auto',
        manufacturingYear: '2020',
        brandId: 'Volkswagen',
        modelId: 'Gol',
        versionId: 'Full',
        usageType: 'Particular',
        patent: 'ABC123',
      });
    }

    it('AC-FE-02 vacía año, marca, modelo, versión y tipo de uso al cambiar el tipo, sin vaciar la patente', async () => {
      await fillVehicle();
      await choose('vehicleType', 'Moto');

      expect(await vehicleValues()).toEqual({
        vehicleType: 'Moto',
        manufacturingYear: '',
        brandId: '',
        modelId: '',
        versionId: '',
        usageType: '',
        patent: 'ABC123',
      });
      expect(formModel()).toMatchObject({
        vehicleType: 'MOTO',
        manufacturingYear: '',
        brandId: '',
        modelId: '',
        versionId: '',
        usageType: '',
        patent: 'ABC123',
      });
    });

    it('AC-FE-02 vacía marca, modelo y versión al cambiar el año', async () => {
      await fillVehicle();
      await choose('manufacturingYear', '2025');

      expect(await vehicleValues()).toEqual({
        vehicleType: 'Auto',
        manufacturingYear: '2025',
        brandId: '',
        modelId: '',
        versionId: '',
        usageType: 'Particular',
        patent: 'ABC123',
      });
      expect(formModel()).toMatchObject({ manufacturingYear: 2025, brandId: '', modelId: '', versionId: '' });
    });

    it('AC-FE-02 vacía modelo y versión al cambiar la marca', async () => {
      await fillVehicle();
      await choose('brandId', 'Fiat');

      expect(await vehicleValues()).toEqual({
        vehicleType: 'Auto',
        manufacturingYear: '2020',
        brandId: 'Fiat',
        modelId: '',
        versionId: '',
        usageType: 'Particular',
        patent: 'ABC123',
      });
      expect(formModel()).toMatchObject({ brandId: 3, modelId: '', versionId: '' });
    });

    it('AC-FE-02 vacía la versión al cambiar el modelo', async () => {
      await fillVehicle();
      await choose('modelId', 'Polo');

      expect(await vehicleValues()).toEqual({
        vehicleType: 'Auto',
        manufacturingYear: '2020',
        brandId: 'Volkswagen',
        modelId: 'Polo',
        versionId: '',
        usageType: 'Particular',
        patent: 'ABC123',
      });
      expect(formModel()).toMatchObject({ modelId: 4, versionId: '' });
    });

    it('AC-FE-02 elegir el mismo valor no vacía los campos dependientes', async () => {
      await fillVehicle();
      await choose('brandId', 'Volkswagen');

      expect(await vehicleValues()).toMatchObject({ modelId: 'Gol', versionId: 'Full' });
    });

    it('AC-FE-08 la patente escrita "abc123" se convierte a mayúsculas mientras se escribe', async () => {
      await findClient();
      const input = await patentInput();
      await (await input.host()).sendKeys('abc123');
      await fixture.whenStable();

      const element = root.querySelector<HTMLInputElement>('[data-field="patent"] input')!;
      expect(element.value).toBe('ABC123');
      expect(formModel()['patent']).toBe('ABC123');

      await input.blur();
      await fixture.whenStable();
      expect(block('vehicle').querySelector('[data-field="patent"] mat-error')).toBeNull();
    });

    it('AC-FE-08 la patente se ve en mayúsculas desde la primera letra', async () => {
      await findClient();
      const input = await patentInput();
      await (await input.host()).sendKeys('a');
      await fixture.whenStable();
      expect(await input.getValue()).toBe('A');
      expect(formModel()['patent']).toBe('A');
    });
  });
});
