/** Tipo de documento del cliente (api.md §4, manual C2). */
export type DocumentType = 'DNI' | 'CUIT';

/** Response de `GET /api/clients/search` (api.md §4). En una empresa, los datos de persona son `null`. */
export interface Client {
  id: number;
  documentType: DocumentType;
  documentNumber: string;
  firstName: string | null;
  lastName: string | null;
  businessName: string | null;
  birthDate: string | null;
  age: number | null;
  email: string;
  phone: string;
  address: string;
  postalCode: string;
  city: string;
  province: string;
}
