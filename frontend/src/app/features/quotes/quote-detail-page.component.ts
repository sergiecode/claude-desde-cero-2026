import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  LOCALE_ID,
  resource,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { QUOTE_DETAIL_ERROR_CODES, QuoteApi } from '../../core/api/quote-api.service';
import { asProblemDetails } from '../../core/errors/error.interceptor';
import { CalculationBreakdownComponent } from '../../shared/components/calculation-breakdown.component';
import { ClientDataComponent } from '../../shared/components/client-data.component';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { VehicleDataComponent } from '../../shared/components/vehicle-data.component';
import { DISPLAY_TIMEZONE, formatMoney } from '../../shared/format';
import {
  IssuePolicyDialogComponent,
  IssuePolicyDialogData,
  IssuePolicyDialogResult,
} from './issue-policy-dialog.component';

/** `:id` de la ruta como número; `null` si no es un id válido. */
function parseId(value: string | null): number | null {
  return value && /^\d+$/.test(value) ? Number(value) : null;
}

/**
 * Detalle de cotización (frontend.md §5.5, manual §5.3). Todo en solo lectura y tal como llega del
 * backend: la fecha de vencimiento y los importes no se calculan acá.
 */
@Component({
  selector: 'app-quote-detail-page',
  imports: [
    CalculationBreakdownComponent,
    ClientDataComponent,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    RouterLink,
    StatusChipComponent,
    VehicleDataComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './quote-detail-page.component.html',
  styleUrl: './quote-detail-page.component.scss',
})
export class QuoteDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly quoteApi = inject(QuoteApi);
  private readonly locale = inject(LOCALE_ID);
  private readonly dialog = inject(MatDialog);

  protected readonly timezone = DISPLAY_TIMEZONE;

  private readonly id = toSignal(
    this.route.paramMap.pipe(map((params) => parseId(params.get('id')))),
    { initialValue: parseId(this.route.snapshot.paramMap.get('id')) },
  );

  protected readonly quoteResource = resource({
    params: () => this.id() ?? undefined,
    loader: ({ params }) => this.quoteApi.get(params),
  });

  protected readonly quote = computed(() =>
    this.quoteResource.hasValue() ? this.quoteResource.value() : null,
  );

  /** Suma asegurada guardada en la cotización, con formato es-AR. */
  protected readonly insuredAmount = computed(() => {
    const quote = this.quote();
    return quote ? formatMoney(quote.calculation.insuredAmount, this.locale) : '';
  });

  /**
   * QUOTE_NOT_FOUND (no existe o es de otro productor, Q5) o un id que no es válido:
   * "Cotización no encontrada". El resto de los errores los muestra el errorInterceptor.
   */
  protected readonly notFound = computed(() => {
    if (this.id() === null) {
      return true;
    }
    // `resource` puede envolver el error original en `cause`.
    const raw = this.quoteResource.error();
    const error = raw instanceof HttpErrorResponse ? raw : raw?.cause;
    const problem = error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
    const handled: readonly string[] = QUOTE_DETAIL_ERROR_CODES;
    return !!problem && handled.includes(problem.code);
  });

  /**
   * Abre el diálogo "Emitir póliza" (frontend.md §5.6). Si la cotización venció o ya se emitió
   * mientras tanto, el diálogo cierra con `'reload'` y se vuelve a pedir el detalle.
   */
  protected openIssueDialog(quoteId: number): void {
    this.dialog
      .open<IssuePolicyDialogComponent, IssuePolicyDialogData, IssuePolicyDialogResult>(
        IssuePolicyDialogComponent,
        { data: { quoteId }, width: '420px' },
      )
      .afterClosed()
      .subscribe((result) => {
        if (result === 'reload') {
          this.quoteResource.reload();
        }
      });
  }
}
