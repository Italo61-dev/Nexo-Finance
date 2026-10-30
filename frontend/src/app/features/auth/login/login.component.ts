import { Component, ElementRef, ViewChild, AfterViewInit, inject, signal, NgZone } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AuthService } from '../../../core/services/auth.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

@Component({
  selector: 'app-login',
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
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly ngZone = inject(NgZone);

  @ViewChild('googleBtnContainer') private readonly googleBtnContainer?: ElementRef<HTMLDivElement>;

  private readonly GOOGLE_CLIENT_ID = environment.googleClientId;

  // Estados reativos (Signals)
  readonly isLoading = signal(false);
  readonly hidePassword = signal(true);

  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  ngAfterViewInit(): void {
    if (this.GOOGLE_CLIENT_ID) {
      this.initGoogleSignIn();
    }
  }

  private initGoogleSignIn(): void {
    if (typeof google !== 'undefined' && google.accounts?.id) {
      google.accounts.id.initialize({
        client_id: this.GOOGLE_CLIENT_ID,
        callback: (response: any) => {
          this.ngZone.run(() => {
            this.handleGoogleSignIn(response);
          });
        }
      });

      if (this.googleBtnContainer?.nativeElement) {
        google.accounts.id.renderButton(this.googleBtnContainer.nativeElement, {
          theme: 'outline',
          size: 'large',
          type: 'standard',
          shape: 'rectangular',
          text: 'signin_with',
          logo_alignment: 'left',
          width: 380
        });
      }
    } else {
      setTimeout(() => this.initGoogleSignIn(), 250);
    }
  }

  private handleGoogleSignIn(response: any): void {
    const idToken = response?.credential;
    if (!idToken) {
      this.snackBar.open('Não foi possível obter as credenciais do Google.', 'Fechar', {
        duration: 3500,
        horizontalPosition: 'end',
        verticalPosition: 'top'
      });
      return;
    }

    this.isLoading.set(true);

    this.authService.loginWithGoogle(idToken).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.snackBar.open('Login com Google efetuado com sucesso!', 'Fechar', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.isLoading.set(false);
        const errorMessage = err.error?.message || 'Falha ao autenticar com Google. Tente novamente.';
        this.snackBar.open(errorMessage, 'Fechar', {
          duration: 4000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
      }
    });
  }

  togglePasswordVisibility(): void {
    this.hidePassword.update(prev => !prev);
  }

  onSubmit(): void {
    if (this.loginForm.invalid || this.isLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);

    const credentials = {
      email: this.loginForm.value.email!.trim().toLowerCase(),
      password: this.loginForm.value.password!
    };

    this.authService.login(credentials).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.snackBar.open('Login efetuado com sucesso!', 'Fechar', {
          duration: 3000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
        this.router.navigate(['/home']);
      },
      error: (err) => {
        this.isLoading.set(false);
        let errorMessage = 'Não foi possível conectar ao servidor. Tente novamente.';
        
        if (err.status === 401) {
          errorMessage = 'E-mail ou senha incorretos.';
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
