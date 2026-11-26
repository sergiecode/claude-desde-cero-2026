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
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs';

import { POLICY_DETAIL_ERROR_CODES, PolicyApi } from '../../core/api/policy-api.service';
import { asProblemDetails } from '../../core/errors/error.interceptor';
import { ClientDataComponent } from '../../shared/components/client-data.component';
import { StatusChipComponent } from '../../shared/components/status-chip.component';
import { VehicleDataComponent } from '../../shared/components/vehicle-data.component';
import { formatMoney } from '../../shared/format';

/** `:id` de la ruta como número; `null` si no es un id válido. */
function parseId(value: string | null): number | null {
  return value && /^\d+$/.test(value) ? Number(value) : null;
}

/**
 * Detalle de póliza (frontend.md §5.8, manual §5.4). Todo en solo lectura: la vigencia y los
 * importes se muestran tal como llegan del backend, sin calcular nada.
 */
@Component({
  selector: 'app-policy-detail-page',
  imports: [
    ClientDataComponent,
    DatePipe,
    MatButtonModule,
    MatCardModule,
    RouterLink,
    StatusChipComponent,
    VehicleDataComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './policy-detail-page.component.html',
  styleUrl: './policy-detail-page.component.scss',
})
export class PolicyDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly policyApi = inject(PolicyApi);
  private readonly locale = inject(LOCALE_ID);

  private readonly id = toSignal(
    this.route.paramMap.pipe(map((params) => parseId(params.get('id')))),
    { initialValue: parseId(this.route.snapshot.paramMap.get('id')) },
  );

  protected readonly policyResource = resource({
    params: () => this.id() ?? undefined,
    loader: ({ params }) => this.policyApi.get(params),
  });

  protected readonly policy = computed(() =>
    this.policyResource.hasValue() ? this.policyResource.value() : null,
  );

  /** Importes de la póliza con formato es-AR, tal como llegan del backend. */
  protected readonly amounts = computed(() => {
    const policy = this.policy();
    if (!policy) {
      return null;
    }
    return {
      insuredAmount: formatMoney(policy.insuredAmount, this.locale),
      premium: formatMoney(policy.premium, this.locale),
      taxes: formatMoney(policy.taxes, this.locale),
      totalPrice: formatMoney(policy.totalPrice, this.locale),
    };
  });

  /**
   * POLICY_NOT_FOUND (no existe o es de otro productor, E10) o un id que no es válido:
   * "Póliza no encontrada". El resto de los errores los muestra el errorInterceptor.
   */
  protected readonly notFound = computed(() => {
    if (this.id() === null) {
      return true;
    }
    // `resource` puede envolver el error original en `cause`.
    const raw = this.policyResource.error();
    const error = raw instanceof HttpErrorResponse ? raw : raw?.cause;
    const problem = error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
    const handled: readonly string[] = POLICY_DETAIL_ERROR_CODES;
    return !!problem && handled.includes(problem.code);
  });
}
