import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  LOCALE_ID,
  resource,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { Router, RouterLink } from '@angular/router';

import { QuoteApi } from '../../core/api/quote-api.service';
import { QuoteSummary } from '../../core/models/quote.model';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { DISPLAY_TIMEZONE, formatMoney } from '../../shared/format';
import { VEHICLE_TYPE_LABELS } from '../../shared/labels';

/** Fila de la tabla: el resumen del backend con el vehículo ya armado para mostrar. */
interface QuoteRow extends QuoteSummary {
  vehicle: string;
  /** `totalPrice` del backend con el formato es-AR del manual: `$ 56.061,72`. */
  total: string;
}

/**
 * Mis cotizaciones (frontend.md §5.3, manual §5.5). Muestra las cotizaciones del productor en el
 * orden en que las devuelve el backend (la más reciente primero). Los errores de la carga los
 * muestra el errorInterceptor.
 */
@Component({
  selector: 'app-quote-list-page',
  imports: [DatePipe, MatButtonModule, MatTableModule, RouterLink, StatusChipComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="quote-list__header">
      <h1>Mis cotizaciones</h1>
      <a matButton="filled" routerLink="/quotes/new">Nueva cotización</a>
    </div>

    @if (quotesResource.hasValue()) {
      @if (rows().length) {
        <table mat-table [dataSource]="rows()" class="quote-list__table">
          <ng-container matColumnDef="quoteNumber">
            <th mat-header-cell *matHeaderCellDef>N.º</th>
            <td mat-cell *matCellDef="let q">{{ q.quoteNumber }}</td>
          </ng-container>
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Fecha</th>
            <td mat-cell *matCellDef="let q">{{ q.createdAt | date: 'dd/MM/yyyy' : timezone }}</td>
          </ng-container>
          <ng-container matColumnDef="clientName">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let q">{{ q.clientName }}</td>
          </ng-container>
          <ng-container matColumnDef="vehicle">
            <th mat-header-cell *matHeaderCellDef>Vehículo</th>
            <td mat-cell *matCellDef="let q">{{ q.vehicle }}</td>
          </ng-container>
          <ng-container matColumnDef="planName">
            <th mat-header-cell *matHeaderCellDef>Plan</th>
            <td mat-cell *matCellDef="let q">{{ q.planName }}</td>
          </ng-container>
          <ng-container matColumnDef="totalPrice">
            <th mat-header-cell *matHeaderCellDef>Total mensual</th>
            <td mat-cell *matCellDef="let q">{{ q.total }}</td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Estado</th>
            <td mat-cell *matCellDef="let q"><app-status-chip kind="quote" [status]="q.status" /></td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr
            mat-row
            *matRowDef="let q; columns: columns"
            class="quote-list__row"
            tabindex="0"
            (click)="open(q)"
            (keydown.enter)="open(q)"
          ></tr>
        </table>
      } @else {
        <p class="quote-list__empty">Todavía no tenés cotizaciones</p>
      }
    }
  `,
  styles: `
    .quote-list__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .quote-list__table {
      width: 100%;
    }
    .quote-list__row {
      cursor: pointer;
    }
    .quote-list__row:hover,
    .quote-list__row:focus-visible {
      background: var(--mat-sys-surface-container-high);
    }
  `,
})
export class QuoteListPageComponent {
  private readonly quoteApi = inject(QuoteApi);
  private readonly router = inject(Router);
  private readonly locale = inject(LOCALE_ID);

  protected readonly timezone = DISPLAY_TIMEZONE;

  protected readonly columns = [
    'quoteNumber',
    'createdAt',
    'clientName',
    'vehicle',
    'planName',
    'totalPrice',
    'status',
  ];

  protected readonly quotesResource = resource({ loader: () => this.quoteApi.list() });

  /** Vehículo: tipo con su etiqueta en español + marca + modelo (frontend.md §5.3). */
  protected readonly rows = computed<QuoteRow[]>(() =>
    (this.quotesResource.hasValue() ? this.quotesResource.value() : []).map((q) => ({
      ...q,
      vehicle: `${VEHICLE_TYPE_LABELS[q.vehicleType]} ${q.brandName} ${q.modelName}`,
      total: formatMoney(q.totalPrice, this.locale),
    })),
  );

  protected open(quote: QuoteSummary): void {
    void this.router.navigate(['/quotes', quote.id]);
  }
}
