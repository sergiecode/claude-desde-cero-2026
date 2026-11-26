/** Tipo de vehículo (api.md §5, manual V1). */
export type VehicleType = 'AUTO' | 'MOTO' | 'CAMIONETA' | 'CAMION';

/** Tipo de uso del vehículo (api.md §5, manual V5). */
export type VehicleUsageType =
  | 'PARTICULAR'
  | 'COMERCIAL'
  | 'TAXI_REMIS'
  | 'TRANSPORTE_CARGA'
  | 'TRANSPORTE_PASAJEROS'
  | 'AGRICOLA'
  | 'DELIVERY';

/** Elemento de los catálogos de marcas, modelos y versiones (api.md §5). */
export interface CatalogItem {
  id: number;
  name: string;
}
