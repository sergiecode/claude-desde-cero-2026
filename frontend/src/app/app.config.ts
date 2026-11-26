import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { provideHttpClient, withFetch } from '@angular/common/http';
import {
  ApplicationConfig,
  DEFAULT_CURRENCY_CODE,
  LOCALE_ID,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

registerLocaleData(localeEsAr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // Los interceptores (auth y errores) se agregan en la etapa 3.
    provideHttpClient(withFetch()),
    { provide: LOCALE_ID, useValue: 'es-AR' },
    { provide: DEFAULT_CURRENCY_CODE, useValue: 'ARS' },
  ],
};
