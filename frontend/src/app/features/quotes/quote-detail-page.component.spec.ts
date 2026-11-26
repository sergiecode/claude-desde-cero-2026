import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { HttpErrorResponse } from '@angular/common/http';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { QuoteApi } from '../../core/api/quote-api.service';
import { Client } from '../../core/models/client.model';
import { QuoteDetail } from '../../core/models/quote.model';
import { IssuePolicyDialogComponent } from './issue-policy-dialog.component';
import { QuoteDetailPageComponent } from './quote-detail-page.component';

registerLocaleData(localeEsAr);

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

const QUOTE: QuoteDetail = {
  id: 1,
  quoteNumber: 'COT-2026-000001',
  status: 'CALCULATED',
  createdAt: '2026-11-19T10:00:00-03:00',
  validUntil: '2026-12-04T10:00:00-03:00',
  client: PERSON,
  vehicle: {
    vehicleType: 'AUTO',
    manufacturingYear: 2018,
    brandName: 'Volkswagen',
    modelName: 'Gol',
    versionName: 'Base',
    patent: 'ABC123',
    usageType: 'TAXI_REMIS',
  },
  planName: 'Terceros completo',
  calculation: {
    insuredAmount: 10800000.0,
    planRate: 0.0025,
    basePremium: 27000.0,
    zone: { code: 'ZONA_1', name: 'CABA', factor: 1.2 },
    usageFactor: 1.0,
    vehicleAgeYears: 8,
    vehicleAgeFactor: 1.1,
    driverAge: 22,
    driverAgeFactor: 1.3,
    premium: 46332.0,
    taxRate: 0.21,
    taxes: 9729.72,
    totalPrice: 56061.72,
    currency: 'ARS',
  },
  policy: null,
};

function notFoundError(): HttpErrorResponse {
  return new HttpErrorResponse({
    status: 404,
    statusText: 'Not Found',
    error: {
      type: 'about:blank',
      title: 'Not Found',
      status: 404,
      detail: 'Cotización no encontrada',
      code: 'QUOTE_NOT_FOUND',
      errors: [],
    },
  });
}

describe('QuoteDetailPageComponent', () => {
  let fixture: ComponentFixture<QuoteDetailPageComponent>;
  let root: HTMLElement;
  let get: ReturnType<typeof vi.fn>;
  let dialog: { open: ReturnType<typeof vi.fn> };
  let dialogResult: 'issued' | 'reload' | undefined;

  async function create(id: string, response: () => Promise<QuoteDetail>): Promise<void> {
    get = vi.fn(response);
    dialogResult = undefined;
    dialog = { open: vi.fn(() => ({ afterClosed: () => of(dialogResult) })) };
    const paramMap = convertToParamMap({ id });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: QuoteApi, useValue: { get } },
        { provide: ActivatedRoute, useValue: { paramMap: of(paramMap), snapshot: { paramMap } } },
        { provide: LOCALE_ID, useValue: 'es-AR' },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    fixture = TestBed.createComponent(QuoteDetailPageComponent);
    root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function text(selector: string): string | undefined {
    return root.querySelector(selector)?.textContent?.replace(/\s+/g, ' ').trim();
  }

  it('muestra encabezado con número, estado, creación y vencimiento tal como llegan del backend', async () => {
    await create('1', async () => QUOTE);

    expect(get).toHaveBeenCalledExactlyOnceWith(1);
    expect(text('[data-field="quoteNumber"]')).toBe('COT-2026-000001');
    expect(text('app-status-chip')).toBe('Calculada');
    expect(text('[data-field="createdAt"]')).toBe('Creada el 19/11/2026 10:00');
    expect(text('[data-field="validUntil"]')).toBe('Vence el 04/12/2026 10:00');
    expect(root.querySelector('[data-field="policy"]')).toBeNull();
  });

  it('muestra cliente, vehículo, plan, suma asegurada y desglose en solo lectura', async () => {
    await create('1', async () => QUOTE);

    const client = text('[data-block="client"]')!;
    expect(client).toContain('Lucía');
    expect(client).toContain('Fernández');
    expect(client).toContain('15/03/2004');
    expect(client).toContain('22 años');
    expect(client).toContain('lucia.fernandez@mail.test');

    const vehicle = text('[data-block="vehicle"]')!;
    expect(vehicle).toContain('Auto');
    expect(vehicle).toContain('2018');
    expect(vehicle).toContain('Volkswagen');
    expect(vehicle).toContain('Gol');
    expect(vehicle).toContain('Base');
    expect(vehicle).toContain('Taxi / remís');
    expect(vehicle).toContain('ABC123');

    const plan = text('[data-block="plan"]')!;
    expect(plan).toContain('Terceros completo');
    expect(plan).toContain('$ 10.800.000,00');

    const breakdown = text('[data-block="breakdown"] app-calculation-breakdown')!;
    expect(breakdown).toContain('$ 46.332,00');
    expect(breakdown).toContain('$ 56.061,72');

    expect(root.querySelector('input, mat-select, textarea')).toBeNull();
  });

  it('cliente empresa: muestra la razón social', async () => {
    await create('1', async () => ({
      ...QUOTE,
      client: {
        ...PERSON,
        firstName: null,
        lastName: null,
        birthDate: null,
        age: null,
        businessName: 'Transportes del Sur S.A.',
      },
    }));

    const client = text('[data-block="client"]')!;
    expect(client).toContain('Razón social');
    expect(client).toContain('Transportes del Sur S.A.');
    expect(client).not.toContain('Apellido');
  });

  it('con póliza emitida muestra "Póliza emitida: [número]" con enlace a su detalle', async () => {
    await create('1', async () => ({
      ...QUOTE,
      status: 'EMITTED',
      policy: { id: 4, policyNumber: 'POL-2026-000004' },
    }));

    expect(text('[data-field="policy"]')).toBe('Póliza emitida: POL-2026-000004');
    const link = root.querySelector<HTMLAnchorElement>('[data-field="policy"] a')!;
    expect(link.getAttribute('href')).toBe('/policies/4');
    expect(text('app-status-chip')).toBe('Emitida');
  });

  it('QUOTE_NOT_FOUND: muestra "Cotización no encontrada" con un botón para volver al listado', async () => {
    await create('99', () => Promise.reject(notFoundError()));

    expect(get).toHaveBeenCalledExactlyOnceWith(99);
    expect(text('[data-state="not-found"] h1')).toBe('Cotización no encontrada');
    const back = root.querySelector<HTMLAnchorElement>('[data-state="not-found"] a')!;
    expect(back.getAttribute('href')).toBe('/quotes');
    expect(root.querySelector('[data-block]')).toBeNull();
  });

  it('un id que no es numérico se muestra como "Cotización no encontrada" sin llamar al backend', async () => {
    await create('abc', async () => QUOTE);

    expect(get).not.toHaveBeenCalled();
    expect(text('[data-state="not-found"] h1')).toBe('Cotización no encontrada');
  });

  it('otro error (lo muestra el errorInterceptor): no muestra "Cotización no encontrada"', async () => {
    await create('1', () =>
      Promise.reject(new HttpErrorResponse({ status: 500, statusText: 'Error', error: {} })),
    );

    expect(root.querySelector('[data-state="not-found"]')).toBeNull();
    expect(root.querySelector('[data-block]')).toBeNull();
  });

  describe('Emitir póliza', () => {
    function issueButton(): HTMLButtonElement {
      return root.querySelector<HTMLButtonElement>('[data-action="issue"]')!;
    }

    it.each(['EMITTED', 'EXPIRED'] as const)(
      'AC-FE-05 con estado %s, "Emitir póliza" está deshabilitado',
      async (status) => {
        await create('1', async () => ({
          ...QUOTE,
          status,
          policy: status === 'EMITTED' ? { id: 4, policyNumber: 'POL-2026-000004' } : null,
        }));

        expect(issueButton().textContent?.trim()).toBe('Emitir póliza');
        expect(issueButton().disabled).toBe(true);
        issueButton().click();
        expect(dialog.open).not.toHaveBeenCalled();
      },
    );

    it('AC-FE-05 con estado CALCULATED, "Emitir póliza" está habilitado', async () => {
      await create('1', async () => QUOTE);

      expect(issueButton().disabled).toBe(false);
    });

    it('"Emitir póliza" abre el diálogo con el id de la cotización', async () => {
      await create('1', async () => QUOTE);

      issueButton().click();

      expect(dialog.open).toHaveBeenCalledExactlyOnceWith(
        IssuePolicyDialogComponent,
        expect.objectContaining({ data: { quoteId: 1 } }),
      );
    });

    it('si el diálogo cierra con "reload" (vencida o ya emitida) recarga el detalle', async () => {
      await create('1', async () => QUOTE);
      get.mockResolvedValueOnce({ ...QUOTE, status: 'EXPIRED' });
      dialogResult = 'reload';

      issueButton().click();
      await vi.waitFor(() => expect(get).toHaveBeenCalledTimes(2));
      await fixture.whenStable();

      expect(text('app-status-chip')).toBe('Vencida');
      expect(issueButton().disabled).toBe(true);
    });

    it('si se cancela el diálogo no recarga el detalle', async () => {
      await create('1', async () => QUOTE);

      issueButton().click();
      await fixture.whenStable();

      expect(get).toHaveBeenCalledTimes(1);
    });
  });
});
