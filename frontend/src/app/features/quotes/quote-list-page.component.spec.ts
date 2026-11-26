import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { QuoteApi } from '../../core/api/quote-api.service';
import { QuoteSummary } from '../../core/models/quote.model';
import { QuoteListPageComponent } from './quote-list-page.component';

registerLocaleData(localeEsAr);

const QUOTES: QuoteSummary[] = [
  {
    id: 2,
    quoteNumber: 'COT-2026-000002',
    createdAt: '2026-11-20T23:30:00-03:00',
    clientName: 'Transportes del Sur S.A.',
    vehicleType: 'CAMION',
    brandName: 'Mercedes-Benz',
    modelName: 'Atego',
    planName: 'Todo riesgo',
    totalPrice: 123456.7,
    currency: 'ARS',
    status: 'EMITTED',
  },
  {
    id: 1,
    quoteNumber: 'COT-2026-000001',
    createdAt: '2026-11-19T10:00:00-03:00',
    clientName: 'Lucía Fernández',
    vehicleType: 'AUTO',
    brandName: 'Volkswagen',
    modelName: 'Gol',
    planName: 'Terceros completo',
    totalPrice: 56061.72,
    currency: 'ARS',
    status: 'CALCULATED',
  },
];

describe('QuoteListPageComponent', () => {
  let fixture: ComponentFixture<QuoteListPageComponent>;
  let root: HTMLElement;
  let list: ReturnType<typeof vi.fn>;

  async function create(quotes: QuoteSummary[]): Promise<void> {
    list = vi.fn(async () => quotes);
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: QuoteApi, useValue: { list } },
        { provide: LOCALE_ID, useValue: 'es-AR' },
      ],
    });
    fixture = TestBed.createComponent(QuoteListPageComponent);
    root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  }

  function rows(): HTMLElement[] {
    return Array.from(root.querySelectorAll<HTMLElement>('tr.quote-list__row'));
  }

  function cells(): string[][] {
    return rows().map((tr) =>
      Array.from(tr.querySelectorAll('td')).map((td) => td.textContent?.trim() ?? ''),
    );
  }

  it('muestra las columnas y las cotizaciones en el orden del backend, con formato es-AR', async () => {
    await create(QUOTES);

    const headers = Array.from(root.querySelectorAll('th')).map((th) => th.textContent?.trim());
    expect(headers).toEqual([
      'N.º',
      'Fecha',
      'Cliente',
      'Vehículo',
      'Plan',
      'Total mensual',
      'Estado',
    ]);
    expect(cells()).toEqual([
      [
        'COT-2026-000002',
        '20/11/2026',
        'Transportes del Sur S.A.',
        'Camión Mercedes-Benz Atego',
        'Todo riesgo',
        '$ 123.456,70',
        'Emitida',
      ],
      [
        'COT-2026-000001',
        '19/11/2026',
        'Lucía Fernández',
        'Auto Volkswagen Gol',
        'Terceros completo',
        '$ 56.061,72',
        'Calculada',
      ],
    ]);
    expect(list).toHaveBeenCalledTimes(1);
    expect(root.querySelector('a[href="/quotes/new"]')?.textContent?.trim()).toBe(
      'Nueva cotización',
    );
  });

  it('clic en una fila abre el detalle de la cotización', async () => {
    await create(QUOTES);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);

    rows()[1].click();

    expect(navigate).toHaveBeenCalledWith(['/quotes', 1]);
  });

  it('sin cotizaciones muestra "Todavía no tenés cotizaciones"', async () => {
    await create([]);

    expect(root.textContent).toContain('Todavía no tenés cotizaciones');
    expect(root.querySelector('table')).toBeNull();
  });
});
