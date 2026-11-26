import { HttpContextToken } from '@angular/common/http';

/**
 * Códigos de error (`code` de api.md §2.1) que el componente que hace la llamada
 * maneja por su cuenta: el errorInterceptor no los muestra en el snackbar.
 * Con `'*'` el componente maneja todos los errores de esa llamada.
 */
export const HANDLED_ERROR_CODES = new HttpContextToken<readonly string[]>(() => []);
