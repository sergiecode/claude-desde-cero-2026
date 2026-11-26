import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  linkedSignal,
  signal,
} from '@angular/core';
import { form, FormField, required, validate } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { CLIENT_SEARCH_ERROR_CODES, ClientApi } from '../../core/api/client-api.service';
import { asProblemDetails } from '../../core/errors/error.interceptor';
import { Client, DocumentType } from '../../core/models/client.model';
import { DOCUMENT_TYPE_LABELS } from '../../shared/labels';

const REQUIRED_MESSAGE = 'Completá este campo';
const INVALID_DOCUMENT_MESSAGE = 'El número de documento no es válido';

/**
 * DNI: 7 u 8 dígitos (C3). CUIT: 11 dígitos, con o sin guiones (C4);
 * el dígito verificador lo valida el backend.
 */
const DOCUMENT_PATTERNS: Record<DocumentType, RegExp> = {
  DNI: /^\d{7,8}$/,
  CUIT: /^\d{2}-?\d{8}-?\d$/,
};

interface QuoteFormModel {
  documentType: DocumentType | '';
  documentNumber: string;
}

/** Resultado de la búsqueda del cliente para el documento ingresado. */
interface ClientSearchState {
  client: Client | null;
  /** `detail` de CLIENT_NOT_FOUND, INVALID_DOCUMENT o CLIENT_UNDERAGE. */
  error: string;
}

const EMPTY_SEARCH: ClientSearchState = { client: null, error: '' };

/**
 * Nueva cotización (frontend.md §5.4). Bloque 1 (Cliente) completo; los bloques 2 y 3
 * se completan en las etapas siguientes y quedan deshabilitados sin cliente (C1).
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

  protected readonly documentTypes = (Object.keys(DOCUMENT_TYPE_LABELS) as DocumentType[]).map(
    (value) => ({ value, label: DOCUMENT_TYPE_LABELS[value] }),
  );

  protected readonly model = signal<QuoteFormModel>({ documentType: '', documentNumber: '' });

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
  });

  protected readonly canSearch = computed(
    () =>
      this.quoteForm.documentType().valid() &&
      this.quoteForm.documentNumber().valid() &&
      !this.searching(),
  );

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
