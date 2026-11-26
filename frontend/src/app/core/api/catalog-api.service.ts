import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CatalogItem, VehicleType, VehicleUsageType } from '../models/catalog.model';

export const CATALOGS_URL = '/api/catalogs';

/** Servicio HTTP de catálogos de vehículos (api.md §5). Los errores los muestra el errorInterceptor. */
@Injectable({ providedIn: 'root' })
export class CatalogApi {
  private readonly http = inject(HttpClient);

  vehicleTypes(): Promise<VehicleType[]> {
    return firstValueFrom(this.http.get<VehicleType[]>(`${CATALOGS_URL}/vehicle-types`));
  }

  years(vehicleType: VehicleType): Promise<number[]> {
    return firstValueFrom(
      this.http.get<number[]>(`${CATALOGS_URL}/years`, {
        params: new HttpParams().set('vehicleType', vehicleType),
      }),
    );
  }

  brands(vehicleType: VehicleType, year: number): Promise<CatalogItem[]> {
    return firstValueFrom(
      this.http.get<CatalogItem[]>(`${CATALOGS_URL}/brands`, {
        params: new HttpParams().set('vehicleType', vehicleType).set('year', year),
      }),
    );
  }

  models(vehicleType: VehicleType, year: number, brandId: number): Promise<CatalogItem[]> {
    return firstValueFrom(
      this.http.get<CatalogItem[]>(`${CATALOGS_URL}/models`, {
        params: new HttpParams()
          .set('vehicleType', vehicleType)
          .set('year', year)
          .set('brandId', brandId),
      }),
    );
  }

  versions(modelId: number): Promise<CatalogItem[]> {
    return firstValueFrom(
      this.http.get<CatalogItem[]>(`${CATALOGS_URL}/versions`, {
        params: new HttpParams().set('modelId', modelId),
      }),
    );
  }

  usageTypes(vehicleType: VehicleType): Promise<VehicleUsageType[]> {
    return firstValueFrom(
      this.http.get<VehicleUsageType[]>(`${CATALOGS_URL}/usage-types`, {
        params: new HttpParams().set('vehicleType', vehicleType),
      }),
    );
  }
}
