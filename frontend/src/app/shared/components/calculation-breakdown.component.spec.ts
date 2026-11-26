import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { QuoteCalculation } from '../../core/models/quote.model';
import { CalculationBreakdownComponent } from './calculation-breakdown.component';

registerLocaleData(localeEsAr);

const CALCULATION: QuoteCalculation = {
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
};

describe('CalculationBreakdownComponent', () => {
  let fixture: ComponentFixture<CalculationBreakdownComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [{ provide: LOCALE_ID, useValue: 'es-AR' }] });
    fixture = TestBed.createComponent(CalculationBreakdownComponent);
  });

  async function render(calculation: QuoteCalculation): Promise<[string, string][]> {
    fixture.componentRef.setInput('calculation', calculation);
    await fixture.whenStable();
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tr');
    return Array.from(rows).map((tr) => {
      const [concept, value] = Array.from(tr.querySelectorAll('td')).map(
        (td) => td.textContent?.trim() ?? '',
      );
      return [concept, value];
    });
  }

  it('AC-FE-06 con premium 46332 y totalPrice 56061.72 muestra "$ 46.332,00" y "$ 56.061,72"', async () => {
    const rows = await render(CALCULATION);
    const text = (fixture.nativeElement as HTMLElement).textContent!;

    expect(text).toContain('$ 46.332,00');
    expect(text).toContain('$ 56.061,72');
    expect(rows).toContainEqual(['Prima mensual', '$ 46.332,00']);
    expect(rows).toContainEqual(['Total mensual', '$ 56.061,72']);
  });

  it('muestra los valores recibidos sin modificarlos, en orden y con formato es-AR', async () => {
    const calculation = structuredClone(CALCULATION);
    const rows = await render(calculation);

    expect(rows).toEqual([
      ['Suma asegurada', '$ 10.800.000,00'],
      ['Tasa del plan', '0,25 %'],
      ['Prima base', '$ 27.000,00'],
      ['Factor geográfico (CABA)', '1,20'],
      ['Factor de uso', '1,00'],
      ['Factor de antigüedad (8 años)', '1,10'],
      ['Factor de edad (22 años)', '1,30'],
      ['Prima mensual', '$ 46.332,00'],
      ['Impuestos (21 %)', '$ 9.729,72'],
      ['Total mensual', '$ 56.061,72'],
    ]);
    // No modifica el objeto recibido.
    expect(calculation).toEqual(CALCULATION);
  });

  it('no recalcula: muestra los importes del backend aunque no coincidan con los factores', async () => {
    const rows = await render({ ...CALCULATION, premium: 1, taxes: 2, totalPrice: 3 });

    expect(rows).toContainEqual(['Prima mensual', '$ 1,00']);
    expect(rows).toContainEqual(['Impuestos (21 %)', '$ 2,00']);
    expect(rows).toContainEqual(['Total mensual', '$ 3,00']);
  });

  it('cliente empresa (driverAge null): muestra "Factor de edad (Empresa)"', async () => {
    const rows = await render({ ...CALCULATION, driverAge: null, driverAgeFactor: 1.0 });

    expect(rows).toContainEqual(['Factor de edad (Empresa)', '1,00']);
  });
});
