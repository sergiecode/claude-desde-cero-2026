import { HttpClient, HttpContext } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { HANDLED_ERROR_CODES } from '../errors/handled-error-codes';
import { LoginRequest, LoginResponse, Producer } from '../models/auth.model';

export const LOGIN_URL = '/api/auth/login';

const TOKEN_KEY = 'auth.token';
const EXPIRES_AT_KEY = 'auth.expiresAt';
const PRODUCER_KEY = 'auth.producer';

/** Login, sesión en `sessionStorage` y productor actual (frontend.md §4). */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly producerState = signal<Producer | null>(readProducer());

  /** Productor de la sesión actual, o `null` si no hay sesión. */
  readonly producer = this.producerState.asReadonly();

  /** Nombre y apellido del productor actual. */
  readonly producerName = computed(() => {
    const producer = this.producerState();
    return producer ? `${producer.firstName} ${producer.lastName}` : '';
  });

  /**
   * Hace el login y guarda la sesión. La pantalla de login maneja `INVALID_CREDENTIALS`,
   * así que el errorInterceptor no lo muestra en el snackbar.
   */
  async login(request: LoginRequest): Promise<Producer> {
    const response = await firstValueFrom(
      this.http.post<LoginResponse>(LOGIN_URL, request, {
        context: new HttpContext().set(HANDLED_ERROR_CODES, ['INVALID_CREDENTIALS']),
      }),
    );
    sessionStorage.setItem(TOKEN_KEY, response.token);
    sessionStorage.setItem(EXPIRES_AT_KEY, response.expiresAt);
    sessionStorage.setItem(PRODUCER_KEY, JSON.stringify(response.producer));
    this.producerState.set(response.producer);
    return response.producer;
  }

  /** Token guardado, o `null` si no hay sesión. */
  getToken(): string | null {
    return sessionStorage.getItem(TOKEN_KEY);
  }

  /** `true` si hay token y su `expiresAt` todavía no pasó. */
  isAuthenticated(): boolean {
    const token = this.getToken();
    const expiresAt = sessionStorage.getItem(EXPIRES_AT_KEY);
    if (!token || !expiresAt) {
      return false;
    }
    const expiresAtMs = Date.parse(expiresAt);
    return !Number.isNaN(expiresAtMs) && expiresAtMs > Date.now();
  }

  /** Borra la sesión. */
  logout(): void {
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(EXPIRES_AT_KEY);
    sessionStorage.removeItem(PRODUCER_KEY);
    this.producerState.set(null);
  }
}

function readProducer(): Producer | null {
  const raw = sessionStorage.getItem(PRODUCER_KEY);
  if (!raw) {
    return null;
  }
  try {
    return JSON.parse(raw) as Producer;
  } catch {
    return null;
  }
}
