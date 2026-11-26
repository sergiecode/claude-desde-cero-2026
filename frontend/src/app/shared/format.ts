import { formatNumber } from '@angular/common';

/**
 * Formatos de presentación es-AR. Solo dan formato a valores que vienen del backend:
 * no calculan precios ni redondean importes (el backend ya los manda con 2 decimales).
 */

/** Monto con el formato del manual: `$ 8.640.000,00`. */
export function formatMoney(amount: number, locale: string): string {
  return `$ ${formatNumber(amount, locale, '1.2-2')}`;
}

/** Factor con 2 decimales y coma: `1,20` (frontend.md §5.7). */
export function formatFactor(factor: number, locale: string): string {
  return formatNumber(factor, locale, '1.2-2');
}

/**
 * Tasa expresada como porcentaje: `0.0025` → `0,25 %`, `0.21` → `21 %` (frontend.md §5.7).
 * `digitsInfo` define los decimales visibles.
 */
export function formatRate(rate: number, locale: string, digitsInfo = '1.2-2'): string {
  return `${formatNumber(rate * 100, locale, digitsInfo)} %`;
}

/**
 * Zona horaria de las fechas con hora del backend (America/Argentina/Buenos_Aires, sin horario
 * de verano). Se pasa al `DatePipe` para que la hora no dependa de la zona del navegador.
 */
export const DISPLAY_TIMEZONE = '-0300';

/**
 * Fecha local como `yyyy-MM-dd`, sin pasar por UTC (`toISOString` podría correr el día).
 * Es el formato de `startDate` en `POST /api/quotes/{id}/issue` (api.md §8.6).
 */
export function toLocalIsoDate(date: Date): string {
  const year = String(date.getFullYear()).padStart(4, '0');
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}
