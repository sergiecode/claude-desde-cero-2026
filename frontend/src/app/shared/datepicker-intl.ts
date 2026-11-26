import { Injectable, Provider } from '@angular/core';
import { MatDatepickerIntl } from '@angular/material/datepicker';

/** Textos del datepicker de Angular Material en español (los que leen los lectores de pantalla). */
@Injectable()
export class SpanishDatepickerIntl extends MatDatepickerIntl {
  override calendarLabel = 'Calendario';
  override openCalendarLabel = 'Abrir calendario';
  override closeCalendarLabel = 'Cerrar calendario';
  override prevMonthLabel = 'Mes anterior';
  override nextMonthLabel = 'Mes siguiente';
  override prevYearLabel = 'Año anterior';
  override nextYearLabel = 'Año siguiente';
  override prevMultiYearLabel = '24 años anteriores';
  override nextMultiYearLabel = '24 años siguientes';
  override switchToMonthViewLabel = 'Elegir fecha';
  override switchToMultiYearViewLabel = 'Elegir mes y año';
  override startDateLabel = 'Fecha de inicio';
  override endDateLabel = 'Fecha de fin';
  override comparisonDateLabel = 'Rango de comparación';

  override formatYearRange(start: string, end: string): string {
    return `${start} – ${end}`;
  }

  override formatYearRangeLabel(start: string, end: string): string {
    return `${start} a ${end}`;
  }
}

/**
 * Registra los textos en español del datepicker. Se usa en los `providers` del componente que
 * tiene el datepicker (no en app.config) para no sumar el datepicker al bundle inicial.
 */
export function provideSpanishDatepickerIntl(): Provider {
  return { provide: MatDatepickerIntl, useClass: SpanishDatepickerIntl };
}
