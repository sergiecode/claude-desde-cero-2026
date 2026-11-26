import { VehicleType, VehicleUsageType } from './catalog.model';

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
