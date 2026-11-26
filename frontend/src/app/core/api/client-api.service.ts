import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { Client, DocumentType } from '../models/client.model';

export const CLIENT_SEARCH_URL = '/api/clients/search';

/** Errores de la búsqueda que muestra la pantalla debajo del campo (frontend.md §5.4, Bloque 1). */
export const CLIENT_SEARCH_ERROR_CODES = [
  'CLIENT_NOT_FOUND',
  'INVALID_DOCUMENT',
  'CLIENT_UNDERAGE',
] as const;

/** Servicio HTTP de clientes (api.md §4). */
@Injectable({ providedIn: 'root' })
export class ClientApi {
  private readonly http = inject(HttpClient);

  /**
   * Busca un cliente por tipo y número de documento. Los errores de
   * CLIENT_SEARCH_ERROR_CODES los maneja el componente: el errorInterceptor no los muestra.
   */
  search(documentType: DocumentType, documentNumber: string): Promise<Client> {
    return firstValueFrom(
      this.http.get<Client>(CLIENT_SEARCH_URL, {
        params: new HttpParams().set('documentType', documentType).set('documentNumber', documentNumber),
        context: new HttpContext().set(HANDLED_ERROR_CODES, CLIENT_SEARCH_ERROR_CODES),
      }),
    );
  }
}
