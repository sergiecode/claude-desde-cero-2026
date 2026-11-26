/** Productor autenticado (api.md §3). */
export interface Producer {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

/** Request de `POST /api/auth/login` (api.md §3). */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Response de `POST /api/auth/login` (api.md §3). */
export interface LoginResponse {
  token: string;
  expiresAt: string;
  producer: Producer;
}
