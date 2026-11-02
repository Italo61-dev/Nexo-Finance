import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';
import { guestGuard } from './core/guards/guest.guard';
import { AuthService } from './core/services/auth.service';
import { MainLayoutComponent } from './core/layout/main-layout/main-layout.component';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password.component').then(m => m.ResetPasswordComponent),
    canActivate: [guestGuard]
  },
  {
    path: '',
    component: MainLayoutComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'home',
        loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
      },
      {
        path: 'home/admin',
        redirectTo: 'admin',
        pathMatch: 'full'
      },
      {
        path: 'admin',
        loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent),
        canActivate: [adminGuard]
      },
      {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
      }
    ]
  },
  {
    path: '**',
    redirectTo: () => (inject(AuthService).isAuthenticated() ? 'home' : 'login')
  }
];
