/** Response de `GET /api/insurance-plans` (api.md §6). La tasa no se expone. */
export interface InsurancePlan {
  id: number;
  code: string;
  name: string;
  description: string;
}
