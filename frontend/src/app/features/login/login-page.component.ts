import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { asProblemDetails } from '../../core/errors/error.interceptor';

interface LoginModel {
  email: string;
  password: string;
}

@Component({
  selector: 'app-login-page',
  imports: [FormRoot, FormField, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.scss',
})
export class LoginPageComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly model = signal<LoginModel>({ email: '', password: '' });

  /** Mensaje de `INVALID_CREDENTIALS`, que se muestra debajo del formulario. */
  protected readonly loginError = signal('');

  protected readonly loginForm = form(
    this.model,
    (f) => {
      required(f.email, { message: 'Completá este campo' });
      email(f.email, { message: 'Email inválido' });
      required(f.password, { message: 'Completá este campo' });
    },
    {
      submission: {
        action: async (field) => {
          this.loginError.set('');
          try {
            await this.auth.login(field().value());
          } catch (error) {
            const problem =
              error instanceof HttpErrorResponse ? asProblemDetails(error.error) : null;
            if (problem?.code === 'INVALID_CREDENTIALS') {
              this.loginError.set(problem.detail);
            }
            // El resto de los errores ya los muestra el errorInterceptor.
            return;
          }
          await this.router.navigateByUrl('/quotes');
        },
      },
    },
  );
}
