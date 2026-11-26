import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

/** Muestra mensajes de error al usuario en un `MatSnackBar` (frontend.md §4). */
@Injectable({ providedIn: 'root' })
export class ErrorMessageService {
  private readonly snackBar = inject(MatSnackBar);

  show(message: string): void {
    this.snackBar.open(message, 'Cerrar', { duration: 5000 });
  }
}
