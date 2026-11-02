import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { adminGuard } from './admin.guard';
import { AuthService } from '../services/auth.service';

describe('adminGuard', () => {
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  beforeEach(() => {
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', ['isAuthenticated', 'isAdmin']);
    snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    });

    router = TestBed.inject(Router);
  });

  it('deve permitir acesso quando usuário estiver autenticado e for administrador', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(true);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toBeTrue();
    expect(snackBarSpy.open).not.toHaveBeenCalled();
  });

  it('deve redirecionar para /home e exibir aviso de acesso negado quando for ROLE_USER comum', () => {
    authServiceSpy.isAuthenticated.and.returnValue(true);
    authServiceSpy.isAdmin.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toEqual(router.parseUrl('/home'));
    expect(snackBarSpy.open).toHaveBeenCalledWith(
      'Acesso negado: seu perfil de usuário não possui permissão de administrador.',
      'Fechar',
      jasmine.objectContaining({ duration: 4000 })
    );
  });

  it('deve redirecionar para /login sem exibir aviso quando usuário não estiver autenticado', () => {
    authServiceSpy.isAuthenticated.and.returnValue(false);
    authServiceSpy.isAdmin.and.returnValue(false);

    const result = TestBed.runInInjectionContext(() => adminGuard({} as any, {} as any));

    expect(result).toEqual(router.parseUrl('/login'));
    expect(snackBarSpy.open).not.toHaveBeenCalled();
  });
});
