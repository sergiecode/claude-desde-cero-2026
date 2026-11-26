import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { HttpErrorResponse } from '@angular/common/http';
import { LOCALE_ID } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter, Router } from '@angular/router';

import { QuoteApi } from '../../core/api/quote-api.service';
import { ErrorMessageService } from '../../core/errors/error-message.service';
import { PolicyDetail } from '../../core/models/policy.model';
import { toLocalIsoDate } from '../../shared/format';
import { IssuePolicyDialogComponent } from './issue-policy-dialog.component';

registerLocaleData(localeEsAr);

const POLICY = { id: 4, policyNumber: 'POL-2026-000004' } as PolicyDetail;

function problem(status: number, code: string, detail: string): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    statusText: 'Error',
    error: { type: 'about:blank', title: 'Error', status, detail, code, errors: [] },
  });
}

/** Hoy + `days` en fecha local, a las 00:00. */
function daysFromToday(days: number): Date {
  const now = new Date();
  return new Date(now.getFullYear(), now.getMonth(), now.getDate() + days);
}

describe('IssuePolicyDialogComponent', () => {
  let fixture: ComponentFixture<IssuePolicyDialogComponent>;
  let root: HTMLElement;
  let issue: ReturnType<typeof vi.fn>;
  let dialogRef: { close: ReturnType<typeof vi.fn> };
  let snackBar: { open: ReturnType<typeof vi.fn> };
  let shown: string[];
  let navigate: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    issue = vi.fn();
    dialogRef = { close: vi.fn() };
    snackBar = { open: vi.fn() };
    shown = [];
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: MAT_DIALOG_DATA, useValue: { quoteId: 1 } },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: QuoteApi, useValue: { issue } },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: ErrorMessageService, useValue: { show: (m: string) => shown.push(m) } },
        { provide: LOCALE_ID, useValue: 'es-AR' },
      ],
    });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true) as never;
    fixture = TestBed.createComponent(IssuePolicyDialogComponent);
    root = fixture.nativeElement as HTMLElement;
    await fixture.whenStable();
  });

  function dateInput(): HTMLInputElement {
    return root.querySelector<HTMLInputElement>('[data-field="startDate"]')!;
  }

  function confirmButton(): HTMLButtonElement {
    return root.querySelector<HTMLButtonElement>('[data-action="confirm"]')!;
  }

  function dialogError(): string | undefined {
    return root.querySelector('[data-field="startDateError"]')?.textContent?.trim();
  }

  async function typeDate(date: Date): Promise<void> {
    // El NativeDateAdapter interpreta `yyyy-MM-ddT00:00` como fecha local.
    dateInput().value = `${toLocalIsoDate(date)}T00:00`;
    dateInput().dispatchEvent(new Event('input'));
    dateInput().dispatchEvent(new Event('blur'));
    await fixture.whenStable();
  }

  async function confirm(): Promise<void> {
    confirmButton().click();
    await fixture.whenStable();
  }

  it('muestra "Fecha de inicio" con hoy como valor inicial y los botones Cancelar y Confirmar emisión', () => {
    expect(root.querySelector('mat-label')?.textContent?.trim()).toBe('Fecha de inicio');
    expect(dateInput().value).not.toBe('');
    expect(confirmButton().textContent?.trim()).toBe('Confirmar emisión');
    expect(confirmButton().disabled).toBe(false);
    expect(root.querySelector('[data-action="cancel"]')?.textContent?.trim()).toBe('Cancelar');
  });

  it('el botón del calendario usa los textos en español ("Abrir calendario") que provee el propio diálogo', () => {
    const toggle = root.querySelector('mat-datepicker-toggle button');
    expect(toggle?.getAttribute('aria-label')).toBe('Abrir calendario');
  });

  it('confirma con hoy: envía startDate en yyyy-MM-dd local', async () => {
    issue.mockResolvedValue(POLICY);

    await confirm();

    expect(issue).toHaveBeenCalledExactlyOnceWith(1, toLocalIsoDate(new Date()));
  });

  it('envía la fecha elegida (hoy + 30) en yyyy-MM-dd', async () => {
    issue.mockResolvedValue(POLICY);
    const date = daysFromToday(30);

    await typeDate(date);
    expect(dialogError()).toBeUndefined();
    await confirm();

    expect(issue).toHaveBeenCalledExactlyOnceWith(1, toLocalIsoDate(date));
  });

  it.each([
    ['ayer', -1],
    ['hoy + 31', 31],
  ])('E4 fecha fuera de rango (%s): muestra el mensaje y no permite confirmar', async (_, days) => {
    await typeDate(daysFromToday(days));

    expect(dialogError()).toBe('La fecha de inicio debe estar entre hoy y los próximos 30 días');
    expect(confirmButton().disabled).toBe(true);
  });

  it('éxito: cierra el diálogo, muestra "Póliza [número] emitida correctamente" y navega a /policies/:id', async () => {
    issue.mockResolvedValue(POLICY);

    await confirm();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith('issued');
    expect(snackBar.open).toHaveBeenCalledWith(
      'Póliza POL-2026-000004 emitida correctamente',
      'Cerrar',
      expect.anything(),
    );
    expect(navigate).toHaveBeenCalledWith(['/policies', 4]);
  });

  it('"Confirmar emisión" queda deshabilitado durante la llamada (sin doble envío)', async () => {
    let resolve!: (policy: PolicyDetail) => void;
    issue.mockReturnValue(new Promise<PolicyDetail>((r) => (resolve = r)));

    confirmButton().click();
    fixture.detectChanges();
    expect(confirmButton().disabled).toBe(true);
    confirmButton().click();
    expect(issue).toHaveBeenCalledTimes(1);

    resolve(POLICY);
    await fixture.whenStable();
    expect(navigate).toHaveBeenCalledTimes(1);
  });

  it.each([
    ['QUOTE_EXPIRED', 'La cotización está vencida. Generá una nueva'],
    ['QUOTE_ALREADY_ISSUED', 'Esta cotización ya fue emitida (póliza POL-2026-000001)'],
  ])('%s: cierra el diálogo pidiendo recargar el detalle y muestra el detail', async (code, detail) => {
    issue.mockRejectedValue(problem(409, code, detail));

    await confirm();

    expect(dialogRef.close).toHaveBeenCalledExactlyOnceWith('reload');
    expect(shown).toEqual([detail]);
    expect(navigate).not.toHaveBeenCalled();
  });

  it('INVALID_START_DATE: muestra el detail dentro del diálogo, sin cerrarlo', async () => {
    const detail = 'La fecha de inicio debe estar entre hoy y los próximos 30 días';
    issue.mockRejectedValue(problem(400, 'INVALID_START_DATE', detail));

    await confirm();

    expect(dialogError()).toBe(detail);
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(shown).toEqual([]);
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('otro error (lo muestra el errorInterceptor): el diálogo queda abierto y se puede reintentar', async () => {
    issue.mockRejectedValueOnce(
      new HttpErrorResponse({ status: 500, statusText: 'Error', error: {} }),
    );

    await confirm();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(confirmButton().disabled).toBe(false);
  });

  it('Cancelar cierra el diálogo sin emitir', async () => {
    root.querySelector<HTMLButtonElement>('[data-action="cancel"]')!.click();
    await fixture.whenStable();

    expect(dialogRef.close).toHaveBeenCalled();
    expect(issue).not.toHaveBeenCalled();
  });
});
