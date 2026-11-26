import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { email, form, FormField, FormRoot, required } from '@angular/forms/signals';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

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
  protected readonly model = signal<LoginModel>({ email: '', password: '' });

  protected readonly loginForm = form(
    this.model,
    (f) => {
      required(f.email, { message: 'Completá este campo' });
      email(f.email, { message: 'Email inválido' });
      required(f.password, { message: 'Completá este campo' });
    },
    {
      submission: {
        // TODO etapa 3: llamar a AuthService.login y navegar a /quotes.
        action: async () => undefined,
      },
    },
  );
}
