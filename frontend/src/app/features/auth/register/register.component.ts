import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';

// Validador customizado sênior para confirmação de senha
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (password && confirmPassword && password !== confirmPassword) {
    control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
    return { passwordMismatch: true };
  }

  // Remove o erro de mismatch se agora estiverem iguais
  if (control.get('confirmPassword')?.hasError('passwordMismatch')) {
    control.get('confirmPassword')?.setErrors(null);
  }

  return null;
};

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  // Estados reativos (Signals)
  readonly isLoading = signal(false);
  readonly hidePassword = signal(true);
  readonly hideConfirmPassword = signal(true);

  readonly registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator });

  togglePasswordVisibility(): void {
    this.hidePassword.update(prev => !prev);
  }

  toggleConfirmPasswordVisibility(): void {
    this.hideConfirmPassword.update(prev => !prev);
  }

  onSubmit(): void {
    if (this.registerForm.invalid || this.isLoading()) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);

    const formData = {
      name: this.registerForm.value.name!.trim(),
      email: this.registerForm.value.email!.trim().toLowerCase(),
      password: this.registerForm.value.password!
    };

    this.authService.register(formData).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.snackBar.open('Conta criada com sucesso! Faça seu login para continuar.', 'Entrar', {
          duration: 4000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isLoading.set(false);
        let errorMessage = 'Não foi possível cadastrar no momento. Tente novamente.';

        if (err.status === 409) {
          errorMessage = 'Este e-mail já está cadastrado no Nexo Finance.';
        } else if (err.error?.message) {
          errorMessage = err.error.message;
        }

        this.snackBar.open(errorMessage, 'Fechar', {
          duration: 4000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
      }
    });
  }
}
