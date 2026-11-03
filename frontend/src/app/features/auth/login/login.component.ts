import { Component, ElementRef, ViewChild, AfterViewInit, inject, signal, NgZone } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { environment } from '../../../../environments/environment';

declare const google: any;

// Validador de confirmação de senha
export const passwordMatchValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const password = control.get('password')?.value;
  const confirmPassword = control.get('confirmPassword')?.value;

  if (password && confirmPassword && password !== confirmPassword) {
    control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
    return { passwordMismatch: true };
  }

  if (control.get('confirmPassword')?.hasError('passwordMismatch')) {
    control.get('confirmPassword')?.setErrors(null);
  }

  return null;
};

// Validador de complexidade de senha (mínimo 8 caracteres, maiúscula, número e caractere especial)
export const strongPasswordValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value || '';
  if (!value) return null;

  const hasMinLength = value.length >= 8;
  const hasUppercase = /[A-Z]/.test(value);
  const hasSpecialChar = /[^a-zA-Z0-9]/.test(value);
  const hasNumber = /\d/.test(value);

  const isValid = hasMinLength && hasUppercase && hasSpecialChar && hasNumber;
  return isValid ? null : { passwordComplexity: true };
};

export type AuthBackFaceMode = 'register' | 'forgot';
export type MobileSheetState = 'initial' | 'login' | 'register' | 'forgot';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent implements AfterViewInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  readonly themeService = inject(ThemeService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly ngZone = inject(NgZone);

  @ViewChild('googleBtnContainer') private readonly googleBtnContainer?: ElementRef<HTMLDivElement>;

  @ViewChild('googleBtnContainerMobile')
  set setGoogleBtnContainerMobile(el: ElementRef<HTMLDivElement> | undefined) {
    if (el?.nativeElement) {
      this.googleBtnContainerMobile = el;
      setTimeout(() => this.renderGoogleMobileButton(), 60);
    }
  }
  private googleBtnContainerMobile?: ElementRef<HTMLDivElement>;

  private readonly GOOGLE_CLIENT_ID = environment.googleClientId;

  // Controle da Animação 3D da Coluna da Esquerda no Desktop (Página Virando)
  readonly isFlipped = signal(false);
  readonly backFaceMode = signal<AuthBackFaceMode>('register');

  // Controle do Bottom Sheet no Mobile (Estilo C6 Bank)
  readonly mobileSheetState = signal<MobileSheetState>('initial');

  // Estados reativos de Login (Signals)
  readonly isLoading = signal(false);
  readonly hidePassword = signal(true);

  // Estados reativos de Cadastro
  readonly isRegisterLoading = signal(false);
  readonly hideRegisterPassword = signal(true);
  readonly hideRegisterConfirmPassword = signal(true);

  // Estados reativos de Recuperação de Senha
  readonly isForgotLoading = signal(false);
  readonly forgotSubmitted = signal(false);

  readonly loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]]
  });

  readonly registerForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, strongPasswordValidator]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: passwordMatchValidator });

  // Getters para o checklist em tempo real da senha ("vai digitando e verificando")
  get registerPasswordValue(): string {
    return this.registerForm.get('password')?.value || '';
  }

  get hasMinLength(): boolean {
    return this.registerPasswordValue.length >= 8;
  }

  get hasUppercase(): boolean {
    return /[A-Z]/.test(this.registerPasswordValue);
  }

  get hasSpecialChar(): boolean {
    return /[^a-zA-Z0-9]/.test(this.registerPasswordValue);
  }

  get hasNumber(): boolean {
    return /\d/.test(this.registerPasswordValue);
  }

  readonly forgotForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  // Métodos de Navegação 3D na Coluna Esquerda (Desktop)
  openRegister(): void {
    this.backFaceMode.set('register');
    this.isFlipped.set(true);
    this.mobileSheetState.set('register');
  }

  openForgotPassword(): void {
    this.backFaceMode.set('forgot');
    this.forgotSubmitted.set(false);
    this.isFlipped.set(true);
    this.mobileSheetState.set('forgot');
  }

  backToLogin(): void {
    this.isFlipped.set(false);
    this.mobileSheetState.set('login');
  }

  // Métodos de Controle do Bottom Sheet (Mobile - Estilo C6 Bank)
  openMobileLogin(): void {
    this.mobileSheetState.set('login');
  }

  openMobileRegister(): void {
    this.mobileSheetState.set('register');
  }

  openMobileForgot(): void {
    this.mobileSheetState.set('forgot');
    this.forgotSubmitted.set(false);
  }

  closeMobileSheet(): void {
    this.mobileSheetState.set('initial');
  }

  toggleRegisterPasswordVisibility(): void {
    this.hideRegisterPassword.update(prev => !prev);
  }

  toggleRegisterConfirmPasswordVisibility(): void {
    this.hideRegisterConfirmPassword.update(prev => !prev);
  }

  onRegisterSubmit(): void {
    if (this.registerForm.invalid || this.isRegisterLoading()) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.isRegisterLoading.set(true);

    const formData = {
      name: this.registerForm.value.name!.trim(),
      email: this.registerForm.value.email!.trim().toLowerCase(),
      password: this.registerForm.value.password!
    };

    this.authService.register(formData).subscribe({
      next: () => {
        this.isRegisterLoading.set(false);
        this.snackBar.open('Conta criada com sucesso! Faça seu login para continuar.', 'Fechar', {
          duration: 4000,
          horizontalPosition: 'end',
          verticalPosition: 'top'
        });
        this.registerForm.reset();
        // Vira a página 3D de volta para o Login no Desktop e abre o sheet de login no Mobile
        this.isFlipped.set(false);
        this.mobileSheetState.set('login');
      },
      error: (err) => {
        this.isRegisterLoading.set(false);
        let errorMessage = 'Não foi possível cadastrar no momento. Tente novamente.';
        if (err.status === 409) {
          errorMessage = 'Este e-mail já está em uso por outro usuário.';
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

  onForgotSubmit(): void {
    if (this.forgotForm.invalid || this.isForgotLoading()) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.isForgotLoading.set(true);
    const email = this.forgotForm.value.email!.trim().toLowerCase();

    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.isForgotLoading.set(false);
        this.forgotSubmitted.set(true);
        this.snackBar.open(
          'Se o e-mail estiver cadastrado, as instruções foram enviadas!',
          'Fechar',
          { duration: 5000, horizontalPosition: 'end', verticalPosition: 'top' }
        );
      },
      error: () => {
        this.isForgotLoading.set(false);
        this.forgotSubmitted.set(true);
        this.snackBar.open(
          'Se o e-mail estiver cadastrado, as instruções foram enviadas!',
          'Fechar',
          { duration: 5000, horizontalPosition: 'end', verticalPosition: 'top' }
        );
      }
    });
  }

  ngAfterViewInit(): void {
    if (this.GOOGLE_CLIENT_ID) {
      this.initGoogleSignIn();
    }
  }

  onToggleTheme(): void {
    this.themeService.toggleTheme();
    // Re-renderiza o botão do Google para adaptar-se dinamicamente ao novo tema (filled_black vs outline)
    setTimeout(() => this.initGoogleSignIn(), 50);
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

      const isDark = this.themeService.isDarkMode();

      if (this.googleBtnContainer?.nativeElement) {
        this.googleBtnContainer.nativeElement.innerHTML = '';
        const containerWidth = this.googleBtnContainer.nativeElement.offsetWidth || 380;
        google.accounts.id.renderButton(this.googleBtnContainer.nativeElement, {
          theme: isDark ? 'filled_black' : 'outline',
          size: 'large',
          type: 'standard',
          shape: 'rectangular',
          text: 'signin_with',
          logo_alignment: 'left',
          width: containerWidth
        });
      }

      this.renderGoogleMobileButton();
    } else {
      setTimeout(() => this.initGoogleSignIn(), 250);
    }
  }

  private renderGoogleMobileButton(): void {
    if (typeof google !== 'undefined' && google.accounts?.id && this.googleBtnContainerMobile?.nativeElement) {
      this.googleBtnContainerMobile.nativeElement.innerHTML = '';
      const isDark = this.themeService.isDarkMode();
      const mobileWidth = this.googleBtnContainerMobile.nativeElement.offsetWidth || Math.min(window.innerWidth - 64, 340);
      google.accounts.id.renderButton(this.googleBtnContainerMobile.nativeElement, {
        theme: isDark ? 'filled_black' : 'outline',
        size: 'large',
        type: 'standard',
        shape: 'rectangular',
        text: 'signin_with',
        logo_alignment: 'left',
        width: mobileWidth
      });
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
