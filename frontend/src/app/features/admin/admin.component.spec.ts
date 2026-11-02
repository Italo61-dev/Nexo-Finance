import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { AdminComponent } from './admin.component';
import { AdminService } from '../../core/services/admin.service';
import { AuthService } from '../../core/services/auth.service';
import { AdminUserSummary, PageResponse, PlatformMetrics } from '../../core/models/auth.models';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('AdminComponent', () => {
  let component: AdminComponent;
  let fixture: ComponentFixture<AdminComponent>;
  let adminServiceSpy: jasmine.SpyObj<AdminService>;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let snackBarSpy: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const mockUsersResponse: PageResponse<AdminUserSummary> = {
    content: [
      {
        id: 1,
        name: 'Admin Master',
        email: 'admin@nexofinance.com',
        authProvider: 'LOCAL',
        role: 'ROLE_ADMIN',
        active: true,
        createdAt: '2026-11-01T10:00:00'
      },
      {
        id: 2,
        name: 'Carlos Silva',
        email: 'carlos@test.com',
        authProvider: 'GOOGLE',
        role: 'ROLE_USER',
        active: true,
        createdAt: '2026-11-01T11:00:00'
      }
    ],
    totalElements: 2,
    totalPages: 1,
    size: 10,
    number: 0,
    first: true,
    last: true,
    empty: false
  };

  const mockMetrics: PlatformMetrics = {
    totalUsers: 2,
    totalAdmins: 1,
    googleAuthPercent: 50.0,
    inactiveUsers: 0
  };

  beforeEach(async () => {
    adminServiceSpy = jasmine.createSpyObj<AdminService>('AdminService', [
      'getUsers',
      'getPlatformMetrics',
      'updateUserRole',
      'updateUserStatus'
    ]);
    authServiceSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'userEmail',
      'isAdmin',
      'isAuthenticated'
    ]);
    snackBarSpy = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    authServiceSpy.userEmail.and.returnValue('admin@nexofinance.com');
    authServiceSpy.isAdmin.and.returnValue(true);
    authServiceSpy.isAuthenticated.and.returnValue(true);

    adminServiceSpy.getUsers.and.returnValue(of(mockUsersResponse));
    adminServiceSpy.getPlatformMetrics.and.returnValue(of(mockMetrics));

    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        provideRouter([]),
        { provide: AdminService, useValue: adminServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
        { provide: MatSnackBar, useValue: snackBarSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    fixture = TestBed.createComponent(AdminComponent);
    component = fixture.componentInstance;
    spyOn(component['snackBar'], 'open');
    fixture.detectChanges();
  });

  it('deve inicializar o componente e carregar métricas e usuários', () => {
    expect(component).toBeTruthy();
    expect(adminServiceSpy.getPlatformMetrics).toHaveBeenCalled();
    expect(adminServiceSpy.getUsers).toHaveBeenCalled();
    expect(component.users().length).toBe(2);
    expect(component.metrics()?.totalUsers).toBe(2);
  });

  it('deve identificar corretamente o usuário logado para proteção anti-deadlock', () => {
    const adminUser = mockUsersResponse.content[0];
    const otherUser = mockUsersResponse.content[1];

    expect(component.isCurrentUser(adminUser)).toBeTrue();
    expect(component.isCurrentUser(otherUser)).toBeFalse();
  });

  it('deve bloquear tentativa de auto-modificação de cargo exibindo aviso e sem chamar a API', () => {
    const adminUser = mockUsersResponse.content[0];

    component.toggleRole(adminUser);

    expect(component['snackBar'].open).toHaveBeenCalledWith(
      jasmine.stringMatching(/Regra de segurança/),
      'Entendido',
      jasmine.any(Object)
    );
    expect(adminServiceSpy.updateUserRole).not.toHaveBeenCalled();
  });

  it('deve bloquear tentativa de auto-bloqueio exibindo aviso e sem chamar a API', () => {
    const adminUser = mockUsersResponse.content[0];

    component.toggleStatus(adminUser);

    expect(component['snackBar'].open).toHaveBeenCalledWith(
      jasmine.stringMatching(/Regra de segurança/),
      'Entendido',
      jasmine.any(Object)
    );
    expect(adminServiceSpy.updateUserStatus).not.toHaveBeenCalled();
  });

  it('deve formatar corretamente as iniciais do avatar do usuário', () => {
    expect(component.getUserInitials('Carlos Silva')).toBe('CS');
    expect(component.getUserInitials('Maria')).toBe('MA');
    expect(component.getUserInitials('João Pedro Santos')).toBe('JS');
    expect(component.getUserInitials('')).toBe('U');
  });

  it('deve navegar de volta ao dashboard ao chamar goToDashboard', () => {
    spyOn(router, 'navigate');
    component.goToDashboard();
    expect(router.navigate).toHaveBeenCalledWith(['/home']);
  });

  it('deve filtrar usuários ao alterar a tab de filtro', () => {
    component.onFilterChange('ADMINS');

    expect(component.selectedFilter).toBe('ADMINS');
    expect(component.currentPage).toBe(0);
    expect(adminServiceSpy.getUsers).toHaveBeenCalledWith(
      jasmine.objectContaining({ role: 'ROLE_ADMIN' })
    );
  });
});
