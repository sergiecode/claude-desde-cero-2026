import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { QuoteVehicle } from '../../core/models/quote.model';
import { VEHICLE_TYPE_LABELS, VEHICLE_USAGE_TYPE_LABELS } from '../labels';

/** Datos del vehículo en solo lectura (detalle de cotización y de póliza). */
@Component({
  selector: 'app-vehicle-data',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let v = vehicle();
    <dl class="read-only-data">
      <div><dt>Tipo de vehículo</dt><dd>{{ vehicleTypeLabels[v.vehicleType] }}</dd></div>
      <div><dt>Año de fabricación</dt><dd>{{ v.manufacturingYear }}</dd></div>
      <div><dt>Marca</dt><dd>{{ v.brandName }}</dd></div>
      <div><dt>Modelo</dt><dd>{{ v.modelName }}</dd></div>
      <div><dt>Versión</dt><dd>{{ v.versionName }}</dd></div>
      <div><dt>Tipo de uso</dt><dd>{{ usageTypeLabels[v.usageType] }}</dd></div>
      <div><dt>Patente</dt><dd>{{ v.patent }}</dd></div>
    </dl>
  `,
  styleUrl: './read-only-data.scss',
})
export class VehicleDataComponent {
  readonly vehicle = input.required<QuoteVehicle>();

  protected readonly vehicleTypeLabels = VEHICLE_TYPE_LABELS;
  protected readonly usageTypeLabels = VEHICLE_USAGE_TYPE_LABELS;
}
