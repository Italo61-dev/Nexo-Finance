import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AuthResponse, LoginRequest, RegisterRequest, ResetPasswordRequest, UserResponse } from '../models/auth.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly TOKEN_KEY = 'nexo_token';

  // Sinais reativos modernos do Angular 19 para gestão de estado
  readonly token = signal<string | null>(this.getInitialToken());
  readonly isAuthenticated = computed(() => !!this.token());

  private getInitialToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', credentials).pipe(
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
}
