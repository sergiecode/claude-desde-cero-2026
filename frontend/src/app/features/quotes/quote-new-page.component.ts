import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  LOCALE_ID,
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
import {
  INSURED_VALUE_ERROR_CODES,
  InsuredValueApi,
} from '../../core/api/insured-value-api.service';
import { PlanApi } from '../../core/api/plan-api.service';
import { QuoteApi } from '../../core/api/quote-api.service';
import { ErrorMessageService } from '../../core/errors/error-message.service';
import {
  asProblemDetails,
  UNEXPECTED_ERROR_MESSAGE,
} from '../../core/errors/error.interceptor';
import { CatalogItem, VehicleType, VehicleUsageType } from '../../core/models/catalog.model';
import { Client, DocumentType } from '../../core/models/client.model';
import { InsuredValue } from '../../core/models/insured-value.model';
import { InsurancePlan } from '../../core/models/plan.model';
import { QuoteCalculation, QuoteRequest } from '../../core/models/quote.model';
import { CalculationBreakdownComponent } from '../../shared/components/calculation-breakdown.component';
import { formatMoney } from '../../shared/format';
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
  /**
   * Suma asegurada tal como está en el `<input type="number">`. Se modela como texto para que
   * el vacío sea `''` (con un modelo numérico, Signal Forms escribe `null` al borrar el campo).
   */
  insuredAmount: string;
}

/**
 * Campos del formulario que pueden recibir un error de `POST /api/quotes/calculate`
 * (`errors[].field` de api.md §2 coincide con el nombre del campo de `QuoteRequest`).
 * Un error de otro campo (por ejemplo `clientId`) se muestra en el snackbar.
 */
const SERVER_ERROR_FIELDS = [
  'vehicleType',
  'manufacturingYear',
  'brandId',
  'modelId',
  'versionId',
  'usageType',
  'patent',
  'planId',
  'insuredAmount',
] as const;

type ServerErrorField = (typeof SERVER_ERROR_FIELDS)[number];

function isServerErrorField(field: string): field is ServerErrorField {
  return (SERVER_ERROR_FIELDS as readonly string[]).includes(field);
}

type VehicleSelectField = 'vehicleType' | 'manufacturingYear' | 'brandId' | 'modelId';

/**
 * V4: campos que se vacían cuando el productor cambia cada select de la cascada (frontend.md §5.4).
 * La patente no depende del tipo de vehículo y no se vacía.
 */
const CASCADE_DEPENDENTS: Record<VehicleSelectField, readonly (keyof QuoteFormModel)[]> = {
  vehicleType: ['manufacturingYear', 'brandId', 'modelId', 'versionId', 'usageType', 'insuredAmount'],
  manufacturingYear: ['brandId', 'modelId', 'versionId', 'insuredAmount'],
  brandId: ['modelId', 'versionId', 'insuredAmount'],
  modelId: ['versionId', 'insuredAmount'],
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

/** Rango de la suma asegurada devuelto por el backend para el año y la versión elegidos. */
interface ValuationState {
  range: InsuredValue | null;
  /** `detail` de VALUATION_NOT_FOUND (S6). */
  error: string;
}

const EMPTY_VALUATION: ValuationState = { range: null, error: '' };

/** Errores de campo devueltos por el backend al cotizar, por nombre de campo. */
type ServerErrors = Partial<Record<ServerErrorField, string>>;

/** Valor de un recurso de catálogo, o una lista vacía mientras carga, sin parámetros o con error. */
function optionsOf<T>(r: { hasValue(): boolean; value(): T[] | undefined }): T[] {
  return r.hasValue() ? (r.value() ?? []) : [];
}

/**
 * Nueva cotización (frontend.md §5.4). Bloque 1 (Cliente), bloque 2 (Vehículo, cascada de
 * catálogos) y bloque 3 (Plan y suma asegurada). Los bloques 2 y 3 quedan deshabilitados sin
 * cliente (C1).
 */
@Component({
  selector: 'app-quote-new-page',
  imports: [
    CalculationBreakdownComponent,
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
  private readonly insuredValueApi = inject(InsuredValueApi);
  private readonly quoteApi = inject(QuoteApi);
  private readonly messages = inject(ErrorMessageService);
  private readonly locale = inject(LOCALE_ID);

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
    insuredAmount: '',
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

  /** Año y versión elegidos: la valuación depende de los dos (S2). */
  private readonly valuationKey = computed(
    () => `${this.model().manufacturingYear}|${this.model().versionId}`,
  );

  /**
   * V4: se reinicia (sin rango y sin error) cada vez que cambian el año o la versión, incluso
   * cuando los vacía la cascada al cambiar el tipo, el año, la marca o el modelo.
   */
  private readonly valuation = linkedSignal<string, ValuationState>({
    source: this.valuationKey,
    computation: () => EMPTY_VALUATION,
  });

  /** "Mínimo $ X — Máximo $ Y" con los montos del backend, sin calcular nada (S3). */
  protected readonly rangeText = computed(() => {
    const range = this.valuation().range;
    return range
      ? `Mínimo ${this.money(range.minimumAmount)} — Máximo ${this.money(range.maximumAmount)}`
      : '';
  });

  /** Todos los valores del formulario: cambia solo cuando cambia algún valor. */
  private readonly formKey = computed(() => JSON.stringify(this.model()));

  /**
   * Resultado de "Cotizar" (frontend.md §5.4). Q1: cualquier cambio en el formulario lo borra,
   * para que nunca quede visible un precio que no corresponde a los datos actuales.
   */
  protected readonly calculation = linkedSignal<string, QuoteCalculation | null>({
    source: this.formKey,
    computation: () => null,
  });

  /** Errores de campo del último cálculo; se borran con cualquier cambio en el formulario. */
  private readonly serverErrors = linkedSignal<string, ServerErrors>({
    source: this.formKey,
    computation: () => ({}),
  });

  protected readonly calculating = signal(false);

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

    // Suma asegurada: el rango sale del backend (S3, S5); sin valuación no se puede cotizar (S6).
    disabled(f.insuredAmount, noClient);
    // Sin valuación (S6) el único mensaje es el `detail` del backend, no "Completá este campo".
    required(f.insuredAmount, {
      message: REQUIRED_MESSAGE,
      when: () => !this.valuation().error,
    });
    validate(f.insuredAmount, () => {
      const error = this.valuation().error;
      return error ? { kind: 'valuationNotFound', message: error } : null;
    });
    validate(f.insuredAmount, ({ value }) => {
      const { range, error } = this.valuation();
      if (value() === '' || error) {
        return null;
      }
      if (!range) {
        // Sin rango del backend (versión sin elegir o valuación en curso) el campo no es válido.
        return { kind: 'valuationMissing', message: REQUIRED_MESSAGE };
      }
      const amount = Number(value());
      if (amount >= range.minimumAmount && amount <= range.maximumAmount) {
        return null;
      }
      return {
        kind: 'insuredAmountOutOfRange',
        message: `La suma asegurada debe estar entre ${this.money(range.minimumAmount)} y ${this.money(range.maximumAmount)}`,
      };
    });

    // Errores de campo que devolvió el backend al cotizar (api.md §2), hasta que cambie el formulario.
    const serverError = (field: ServerErrorField) => () => {
      const message = this.serverErrors()[field];
      return message ? { kind: 'server', message } : null;
    };
    validate(f.vehicleType, serverError('vehicleType'));
    validate(f.manufacturingYear, serverError('manufacturingYear'));
    validate(f.brandId, serverError('brandId'));
    validate(f.modelId, serverError('modelId'));
    validate(f.versionId, serverError('versionId'));
    validate(f.usageType, serverError('usageType'));
    validate(f.patent, serverError('patent'));
    validate(f.planId, serverError('planId'));
    validate(f.insuredAmount, serverError('insuredAmount'));
  });

  protected readonly canSearch = computed(
    () =>
      this.quoteForm.documentType().valid() &&
      this.quoteForm.documentNumber().valid() &&
      !this.searching(),
  );

  /**
   * "Cotizar": cliente encontrado, valuación disponible (S6) y formulario válido; deshabilitado
   * mientras hay una llamada en curso (frontend.md §5.4).
   */
  protected readonly canCalculate = computed(
    () =>
      this.clientFound() &&
      this.valuation().range !== null &&
      this.quoteForm().valid() &&
      !this.calculating(),
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
   * S2/S6: el productor eligió una versión; se vacía la suma asegurada y se pide la valuación.
   * Si llega, se carga el sugerido; si no hay valuación, el campo queda inválido con el `detail`.
   */
  protected async loadInsuredValue(): Promise<void> {
    this.model.update((m) => ({ ...m, insuredAmount: '' }));
    const { versionId, manufacturingYear } = this.model();
    if (versionId === '' || manufacturingYear === '') {
      return;
    }
    const key = this.valuationKey();
    try {
      const range = await this.insuredValueApi.get(versionId, manufacturingYear);
      if (this.valuationKey() === key) {
        this.valuation.set({ range, error: '' });
        this.model.update((m) => ({ ...m, insuredAmount: String(range.suggestedAmount) }));
      }
    } catch (error) {
      const problem = error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
      const handled: readonly string[] = INSURED_VALUE_ERROR_CODES;
      if (problem && handled.includes(problem.code) && this.valuationKey() === key) {
        this.valuation.set({ range: null, error: problem.detail });
        this.quoteForm.insuredAmount().markAsTouched();
      }
      // El resto de los errores los muestra el errorInterceptor.
    }
  }

  /** Monto con el formato es-AR del manual: `$ 8.640.000,00`. */
  private money(amount: number): string {
    return formatMoney(amount, this.locale);
  }

  /**
   * Pide el precio al backend (api.md §8.2) y muestra el resultado tal como llega.
   * Si el formulario cambió durante la llamada, el resultado se descarta (Q1).
   */
  protected async calculate(): Promise<void> {
    const request = this.buildRequest();
    if (!this.canCalculate() || !request) {
      return;
    }
    const key = this.formKey();
    this.calculating.set(true);
    try {
      const result = await this.quoteApi.calculate(request);
      if (this.formKey() === key) {
        this.calculation.set(result);
      }
    } catch (error) {
      this.handleCalculateError(error, key);
    } finally {
      this.calculating.set(false);
    }
  }

  /** `QuoteRequest` con los datos del formulario, o `null` si falta alguno. */
  private buildRequest(): QuoteRequest | null {
    const client = this.client();
    const m = this.model();
    if (
      !client ||
      m.vehicleType === '' ||
      m.manufacturingYear === '' ||
      m.brandId === '' ||
      m.modelId === '' ||
      m.versionId === '' ||
      m.usageType === '' ||
      m.planId === '' ||
      m.insuredAmount === ''
    ) {
      return null;
    }
    return {
      clientId: client.id,
      vehicleType: m.vehicleType,
      manufacturingYear: m.manufacturingYear,
      brandId: m.brandId,
      modelId: m.modelId,
      versionId: m.versionId,
      usageType: m.usageType,
      patent: m.patent,
      planId: m.planId,
      insuredAmount: Number(m.insuredAmount),
    };
  }

  /**
   * Errores de "Cotizar" (frontend.md §4 y §5.4): los que traen `errors` de campos del formulario
   * se muestran en esos campos; el resto, en el snackbar. Sin conexión y 401 ya los resolvió
   * el errorInterceptor.
   */
  private handleCalculateError(error: unknown, key: string): void {
    if (error instanceof HttpErrorResponse && (error.status === 0 || error.status === 401)) {
      return;
    }
    const problem = error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
    const allErrors = problem?.errors ?? [];
    const fieldErrors = allErrors.filter((e) => isServerErrorField(e.field));

    if (fieldErrors.length && this.formKey() === key) {
      const errors: ServerErrors = {};
      for (const { field, message } of fieldErrors) {
        if (isServerErrorField(field) && !errors[field]) {
          errors[field] = message;
        }
      }
      this.serverErrors.set(errors);
      for (const field of Object.keys(errors) as ServerErrorField[]) {
        this.quoteForm[field]().markAsTouched();
      }
    }
    if (!fieldErrors.length || fieldErrors.length < allErrors.length) {
      this.messages.show(problem?.detail ?? UNEXPECTED_ERROR_MESSAGE);
    }
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
