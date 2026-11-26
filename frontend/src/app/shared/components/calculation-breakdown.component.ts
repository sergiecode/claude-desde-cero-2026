import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  LOCALE_ID,
} from '@angular/core';
import { MatTableModule } from '@angular/material/table';

import { QuoteCalculation } from '../../core/models/quote.model';
import { formatFactor, formatMoney, formatRate } from '../format';

/** Una fila del desglose: concepto y valor ya formateado. */
interface BreakdownRow {
  concept: string;
  value: string;
  /** Prima mensual y Total mensual van destacados. */
  total: boolean;
}

/**
 * Desglose del precio de una cotización (frontend.md §5.7). Muestra los valores tal como los
 * devuelve el backend, solo con formato es-AR: no calcula nada.
 */
@Component({
  selector: 'app-calculation-breakdown',
  imports: [MatTableModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './calculation-breakdown.component.html',
  styleUrl: './calculation-breakdown.component.scss',
})
export class CalculationBreakdownComponent {
  private readonly locale = inject(LOCALE_ID);

  readonly calculation = input.required<QuoteCalculation>();

  protected readonly columns = ['concept', 'value'];

  protected readonly rows = computed<BreakdownRow[]>(() => {
    const c = this.calculation();
    const money = (amount: number) => formatMoney(amount, this.locale);
    const factor = (value: number) => formatFactor(value, this.locale);
    const driver = c.driverAge === null ? 'Empresa' : years(c.driverAge);
    return [
      row('Suma asegurada', money(c.insuredAmount)),
      row('Tasa del plan', formatRate(c.planRate, this.locale)),
      row('Prima base', money(c.basePremium)),
      row(`Factor geográfico (${c.zone.name})`, factor(c.zone.factor)),
      row('Factor de uso', factor(c.usageFactor)),
      row(`Factor de antigüedad (${years(c.vehicleAgeYears)})`, factor(c.vehicleAgeFactor)),
      row(`Factor de edad (${driver})`, factor(c.driverAgeFactor)),
      row('Prima mensual', money(c.premium), true),
      row(`Impuestos (${formatRate(c.taxRate, this.locale, '1.0-2')})`, money(c.taxes)),
      row('Total mensual', money(c.totalPrice), true),
    ];
  });
}

function row(concept: string, value: string, total = false): BreakdownRow {
  return { concept, value, total };
}

function years(n: number): string {
  return n === 1 ? '1 año' : `${n} años`;
}
