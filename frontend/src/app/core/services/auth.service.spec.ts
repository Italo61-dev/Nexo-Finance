import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('deve ser instanciado com sucesso', () => {
    expect(service).toBeTruthy();
  });

  it('deve realizar login e persistir o token no localStorage e no Signal', () => {
    const mockCredentials: LoginRequest = { email: 'teste@nexofinance.com', password: 'password123' };
    const mockResponse: AuthResponse = { token: 'mock-jwt-token-123', type: 'Bearer' };

    service.login(mockCredentials).subscribe(response => {
      expect(response).toEqual(mockResponse);
      expect(service.token()).toBe('mock-jwt-token-123');
      expect(service.isAuthenticated()).toBeTrue();
      expect(localStorage.getItem('nexo_token')).toBe('mock-jwt-token-123');
    });

    const req = httpTesting.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockCredentials);
    req.flush(mockResponse);
  });

  it('deve realizar cadastro de novo usuário', () => {
    const mockRegister: RegisterRequest = { name: 'Novo Usuário', email: 'novo@nexofinance.com', password: 'password123' };
    const mockUser = { id: 1, name: 'Novo Usuário', email: 'novo@nexofinance.com', createdAt: '', updatedAt: '' };

    service.register(mockRegister).subscribe(response => {
      expect(response).toEqual(mockUser);
    });

    const req = httpTesting.expectOne('/api/v1/users');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(mockRegister);
    req.flush(mockUser);
  });

  it('deve remover token ao efetuar logout', () => {
    service.saveToken('token-ativo');
    expect(service.isAuthenticated()).toBeTrue();

    service.logout();
    expect(service.token()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem('nexo_token')).toBeNull();
  });
});
