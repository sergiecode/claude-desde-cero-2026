import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { form, FormField, maxDate, minDate, required, validate } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { provideNativeDateAdapter } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { ISSUE_ERROR_CODES, QuoteApi } from '../../core/api/quote-api.service';
import { ErrorMessageService } from '../../core/errors/error-message.service';
import { asProblemDetails } from '../../core/errors/error.interceptor';
import { toLocalIsoDate } from '../../shared/format';

/** Datos que recibe el diálogo. */
export interface IssuePolicyDialogData {
  quoteId: number;
}

/**
 * Resultado al cerrar: `'issued'` si se emitió (el diálogo ya navegó a la póliza), `'reload'` si
 * la cotización venció o ya estaba emitida y hay que recargar el detalle. `undefined` al cancelar.
 */
export type IssuePolicyDialogResult = 'issued' | 'reload';

const REQUIRED_MESSAGE = 'Completá este campo';
/** Mensaje de E4 (manual §9); lo mismo responde el backend con INVALID_START_DATE. */
const START_DATE_RANGE_MESSAGE = 'La fecha de inicio debe estar entre hoy y los próximos 30 días';
/** Días hacia adelante que permite el selector (frontend.md §5.6, regla E4). */
const MAX_DAYS_AHEAD = 30;

function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

function isValidDate(value: unknown): value is Date {
  return value instanceof Date && !Number.isNaN(value.getTime());
}

/**
 * Diálogo "Emitir póliza" (frontend.md §5.6, manual §5.3). El rango hoy…hoy+30 es solo una ayuda
 * para el productor: la validación que vale es la del backend (INVALID_START_DATE).
 */
@Component({
  selector: 'app-issue-policy-dialog',
  imports: [
    FormField,
    MatButtonModule,
    MatDatepickerModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  providers: [provideNativeDateAdapter()],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>Emitir póliza</h2>
    <mat-dialog-content>
      <mat-form-field class="issue-dialog__field">
        <mat-label>Fecha de inicio</mat-label>
        <!--
          $any: FormField pasa min/max como \`Date | undefined\` y MatDatepickerInput los tipa
          \`Date | null\`; en ejecución acepta los dos.
        -->
        <input
          matInput
          data-field="startDate"
          [matDatepicker]="picker"
          [formField]="$any(issueForm.startDate)"
        />
        <mat-hint>dd/mm/aaaa</mat-hint>
        <mat-datepicker-toggle matIconSuffix [for]="picker" />
        <mat-datepicker #picker [startAt]="today" />
      </mat-form-field>
      @if (startDateError(); as error) {
        <p class="issue-dialog__error" role="alert" data-field="startDateError">{{ error }}</p>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button matButton type="button" data-action="cancel" mat-dialog-close>Cancelar</button>
      <button
        matButton="filled"
        type="button"
        data-action="confirm"
        [disabled]="issuing() || issueForm().invalid()"
        (click)="confirm()"
      >
        Confirmar emisión
      </button>
    </mat-dialog-actions>
  `,
  styles: `
    .issue-dialog__field {
      width: 100%;
    }
    .issue-dialog__error {
      margin: 0;
      color: var(--mat-sys-error);
      font-size: 0.875rem;
    }
  `,
})
export class IssuePolicyDialogComponent {
  private readonly data = inject<IssuePolicyDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef =
    inject<MatDialogRef<IssuePolicyDialogComponent, IssuePolicyDialogResult>>(MatDialogRef);
  private readonly quoteApi = inject(QuoteApi);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly messages = inject(ErrorMessageService);

  /** Hoy en fecha local; mínimo y valor inicial del selector. */
  protected readonly today = startOfDay(new Date());
  private readonly lastAllowedDate = addDays(this.today, MAX_DAYS_AHEAD);

  /** Llamada en curso: deshabilita "Confirmar emisión" (evita dobles envíos). */
  protected readonly issuing = signal(false);
  /** `detail` de INVALID_START_DATE; se borra al cambiar la fecha. */
  private readonly serverError = signal<{ date: Date; message: string } | null>(null);

  /** El datepicker escribe `null` si se borra la fecha o se escribe una inválida. */
  private readonly model = signal<{ startDate: Date | null }>({ startDate: this.today });

  /**
   * Las reglas `minDate`/`maxDate` validan el rango y, a través de `[formField]`, también
   * limitan las fechas que se pueden elegir en el calendario (`min`/`max` del datepicker).
   */
  protected readonly issueForm = form(this.model, (f) => {
    required(f.startDate, { message: REQUIRED_MESSAGE });
    minDate(f.startDate, this.today, { message: START_DATE_RANGE_MESSAGE });
    maxDate(f.startDate, this.lastAllowedDate, { message: START_DATE_RANGE_MESSAGE });
    validate(f.startDate, ({ value }) => {
      const error = this.serverError();
      return error && error.date === value() ? { kind: 'server', message: error.message } : null;
    });
  });

  /**
   * Mensaje a mostrar debajo de la fecha: el error del backend apenas llega; los de validación,
   * cuando el campo fue tocado. El datepicker suma sus propios errores (`matDatepickerMin`,
   * `matDatepickerParse`…) sin mensaje: se muestra el primero de las reglas del schema.
   */
  protected readonly startDateError = computed((): string | null => {
    const field = this.issueForm.startDate();
    const errors = field.errors();
    const server = errors.find((e) => e.kind === 'server');
    if (server) {
      return server.message ?? null;
    }
    return field.touched() ? (errors.find((e) => e.message)?.message ?? null) : null;
  });

  protected async confirm(): Promise<void> {
    const startDate: unknown = this.model().startDate;
    if (this.issuing() || this.issueForm().invalid() || !isValidDate(startDate)) {
      return;
    }
    this.issuing.set(true);
    try {
      const policy = await this.quoteApi.issue(this.data.quoteId, toLocalIsoDate(startDate));
      this.dialogRef.close('issued');
      this.snackBar.open(`Póliza ${policy.policyNumber} emitida correctamente`, 'Cerrar', {
        duration: 5000,
      });
      await this.router.navigate(['/policies', policy.id]);
    } catch (error) {
      this.handleError(error, startDate);
    } finally {
      this.issuing.set(false);
    }
  }

  /**
   * QUOTE_EXPIRED / QUOTE_ALREADY_ISSUED: cierra, muestra el `detail` y pide recargar el detalle.
   * INVALID_START_DATE: muestra el `detail` dentro del diálogo. El resto ya lo mostró el
   * errorInterceptor; el diálogo queda abierto.
   */
  private handleError(error: unknown, startDate: Date): void {
    const problem = error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
    const handled: readonly string[] = ISSUE_ERROR_CODES;
    if (!problem || !handled.includes(problem.code)) {
      return;
    }
    if (problem.code === 'INVALID_START_DATE') {
      this.serverError.set({ date: startDate, message: problem.detail });
      return;
    }
    this.dialogRef.close('reload');
    this.messages.show(problem.detail);
  }
}
