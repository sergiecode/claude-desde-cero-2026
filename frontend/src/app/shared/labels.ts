import { DocumentType } from '../core/models/client.model';

/** Etiquetas en español de los enums (frontend.md §6). Se completan en cada etapa. */
export const DOCUMENT_TYPE_LABELS: Record<DocumentType, string> = {
  DNI: 'DNI',
  CUIT: 'CUIT',
};
