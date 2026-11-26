import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CatalogItem } from '../models/catalog.model';
import { CATALOGS_URL, CatalogApi } from './catalog-api.service';

const ITEMS: CatalogItem[] = [
  { id: 3, name: 'Gol' },
  { id: 4, name: 'Polo' },
];

describe('CatalogApi', () => {
  let api: CatalogApi;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(CatalogApi);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  function expectGet(path: string, params: Record<string, string>) {
    const req = httpTesting.expectOne((r) => r.url === `${CATALOGS_URL}/${path}`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().sort()).toEqual(Object.keys(params).sort());
    for (const [key, value] of Object.entries(params)) {
      expect(req.request.params.get(key)).toBe(value);
    }
    return req;
  }

  it('vehicleTypes: GET /api/catalogs/vehicle-types sin parámetros', async () => {
    const result = api.vehicleTypes();
    expectGet('vehicle-types', {}).flush(['AUTO', 'MOTO', 'CAMIONETA', 'CAMION']);
    await expect(result).resolves.toEqual(['AUTO', 'MOTO', 'CAMIONETA', 'CAMION']);
  });

  it('years: GET /api/catalogs/years?vehicleType', async () => {
    const result = api.years('AUTO');
    expectGet('years', { vehicleType: 'AUTO' }).flush([2026, 2025]);
    await expect(result).resolves.toEqual([2026, 2025]);
  });

  it('brands: GET /api/catalogs/brands?vehicleType&year', async () => {
    const result = api.brands('MOTO', 2020);
    expectGet('brands', { vehicleType: 'MOTO', year: '2020' }).flush(ITEMS);
    await expect(result).resolves.toEqual(ITEMS);
  });

  it('models: GET /api/catalogs/models?vehicleType&year&brandId', async () => {
    const result = api.models('AUTO', 2020, 2);
    expectGet('models', { vehicleType: 'AUTO', year: '2020', brandId: '2' }).flush(ITEMS);
    await expect(result).resolves.toEqual(ITEMS);
  });

  it('versions: GET /api/catalogs/versions?modelId', async () => {
    const result = api.versions(3);
    expectGet('versions', { modelId: '3' }).flush(ITEMS);
    await expect(result).resolves.toEqual(ITEMS);
  });

  it('usageTypes: GET /api/catalogs/usage-types?vehicleType', async () => {
    const result = api.usageTypes('CAMION');
    expectGet('usage-types', { vehicleType: 'CAMION' }).flush(['COMERCIAL', 'TRANSPORTE_CARGA']);
    await expect(result).resolves.toEqual(['COMERCIAL', 'TRANSPORTE_CARGA']);
  });
});
