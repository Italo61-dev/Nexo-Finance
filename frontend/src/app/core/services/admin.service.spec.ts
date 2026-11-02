import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { AdminUserSummary, PageResponse, PlatformMetrics } from '../models/auth.models';

describe('AdminService', () => {
  let service: AdminService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        AdminService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AdminService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('deve buscar usuários com parâmetros de paginação e filtros', () => {
    const mockResponse: PageResponse<AdminUserSummary> = {
      content: [
        {
          id: 1,
          name: 'Admin Teste',
          email: 'admin@nexofinance.com',
          authProvider: 'LOCAL',
          role: 'ROLE_ADMIN',
          active: true,
          createdAt: '2026-11-01T10:00:00'
        }
      ],
      totalElements: 1,
      totalPages: 1,
      size: 10,
      number: 0,
      first: true,
      last: true,
      empty: false
    };

    service.getUsers({ search: 'Admin', role: 'ROLE_ADMIN', active: true, page: 0, size: 10 }).subscribe(res => {
      expect(res).toEqual(mockResponse);
      expect(res.content.length).toBe(1);
      expect(res.content[0].email).toBe('admin@nexofinance.com');
    });

    const req = httpTesting.expectOne(request =>
      request.url === '/api/v1/admin/users' &&
      request.params.get('search') === 'Admin' &&
      request.params.get('role') === 'ROLE_ADMIN' &&
      request.params.get('active') === 'true'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('deve obter métricas consolidadas da plataforma', () => {
    const mockMetrics: PlatformMetrics = {
      totalUsers: 150,
      totalAdmins: 4,
      googleAuthPercent: 35.5,
      inactiveUsers: 3
    };

    service.getPlatformMetrics().subscribe(metrics => {
      expect(metrics).toEqual(mockMetrics);
      expect(metrics.totalUsers).toBe(150);
      expect(metrics.totalAdmins).toBe(4);
    });

    const req = httpTesting.expectOne('/api/v1/admin/users/metrics');
    expect(req.request.method).toBe('GET');
    req.flush(mockMetrics);
  });

  it('deve atualizar o cargo (role) do usuário via PATCH', () => {
    const mockUpdated: AdminUserSummary = {
      id: 2,
      name: 'Membro Promovido',
      email: 'membro@nexofinance.com',
      authProvider: 'LOCAL',
      role: 'ROLE_ADMIN',
      active: true,
      createdAt: '2026-11-01T10:00:00'
    };

    service.updateUserRole(2, 'ROLE_ADMIN').subscribe(user => {
      expect(user).toEqual(mockUpdated);
      expect(user.role).toBe('ROLE_ADMIN');
    });

    const req = httpTesting.expectOne('/api/v1/admin/users/2/role');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ role: 'ROLE_ADMIN' });
    req.flush(mockUpdated);
  });

  it('deve atualizar o status de ativação do usuário via PATCH', () => {
    const mockUpdated: AdminUserSummary = {
      id: 3,
      name: 'Usuário Bloqueado',
      email: 'bloqueado@nexofinance.com',
      authProvider: 'LOCAL',
      role: 'ROLE_USER',
      active: false,
      createdAt: '2026-11-01T10:00:00'
    };

    service.updateUserStatus(3, false).subscribe(user => {
      expect(user).toEqual(mockUpdated);
      expect(user.active).toBeFalse();
    });

    const req = httpTesting.expectOne('/api/v1/admin/users/3/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ active: false });
    req.flush(mockUpdated);
  });
});
