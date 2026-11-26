import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterOutlet } from '@angular/router';

import { AuthService } from '../core/auth/auth.service';

/** Contenedor de las rutas protegidas: toolbar con el productor y "Salir" (frontend.md §5.2). */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, MatToolbarModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-toolbar color="primary" class="shell__toolbar">
      <span class="shell__title">Cotizador de seguros</span>
      <span class="shell__spacer"></span>
      <span class="shell__producer">{{ auth.producerName() }}</span>
      <button matButton type="button" (click)="logout()">Salir</button>
    </mat-toolbar>
    <main class="shell__content">
      <router-outlet />
    </main>
  `,
  styles: `
    .shell__toolbar {
      gap: 16px;
    }
    .shell__spacer {
      flex: 1 1 auto;
    }
    .shell__producer {
      font-size: 14px;
    }
    .shell__content {
      padding: 16px;
    }
  `,
})
export class ShellComponent {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected logout(): void {
    this.auth.logout();
    void this.router.navigateByUrl('/login');
  }
}
