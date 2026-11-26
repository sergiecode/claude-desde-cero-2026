import { Client } from './client.model';
import { QuoteVehicle } from './quote.model';

/** Estado de una póliza (api.md, data-model.md §2). */
export type PolicyStatus = 'ACTIVE' | 'EXPIRED';

/** Request de `POST /api/quotes/{id}/issue` (api.md §8.6). `startDate` en `yyyy-MM-dd`. */
export interface IssueRequest {
  startDate: string;
}

/**
 * Response de `POST /api/quotes/{id}/issue` y `GET /api/policies/{id}` (api.md §8.6, §9.1).
 * Fechas `startDate` y `endDate` en `yyyy-MM-dd`; importes tal como los calcula el backend.
 */
export interface PolicyDetail {
  id: number;
  policyNumber: string;
  status: PolicyStatus;
  issuedAt: string;
  startDate: string;
  endDate: string;
  quoteId: number;
  quoteNumber: string;
  client: Client;
  vehicle: QuoteVehicle;
  planName: string;
  insuredAmount: number;
  premium: number;
  taxes: number;
  totalPrice: number;
  currency: string;
}
