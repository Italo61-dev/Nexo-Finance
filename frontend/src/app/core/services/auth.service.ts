import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  ResetPasswordRequest,
  Role,
  UserResponse
} from '../models/auth.models';

interface JwtPayload {
  sub?: string;
  email?: string;
  role?: Role;
  exp?: number;
  iat?: number;
  iss?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly TOKEN_KEY = 'nexo_token';

  // Sinais reativos modernos do Angular 19 para gestão de estado
  readonly token = signal<string | null>(this.getInitialToken());
  readonly isAuthenticated = computed(() => !!this.token());

  // Extração reativa de claims do token JWT
  readonly role = computed<Role | null>(() => this.extractClaimFromToken<Role>('role'));
  readonly userEmail = computed<string | null>(() => {
    const payload = this.decodeTokenPayload();
    return payload?.sub || payload?.email || null;
  });
  readonly isAdmin = computed<boolean>(() => this.role() === 'ROLE_ADMIN');

  private getInitialToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRole(): Role | null {
    return this.role();
  }

  getUserEmail(): string | null {
    return this.userEmail();
  }

  isAdminUser(): boolean {
    return this.isAdmin();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', credentials).pipe(
      tap(response => {
        this.saveToken(response.token);
      })
    );
  }

  loginWithGoogle(idToken: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/google', { idToken }).pipe(
      tap(response => {
        this.saveToken(response.token);
      })
    );
  }

  register(userData: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>('/api/v1/users', userData);
  }

  forgotPassword(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>('/api/v1/auth/forgot-password', { email });
  }

  resetPassword(data: ResetPasswordRequest): Observable<{ message: string }> {
    return this.http.post<{ message: string }>('/api/v1/auth/reset-password', data);
  }

  saveToken(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.token.set(token);
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.token.set(null);
  }

  private extractClaimFromToken<T>(claimName: keyof JwtPayload): T | null {
    const payload = this.decodeTokenPayload();
    if (!payload) return null;
    return (payload[claimName] as T) ?? null;
  }

  private decodeTokenPayload(): JwtPayload | null {
    const rawToken = this.token();
    if (!rawToken) return null;

    try {
      const parts = rawToken.split('.');
      if (parts.length !== 3) return null;

      const base64Url = parts[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );

      return JSON.parse(jsonPayload) as JwtPayload;
    } catch {
      return null;
    }
  }
}
