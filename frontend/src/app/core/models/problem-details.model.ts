/** Error de campo dentro de un Problem Details (api.md §2). */
export interface FieldError {
  field: string;
  message: string;
}

/** Formato de error de la API: Problem Details con `code` y `errors` (api.md §2). */
export interface ProblemDetails {
  type: string;
  title: string;
  status: number;
  detail: string;
  code: string;
  errors: FieldError[];
}
