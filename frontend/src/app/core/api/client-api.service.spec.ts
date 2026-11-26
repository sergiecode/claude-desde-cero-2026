import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ErrorMessageService } from '../errors/error-message.service';
import { errorInterceptor } from '../errors/error.interceptor';
import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { Client } from '../models/client.model';
import { CLIENT_SEARCH_URL, ClientApi } from './client-api.service';

const CLIENT: Client = {
  id: 1,
  documentType: 'DNI',
  documentNumber: '30123456',
  firstName: 'Lucía',
  lastName: 'Fernández',
  businessName: null,
  birthDate: '2004-03-15',
  age: 22,
  email: 'lucia.fernandez@mail.test',
  phone: '11 5555-0101',
  address: 'Av. Rivadavia 5000',
  postalCode: 'C1406',
  city: 'CABA',
  province: 'CABA',
};

describe('ClientApi', () => {
  let api: ClientApi;
  let httpTesting: HttpTestingController;
  let shown: string[];

  beforeEach(() => {
    shown = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
      ],
    });
    api = TestBed.inject(ClientApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  it('busca con GET /api/clients/search?documentType&documentNumber y devuelve el cliente', async () => {
    const result = api.search('DNI', '30123456');

    const req = httpTesting.expectOne(
      (r) => r.url === CLIENT_SEARCH_URL && r.method === 'GET',
    );
    expect(req.request.params.get('documentType')).toBe('DNI');
    expect(req.request.params.get('documentNumber')).toBe('30123456');
    expect(req.request.context.get(HANDLED_ERROR_CODES)).toEqual([
      'CLIENT_NOT_FOUND',
      'INVALID_DOCUMENT',
      'CLIENT_UNDERAGE',
    ]);
    req.flush(CLIENT);

    await expect(result).resolves.toEqual(CLIENT);
  });

  it.each([
    [404, 'CLIENT_NOT_FOUND', 'Cliente no encontrado'],
    [400, 'INVALID_DOCUMENT', 'El número de documento no es válido'],
    [422, 'CLIENT_UNDERAGE', 'El cliente debe ser mayor de 18 años'],
  ])('%s %s: rechaza la promesa y el errorInterceptor no muestra el snackbar', async (status, code, detail) => {
    const result = api.search('CUIT', '30-71234567-1');

    httpTesting
      .expectOne((r) => r.url === CLIENT_SEARCH_URL)
      .flush(
        { type: 'about:blank', title: 'Error', status, detail, code, errors: [] },
        { status, statusText: 'Error' },
      );

    await expect(result).rejects.toBeDefined();
    expect(shown).toEqual([]);
  });
});
