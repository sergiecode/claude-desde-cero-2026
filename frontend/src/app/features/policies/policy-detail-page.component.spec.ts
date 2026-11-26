import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { HttpErrorResponse } from '@angular/common/http';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { PolicyApi } from '../../core/api/policy-api.service';
import { PolicyDetail } from '../../core/models/policy.model';
import { PolicyDetailPageComponent } from './policy-detail-page.component';

registerLocaleData(localeEsAr);

const POLICY: PolicyDetail = {
  id: 4,
  policyNumber: 'POL-2026-000004',
  status: 'ACTIVE',
  issuedAt: '2026-11-19T10:05:00-03:00',
  startDate: '2026-11-20',
  endDate: '2027-11-20',
  quoteId: 1,
  quoteNumber: 'COT-2026-000001',
  client: {
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
  },
  vehicle: {
    vehicleType: 'AUTO',
    manufacturingYear: 2018,
    brandName: 'Volkswagen',
    modelName: 'Gol',
    versionName: 'Base',
    patent: 'ABC123',
    usageType: 'PARTICULAR',
  },
  planName: 'Terceros completo',
  insuredAmount: 10800000.0,
  premium: 46332.0,
  taxes: 9729.72,
  totalPrice: 56061.72,
  currency: 'ARS',
};

describe('PolicyDetailPageComponent', () => {
  let fixture: ComponentFixture<PolicyDetailPageComponent>;
  let root: HTMLElement;
  let get: ReturnType<typeof vi.fn>;

  async function create(id: string, response: () => Promise<PolicyDetail>): Promise<void> {
    get = vi.fn(response);
    const paramMap = convertToParamMap({ id });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: PolicyApi, useValue: { get } },
        { provide: ActivatedRoute, useValue: { paramMap: of(paramMap), snapshot: { paramMap } } },
        { provide: LOCALE_ID, useValue: 'es-AR' },
      ],
    });
    fixture = TestBed.createComponent(PolicyDetailPageComponent);
    root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function text(selector: string): string | undefined {
    return root.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();
  }

  it('encabezado: número, estado y "Vigencia: dd/MM/yyyy al dd/MM/yyyy" tal como llegan', async () => {
    await create('4', async () => POLICY);

    expect(get).toHaveBeenCalledExactlyOnceWith(4);
    expect(text('[data-field="policyNumber"]')).toBe('POL-2026-000004');
    expect(text('app-status-chip')).toBe('Activa');
    expect(text('[data-field="validity"]')).toBe('Vigencia: 20/11/2026 al 20/11/2027');
  });

  it('enlace a la cotización de origen', async () => {
    await create('4', async () => POLICY);

    expect(text('[data-field="quote"]')).toBe('Cotización de origen: COT-2026-000001');
    const link = root.querySelector<HTMLAnchorElement>('[data-field="quote"] a')!;
    expect(link.getAttribute('href')).toBe('/quotes/1');
  });

  it('muestra cliente, vehículo, plan e importes sin calcular nada, en solo lectura', async () => {
    await create('4', async () => POLICY);

    const client = text('[data-block="client"]')!;
    expect(client).toContain('Lucía');
    expect(client).toContain('Fernández');
    expect(client).toContain('22 años');

    const vehicle = text('[data-block="vehicle"]')!;
    expect(vehicle).toContain('Auto');
    expect(vehicle).toContain('Volkswagen');
    expect(vehicle).toContain('Gol');
    expect(vehicle).toContain('Particular');
    expect(vehicle).toContain('ABC123');

    expect(text('[data-field="planName"]')).toBe('Terceros completo');
    expect(text('[data-field="insuredAmount"]')).toBe('$ 10.800.000,00');
    expect(text('[data-field="premium"]')).toBe('$ 46.332,00');
    expect(text('[data-field="taxes"]')).toBe('$ 9.729,72');
    expect(text('[data-field="totalPrice"]')).toBe('$ 56.061,72');

    expect(root.querySelector('input, mat-select, textarea, button')).toBeNull();
  });

  it('póliza vencida: muestra el estado "Vencida"', async () => {
    await create('4', async () => ({ ...POLICY, status: 'EXPIRED' }));

    expect(text('app-status-chip')).toBe('Vencida');
  });

  it('POLICY_NOT_FOUND: muestra "Póliza no encontrada"', async () => {
    await create('99', () =>
      Promise.reject(
        new HttpErrorResponse({
          status: 404,
          statusText: 'Not Found',
          error: {
            type: 'about:blank',
            title: 'Not Found',
            status: 404,
            detail: 'Póliza no encontrada',
            code: 'POLICY_NOT_FOUND',
            errors: [],
          },
        }),
      ),
    );

    expect(get).toHaveBeenCalledExactlyOnceWith(99);
    expect(text('[data-state="not-found"] h1')).toBe('Póliza no encontrada');
    expect(root.querySelector('[data-block]')).toBeNull();
  });

  it('un id que no es numérico se muestra como "Póliza no encontrada" sin llamar al backend', async () => {
    await create('abc', async () => POLICY);

    expect(get).not.toHaveBeenCalled();
    expect(text('[data-state="not-found"] h1')).toBe('Póliza no encontrada');
  });

  it('otro error (lo muestra el errorInterceptor): no muestra "Póliza no encontrada"', async () => {
    await create('4', () =>
      Promise.reject(new HttpErrorResponse({ status: 500, statusText: 'Error', error: {} })),
    );

    expect(root.querySelector('[data-state="not-found"]')).toBeNull();
    expect(root.querySelector('[data-block]')).toBeNull();
  });
});
