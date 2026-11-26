import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Client } from '../../core/models/client.model';

/**
 * Datos del cliente en solo lectura (detalle de cotización y de póliza). La edad se muestra tal
 * como llega del backend, sin recalcular.
 */
@Component({
  selector: 'app-client-data',
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let c = client();
    <dl class="read-only-data">
      @if (c.businessName) {
        <div><dt>Razón social</dt><dd>{{ c.businessName }}</dd></div>
      } @else {
        <div><dt>Nombre</dt><dd>{{ c.firstName }}</dd></div>
        <div><dt>Apellido</dt><dd>{{ c.lastName }}</dd></div>
        <div><dt>Fecha de nacimiento</dt><dd>{{ c.birthDate | date: 'dd/MM/yyyy' }}</dd></div>
        <div><dt>Edad</dt><dd>{{ c.age }} años</dd></div>
      }
      <div><dt>Email</dt><dd>{{ c.email }}</dd></div>
      <div><dt>Teléfono</dt><dd>{{ c.phone }}</dd></div>
      <div><dt>Domicilio</dt><dd>{{ c.address }}</dd></div>
      <div><dt>Localidad</dt><dd>{{ c.city }}</dd></div>
      <div><dt>Provincia</dt><dd>{{ c.province }}</dd></div>
    </dl>
  `,
  styleUrl: './read-only-data.scss',
})
export class ClientDataComponent {
  readonly client = input.required<Client>();
}
