import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;

  function createMockJwt(payload: object): string {
    const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
    const body = btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    return `${header}.${body}.mock-signature`;
  }

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

  it('deve decodificar claim de role e identificar administrador quando ROLE_ADMIN', () => {
    const adminToken = createMockJwt({
      sub: 'admin@nexofinance.com',
      role: 'ROLE_ADMIN'
    });

    service.saveToken(adminToken);

    expect(service.role()).toBe('ROLE_ADMIN');
    expect(service.isAdmin()).toBeTrue();
    expect(service.userEmail()).toBe('admin@nexofinance.com');
  });

  it('deve identificar usuário comum quando role for ROLE_USER', () => {
    const userToken = createMockJwt({
      sub: 'user@nexofinance.com',
      role: 'ROLE_USER'
    });

    service.saveToken(userToken);

    expect(service.role()).toBe('ROLE_USER');
    expect(service.isAdmin()).toBeFalse();
    expect(service.userEmail()).toBe('user@nexofinance.com');
  });

  it('deve retornar null para role e isAdmin false quando não houver token ou token for malformado', () => {
    service.saveToken('token-invalido');

    expect(service.role()).toBeNull();
    expect(service.isAdmin()).toBeFalse();
    expect(service.userEmail()).toBeNull();
  });

  it('deve realizar login e persistir o token no localStorage e no Signal', () => {
    const mockCredentials: LoginRequest = { email: 'teste@nexofinance.com', password: 'password123' };
    const mockToken = createMockJwt({ sub: 'teste@nexofinance.com', role: 'ROLE_USER' });
    const mockResponse: AuthResponse = { token: mockToken, type: 'Bearer' };

    service.login(mockCredentials).subscribe(response => {
      expect(response).toEqual(mockResponse);
      expect(service.token()).toBe(mockToken);
      expect(service.isAuthenticated()).toBeTrue();
      expect(localStorage.getItem('nexo_token')).toBe(mockToken);
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
    const mockToken = createMockJwt({ sub: 'ativo@nexofinance.com', role: 'ROLE_USER' });
    service.saveToken(mockToken);
    expect(service.isAuthenticated()).toBeTrue();

    service.logout();
    expect(service.token()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.role()).toBeNull();
    expect(service.isAdmin()).toBeFalse();
    expect(localStorage.getItem('nexo_token')).toBeNull();
  });
});
