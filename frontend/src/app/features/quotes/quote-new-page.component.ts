import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  resource,
  signal,
} from '@angular/core';
import { disabled, form, FormField, pattern, required, validate } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { CatalogApi } from '../../core/api/catalog-api.service';
import { CLIENT_SEARCH_ERROR_CODES, ClientApi } from '../../core/api/client-api.service';
import { PlanApi } from '../../core/api/plan-api.service';
import { asProblemDetails } from '../../core/errors/error.interceptor';
import { CatalogItem, VehicleType, VehicleUsageType } from '../../core/models/catalog.model';
import { Client, DocumentType } from '../../core/models/client.model';
import { InsurancePlan } from '../../core/models/plan.model';
import {
  DOCUMENT_TYPE_LABELS,
  VEHICLE_TYPE_LABELS,
  VEHICLE_USAGE_TYPE_LABELS,
} from '../../shared/labels';

const REQUIRED_MESSAGE = 'Completá este campo';
const INVALID_DOCUMENT_MESSAGE = 'El número de documento no es válido';
const INVALID_PATENT_MESSAGE = 'La patente debe tener el formato ABC123';

/** Patente: 3 letras y 3 números (V6). */
const PATENT_PATTERN = /^[A-Z]{3}\d{3}$/;

/**
 * DNI: 7 u 8 dígitos (C3). CUIT: 11 dígitos, con o sin guiones (C4);
 * el dígito verificador lo valida el backend.
 */
const DOCUMENT_PATTERNS: Record<DocumentType, RegExp> = {
  DNI: /^\d{7,8}$/,
  CUIT: /^\d{2}-?\d{8}-?\d$/,
};

/** Los campos vacíos se representan con `''` (sin `null`, frontend.md §2.1). */
interface QuoteFormModel {
  documentType: DocumentType | '';
  documentNumber: string;
  vehicleType: VehicleType | '';
  manufacturingYear: number | '';
  brandId: number | '';
  modelId: number | '';
  versionId: number | '';
  usageType: VehicleUsageType | '';
  patent: string;
  planId: number | '';
}

type VehicleSelectField = 'vehicleType' | 'manufacturingYear' | 'brandId' | 'modelId';

/**
 * V4: campos que se vacían cuando el productor cambia cada select de la cascada (frontend.md §5.4).
 * La patente no depende del tipo de vehículo y no se vacía.
 */
const CASCADE_DEPENDENTS: Record<VehicleSelectField, readonly (keyof QuoteFormModel)[]> = {
  vehicleType: ['manufacturingYear', 'brandId', 'modelId', 'versionId', 'usageType'],
  manufacturingYear: ['brandId', 'modelId', 'versionId'],
  brandId: ['modelId', 'versionId'],
  modelId: ['versionId'],
};

interface Option<T> {
  value: T;
  label: string;
}

/** Resultado de la búsqueda del cliente para el documento ingresado. */
interface ClientSearchState {
  client: Client | null;
  /** `detail` de CLIENT_NOT_FOUND, INVALID_DOCUMENT o CLIENT_UNDERAGE. */
  error: string;
}

const EMPTY_SEARCH: ClientSearchState = { client: null, error: '' };

/** Valor de un recurso de catálogo, o una lista vacía mientras carga, sin parámetros o con error. */
function optionsOf<T>(r: { hasValue(): boolean; value(): T[] | undefined }): T[] {
  return r.hasValue() ? (r.value() ?? []) : [];
}

/**
 * Nueva cotización (frontend.md §5.4). Bloque 1 (Cliente), bloque 2 (Vehículo, cascada de
 * catálogos) y bloque 3 (Plan; la suma asegurada se agrega en la etapa siguiente). Los bloques
 * 2 y 3 quedan deshabilitados sin cliente (C1).
 */
@Component({
  selector: 'app-quote-new-page',
  imports: [
    DatePipe,
    FormField,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './quote-new-page.component.html',
  styleUrl: './quote-new-page.component.scss',
})
export class QuoteNewPageComponent {
  private readonly clientApi = inject(ClientApi);
  private readonly catalogApi = inject(CatalogApi);
  private readonly planApi = inject(PlanApi);

  protected readonly documentTypes = (Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[]).map(
    (value) => ({ value, label: DOCUMENT_TYPE_LABELS[value] }),
  );

  protected readonly model = signal<QuoteFormModel>({
    documentType: '',
    documentNumber: '',
    vehicleType: '',
    manufacturingYear: '',
    brandId: '',
    modelId: '',
    versionId: '',
    usageType: '',
    patent: '',
    planId: '',
  });

  /** Documento ingresado; cuando cambia, se descarta el resultado de la búsqueda anterior. */
  private readonly documentKey = computed(
    () => `${this.model().documentType}|${this.model().documentNumber}`,
  );

  /** Se reinicia (sin cliente y sin error) cada vez que cambia el tipo o el número de documento. */
  private readonly search = linkedSignal<string, ClientSearchState>({
    source: this.documentKey,
    computation: () => EMPTY_SEARCH,
  });

  protected readonly searching = signal(false);

  protected readonly client = computed(() => this.search().client);

  /** C1: los bloques de vehículo y plan se habilitan solo con un cliente encontrado. */
  protected readonly clientFound = computed(() => this.client() !== null);

  // Bloque 2 — Vehículo: cada catálogo se pide cuando el campo anterior tiene valor (V4).
  private readonly vehicleTypesResource = resource({
    loader: () => this.catalogApi.vehicleTypes(),
  });

  private readonly yearsResource = resource({
    params: () => this.model().vehicleType || undefined,
    loader: ({ params }) => this.catalogApi.years(params),
  });

  private readonly brandsResource = resource({
    params: () => {
      const { vehicleType, manufacturingYear } = this.model();
      return vehicleType && manufacturingYear !== ''
        ? { vehicleType, year: manufacturingYear }
        : undefined;
    },
    loader: ({ params }) => this.catalogApi.brands(params.vehicleType, params.year),
  });

  private readonly modelsResource = resource({
    params: () => {
      const { vehicleType, manufacturingYear, brandId } = this.model();
      return vehicleType && manufacturingYear !== '' && brandId !== ''
        ? { vehicleType, year: manufacturingYear, brandId }
        : undefined;
    },
    loader: ({ params }) => this.catalogApi.models(params.vehicleType, params.year, params.brandId),
  });

  private readonly versionsResource = resource({
    params: () => {
      const { modelId } = this.model();
      return modelId !== '' ? modelId : undefined;
    },
    loader: ({ params }) => this.catalogApi.versions(params),
  });

  private readonly usageTypesResource = resource({
    params: () => this.model().vehicleType || undefined,
    loader: ({ params }) => this.catalogApi.usageTypes(params),
  });

  protected readonly vehicleTypes = computed<Option<VehicleType>[]>(() =>
    optionsOf(this.vehicleTypesResource).map((value) => ({
      value,
      label: VEHICLE_TYPE_LABELS[value],
    })),
  );
  protected readonly years = computed<number[]>(() => optionsOf(this.yearsResource));
  protected readonly brands = computed<CatalogItem[]>(() => optionsOf(this.brandsResource));
  protected readonly models = computed<CatalogItem[]>(() => optionsOf(this.modelsResource));
  protected readonly versions = computed<CatalogItem[]>(() => optionsOf(this.versionsResource));
  protected readonly usageTypes = computed<Option<VehicleUsageType>[]>(() =>
    optionsOf(this.usageTypesResource).map((value) => ({
      value,
      label: VEHICLE_USAGE_TYPE_LABELS[value],
    })),
  );

  // Bloque 3 — Plan: los mismos planes para todos los tipos de vehículo (P2), cargados al iniciar.
  private readonly plansResource = resource({
    loader: () => this.planApi.list(),
  });

  protected readonly plans = computed<InsurancePlan[]>(() => optionsOf(this.plansResource));

  /** Plan elegido, para mostrar solo su nombre en el select cerrado. */
  protected readonly selectedPlan = computed(() => {
    const planId = this.model().planId;
    return this.plans().find((plan) => plan.id === planId);
  });

  protected readonly quoteForm = form(this.model, (f) => {
    required(f.documentType, { message: REQUIRED_MESSAGE });
    required(f.documentNumber, { message: REQUIRED_MESSAGE });
    validate(f.documentNumber, ({ value, valueOf }) => {
      const type = valueOf(f.documentType);
      const number = value().trim();
      if (!type || !number || DOCUMENT_PATTERNS[type].test(number)) {
        return null;
      }
      return { kind: 'documentFormat', message: INVALID_DOCUMENT_MESSAGE };
    });
    // Error de la búsqueda: se muestra en el mat-error del número hasta que cambie el documento.
    validate(f.documentNumber, () => {
      const error = this.search().error;
      return error ? { kind: 'clientSearch', message: error } : null;
    });

    // Bloque 2: todo deshabilitado sin cliente (C1); cada select, hasta que el anterior tenga valor.
    const noClient = () => !this.clientFound();
    disabled(f.vehicleType, noClient);
    disabled(f.manufacturingYear, ({ valueOf }) => noClient() || valueOf(f.vehicleType) === '');
    disabled(f.brandId, ({ valueOf }) => noClient() || valueOf(f.manufacturingYear) === '');
    disabled(f.modelId, ({ valueOf }) => noClient() || valueOf(f.brandId) === '');
    disabled(f.versionId, ({ valueOf }) => noClient() || valueOf(f.modelId) === '');
    disabled(f.usageType, ({ valueOf }) => noClient() || valueOf(f.vehicleType) === '');
    disabled(f.patent, noClient);

    required(f.vehicleType, { message: REQUIRED_MESSAGE });
    required(f.manufacturingYear, { message: REQUIRED_MESSAGE });
    required(f.brandId, { message: REQUIRED_MESSAGE });
    required(f.modelId, { message: REQUIRED_MESSAGE });
    required(f.versionId, { message: REQUIRED_MESSAGE });
    required(f.usageType, { message: REQUIRED_MESSAGE });
    required(f.patent, { message: REQUIRED_MESSAGE });
    pattern(f.patent, PATENT_PATTERN, { message: INVALID_PATENT_MESSAGE });

    // Bloque 3: un único plan por cotización (P1), deshabilitado sin cliente (C1).
    disabled(f.planId, noClient);
    required(f.planId, { message: REQUIRED_MESSAGE });
  });

  protected readonly canSearch = computed(
    () =>
      this.quoteForm.documentType().valid() &&
      this.quoteForm.documentNumber().valid() &&
      !this.searching(),
  );

  /**
   * V4: el productor cambió un select del vehículo; se vacían los que dependen de él.
   * Responde al evento del usuario (no a un `effect`), así que no hay escrituras en cadena.
   */
  protected clearDependents(field: VehicleSelectField): void {
    const cleared = Object.fromEntries(CASCADE_DEPENDENTS[field].map((key) => [key, '']));
    this.model.update((m) => ({ ...m, ...cleared }));
  }

  /**
   * V6: la patente se pasa a mayúsculas mientras se escribe. Se actualizan el input y el modelo,
   * así el resultado es el mismo sin importar si `FormField` procesa el evento antes o después.
   */
  protected uppercasePatent(event: Event): void {
    const input = event.target as HTMLInputElement;
    const upper = input.value.toUpperCase();
    if (input.value !== upper) {
      const { selectionStart, selectionEnd } = input;
      input.value = upper;
      input.setSelectionRange(selectionStart, selectionEnd);
    }
    this.model.update((m) => (m.patent === upper ? m : { ...m, patent: upper }));
  }

  protected async searchClient(): Promise<void> {
    const { documentType, documentNumber } = this.model();
    if (!this.canSearch() || !documentType) {
      return;
    }
    const key = this.documentKey();
    this.searching.set(true);
    try {
      const client = await this.clientApi.search(documentType, documentNumber.trim());
      if (this.documentKey() === key) {
        this.search.set({ client, error: '' });
      }
    } catch (error) {
      const problem = error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
      const handled: readonly string[] = CLIENT_SEARCH_ERROR_CODES;
      if (problem && handled.includes(problem.code) && this.documentKey() === key) {
        this.search.set({ client: null, error: problem.detail });
        this.quoteForm.documentNumber().markAsTouched();
      }
      // El resto de los errores los muestra el errorInterceptor.
    } finally {
      this.searching.set(false);
    }
  }
}
