import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';

import { PolicyStatus } from '../../core/models/policy.model';
import { QuoteStatus } from '../../core/models/quote.model';
import { POLICY_STATUS_LABELS, QUOTE_STATUS_LABELS } from '../labels';

/** Color del chip (frontend.md §6): Calculada y Activa primario, Emitida verde, Vencida gris. */
type ChipColor = 'primary' | 'success' | 'muted';

type StatusChipValue =
  | { kind: 'quote'; status: QuoteStatus }
  | { kind: 'policy'; status: PolicyStatus };

const QUOTE_COLORS: Record<QuoteStatus, ChipColor> = {
  CALCULATED: 'primary',
  EMITTED: 'success',
  EXPIRED: 'muted',
};

const POLICY_COLORS: Record<PolicyStatus, ChipColor> = {
  ACTIVE: 'primary',
  EXPIRED: 'muted',
};

/**
 * Estado de una cotización o de una póliza con su etiqueta en español (frontend.md §6).
 * Uso: `<app-status-chip kind="quote" [status]="quote.status" />`.
 */
@Component({
  selector: 'app-status-chip',
  imports: [MatChipsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-chip-set>
      <mat-chip
        class="status-chip"
        [class]="'status-chip--' + chip().color"
        [attr.data-status]="status()"
        disableRipple
      >
        {{ chip().label }}
      </mat-chip>
    </mat-chip-set>
  `,
  styles: `
    :host {
      display: inline-block;
    }
    .status-chip--primary {
      --mat-chip-elevated-container-color: var(--mat-sys-primary-container);
      --mat-chip-label-text-color: var(--mat-sys-on-primary-container);
    }
    .status-chip--success {
      --mat-chip-elevated-container-color: #c8e6c9;
      --mat-chip-label-text-color: #1b5e20;
    }
    .status-chip--muted {
      --mat-chip-elevated-container-color: #e0e0e0;
      --mat-chip-label-text-color: #424242;
    }
  `,
})
export class StatusChipComponent {
  readonly kind = input.required<'quote' | 'policy'>();
  readonly status = input.required<QuoteStatus | PolicyStatus>();

  protected readonly chip = computed(() => {
    const value = { kind: this.kind(), status: this.status() } as StatusChipValue;
    return value.kind === 'quote'
      ? { label: QUOTE_STATUS_LABELS[value.status], color: QUOTE_COLORS[value.status] }
      : { label: POLICY_STATUS_LABELS[value.status], color: POLICY_COLORS[value.status] };
  });
}
