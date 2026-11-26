import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideNativeDateAdapter } from '@angular/material/core';
import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerIntl,
  MatDatepickerToggle,
} from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';

import { appConfig } from '../app.config';
import { provideSpanishDatepickerIntl, SpanishDatepickerIntl } from './datepicker-intl';

@Component({
  // Igual que IssuePolicyDialogComponent: directivas sueltas, sin MatDatepickerModule.
  imports: [MatDatepicker, MatDatepickerInput, MatDatepickerToggle, MatInputModule],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-form-field>
      <input matInput [matDatepicker]="picker" />
      <mat-datepicker-toggle matIconSuffix [for]="picker" />
      <mat-datepicker #picker />
    </mat-form-field>
  `,
})
class DatepickerHost {}

describe('SpanishDatepickerIntl', () => {
  // Lo provee IssuePolicyDialogComponent (ver su spec): en app.config sumaría
  // @angular/material/datepicker al bundle inicial (frontend.md §3).
  it('app.config no registra el datepicker (queda fuera del bundle inicial)', () => {
    expect(appConfig.providers).not.toContainEqual(provideSpanishDatepickerIntl());
  });

  it('el botón del calendario dice "Abrir calendario"', async () => {
    TestBed.configureTestingModule({ providers: [provideSpanishDatepickerIntl()] });
    const fixture = TestBed.createComponent(DatepickerHost);
    await fixture.whenStable();

    const button = (fixture.nativeElement as HTMLElement).querySelector(
      'mat-datepicker-toggle button',
    );
    expect(button?.getAttribute('aria-label')).toBe('Abrir calendario');
    expect(TestBed.inject(MatDatepickerIntl)).toBeInstanceOf(SpanishDatepickerIntl);
  });

  it('todos los textos del datepicker están en español', () => {
    const intl = new SpanishDatepickerIntl();
    expect(intl).toMatchObject({
      calendarLabel: 'Calendario',
      openCalendarLabel: 'Abrir calendario',
      closeCalendarLabel: 'Cerrar calendario',
      prevMonthLabel: 'Mes anterior',
      nextMonthLabel: 'Mes siguiente',
      prevYearLabel: 'Año anterior',
      nextYearLabel: 'Año siguiente',
      prevMultiYearLabel: '24 años anteriores',
      nextMultiYearLabel: '24 años siguientes',
      switchToMonthViewLabel: 'Elegir fecha',
      switchToMultiYearViewLabel: 'Elegir mes y año',
      startDateLabel: 'Fecha de inicio',
      endDateLabel: 'Fecha de fin',
      comparisonDateLabel: 'Rango de comparación',
    });
    expect(intl.formatYearRangeLabel('2024', '2047')).toBe('2024 a 2047');
  });
});
