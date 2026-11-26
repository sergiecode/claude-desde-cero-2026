import { VehicleType, VehicleUsageType } from './catalog.model';
import { Client } from './client.model';

/** Estado de una cotización (api.md, data-model.md §2). */
export type QuoteStatus = 'CALCULATED' | 'EMITTED' | 'EXPIRED';

/** Request común de `POST /api/quotes/calculate` y `POST /api/quotes` (api.md §8.1). */
export interface QuoteRequest {
  clientId: number;
  vehicleType: VehicleType;
  manufacturingYear: number;
  brandId: number;
  modelId: number;
  versionId: number;
  usageType: VehicleUsageType;
  patent: string;
  planId: number;
  insuredAmount: number;
}

/** Zona tarifaria usada en el cálculo (api.md §8.2). */
export interface Zone {
  code: string;
  name: string;
  factor: number;
}

/**
 * Response de `POST /api/quotes/calculate` (api.md §8.2). Todo lo calcula el backend;
 * `driverAge` es `null` cuando el cliente es una empresa.
 */
export interface QuoteCalculation {
  insuredAmount: number;
  planRate: number;
  basePremium: number;
  zone: Zone;
  usageFactor: number;
  vehicleAgeYears: number;
  vehicleAgeFactor: number;
  driverAge: number | null;
  driverAgeFactor: number;
  premium: number;
  taxRate: number;
  taxes: number;
  totalPrice: number;
  currency: string;
}

/** Vehículo de una cotización guardada (api.md §8.5). */
export interface QuoteVehicle {
  vehicleType: VehicleType;
  manufacturingYear: number;
  brandName: string;
  modelName: string;
  versionName: string;
  patent: string;
  usageType: VehicleUsageType;
}

/** Póliza emitida a partir de una cotización (api.md §8.5); `null` si no se emitió. */
export interface QuotePolicy {
  id: number;
  policyNumber: string;
}

/** Response de `POST /api/quotes` y `GET /api/quotes/{id}` (api.md §8.3, §8.5). */
export interface QuoteDetail {
  id: number;
  quoteNumber: string;
  status: QuoteStatus;
  createdAt: string;
  validUntil: string;
  client: Client;
  vehicle: QuoteVehicle;
  planName: string;
  calculation: QuoteCalculation;
  policy: QuotePolicy | null;
}

/** Elemento de `GET /api/quotes` (api.md §8.4). */
export interface QuoteSummary {
  id: number;
  quoteNumber: string;
  createdAt: string;
  clientName: string;
  vehicleType: VehicleType;
  brandName: string;
  modelName: string;
  planName: string;
  totalPrice: number;
  currency: string;
  status: QuoteStatus;
}
