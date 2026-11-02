import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  if (authService.isAuthenticated() && authService.isAdmin()) {
    return true;
  }

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/login');
  }

  snackBar.open('Acesso negado: seu perfil de usuário não possui permissão de administrador.', 'Fechar', {
    duration: 4000,
    horizontalPosition: 'end',
    verticalPosition: 'top'
  });

  return router.parseUrl('/home');
};
