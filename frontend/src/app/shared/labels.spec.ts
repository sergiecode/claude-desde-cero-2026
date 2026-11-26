import {
  DOCUMENT_TYPE_LABELS,
  POLICY_STATUS_LABELS,
  QUOTE_STATUS_LABELS,
  VEHICLE_TYPE_LABELS,
  VEHICLE_USAGE_TYPE_LABELS,
} from './labels';

/** Códigos completos de cada enum, según data-model.md §2. */
const ENUMS: [string, Record<string, string>, string[]][] = [
  ['DocumentType', DOCUMENT_TYPE_LABELS, ['DNI', 'CUIT']],
  ['VehicleType', VEHICLE_TYPE_LABELS, ['AUTO', 'MOTO', 'CAMIONETA', 'CAMION']],
  [
    'VehicleUsageType',
    VEHICLE_USAGE_TYPE_LABELS,
    [
      'PARTICULAR',
      'COMERCIAL',
      'TAXI_REMIS',
      'TRANSPORTE_CARGA',
      'TRANSPORTE_PASAJEROS',
      'AGRICOLA',
      'DELIVERY',
    ],
  ],
  ['QuoteStatus', QUOTE_STATUS_LABELS, ['CALCULATED', 'EMITTED', 'EXPIRED']],
  ['PolicyStatus', POLICY_STATUS_LABELS, ['ACTIVE', 'EXPIRED']],
];

describe('labels', () => {
  it.each(ENUMS)(
    'AC-FE-07 todos los códigos de %s tienen etiqueta en español',
    (_name, labels, codes) => {
      expect(Object.keys(labels).sort()).toEqual([...codes].sort());
      for (const code of codes) {
        expect(labels[code]?.trim()).toBeTruthy();
      }
    },
  );

  it('AC-FE-07 las etiquetas de los estados son las de frontend.md §6', () => {
    expect(QUOTE_STATUS_LABELS).toEqual({
      CALCULATED: 'Calculada',
      EMITTED: 'Emitida',
      EXPIRED: 'Vencida',
    });
    expect(POLICY_STATUS_LABELS).toEqual({ ACTIVE: 'Activa', EXPIRED: 'Vencida' });
  });
});
