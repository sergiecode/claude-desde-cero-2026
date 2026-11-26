/** Response de `GET /api/insured-values` (api.md §7). Los montos los calcula el backend (S2, S3). */
export interface InsuredValue {
  minimumAmount: number;
  suggestedAmount: number;
  maximumAmount: number;
  currency: string;
}
