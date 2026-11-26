import { VehicleType, VehicleUsageType } from '../core/models/catalog.model';
import { DocumentType } from '../core/models/client.model';
import { PolicyStatus } from '../core/models/policy.model';
import { QuoteStatus } from '../core/models/quote.model';

/** Etiquetas en español de los enums (frontend.md §6). */
export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  DNI: 'DNI',
  CUIT: 'CUIT',
};

export const VEHICLE_TYPE_LABELS: Record<VehicleType, string> = {
  AUTO: 'Auto',
  MOTO: 'Moto',
  CAMIONETA: 'Camioneta',
  CAMION: 'Camión',
};

export const VEHICLE_USAGE_TYPE_LABELS: Record<VehicleUsageType, string> = {
  PARTICULAR: 'Particular',
  COMERCIAL: 'Comercial',
  TAXI_REMIS: 'Taxi / remís',
  TRANSPORTE_CARGA: 'Transporte de carga',
  TRANSPORTE_PASAJEROS: 'Transporte de pasajeros',
  AGRICOLA: 'Agrícola',
  DELIVERY: 'Delivery',
};

export const QUOTE_STATUS_LABELS: Record<QuoteStatus, string> = {
  CALCULATED: 'Calculada',
  EMITTED: 'Emitida',
  EXPIRED: 'Vencida',
};

export const POLICY_STATUS_LABELS: Record<PolicyStatus, string> = {
  ACTIVE: 'Activa',
  EXPIRED: 'Vencida',
};
