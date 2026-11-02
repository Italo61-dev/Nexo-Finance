import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { MainLayoutComponent } from './main-layout.component';
import { AuthService } from '../../services/auth.service';
import { ThemeService } from '../../services/theme.service';
import { signal, computed } from '@angular/core';
import { Role } from '../../models/auth.models';

describe('MainLayoutComponent', () => {
  let fixture: ComponentFixture<MainLayoutComponent>;
  let component: MainLayoutComponent;
  let authServiceSpy: jasmine.SpyObj<AuthService>;
  let themeServiceSpy: jasmine.SpyObj<ThemeService>;
  let router: Router;

  const mockToken = signal<string | null>('fake-token');
  const mockRole = signal<Role | null>('ROLE_USER');
  const mockIsAdmin = computed(() => mockRole() === 'ROLE_ADMIN');
  const mockUserEmail = computed(() => 'cliente@nexo.com');
  const mockIsDarkMode = signal<boolean>(true);

  beforeEach(async () => {
    mockRole.set('ROLE_USER');
    mockIsDarkMode.set(true);

    authServiceSpy = jasmine.createSpyObj('AuthService', ['logout'], {
      token: mockToken,
      role: mockRole,
      isAdmin: mockIsAdmin,
      userEmail: mockUserEmail
    });

    themeServiceSpy = jasmine.createSpyObj('ThemeService', ['toggleTheme'], {
      isDarkMode: mockIsDarkMode
    });

    await TestBed.configureTestingModule({
      imports: [MainLayoutComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: ThemeService, useValue: themeServiceSpy }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate');

    fixture = TestBed.createComponent(MainLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve ser instanciado corretamente com o layout padrão', () => {
    expect(component).toBeTruthy();
    expect(component.isSidebarCollapsed()).toBeFalse();
    expect(component.isMobileMenuOpen()).toBeFalse();
  });

  it('não deve exibir o link do Painel Admin quando o usuário for ROLE_USER', () => {
    mockRole.set('ROLE_USER');
    fixture.detectChanges();

    const adminLink = fixture.nativeElement.querySelector('.nav-admin-item');
    expect(adminLink).toBeNull();
  });

  it('deve exibir o link do Painel Admin quando o usuário for ROLE_ADMIN', () => {
    mockRole.set('ROLE_ADMIN');
    fixture.detectChanges();

    const adminLink = fixture.nativeElement.querySelector('.nav-admin-item');
    expect(adminLink).toBeTruthy();
    expect(adminLink.textContent).toContain('Painel Admin');
  });

  it('deve alternar o colapso da sidebar ao chamar toggleSidebarCollapse()', () => {
    expect(component.isSidebarCollapsed()).toBeFalse();

    component.toggleSidebarCollapse();
    expect(component.isSidebarCollapsed()).toBeTrue();

    component.toggleSidebarCollapse();
    expect(component.isSidebarCollapsed()).toBeFalse();
  });

  it('deve alternar o menu mobile com toggleMobileMenu() e fechar com closeMobileMenu()', () => {
    expect(component.isMobileMenuOpen()).toBeFalse();

    component.toggleMobileMenu();
    expect(component.isMobileMenuOpen()).toBeTrue();

    component.closeMobileMenu();
    expect(component.isMobileMenuOpen()).toBeFalse();
  });

  it('deve chamar themeService.toggleTheme() ao clicar no botão de tema', () => {
    const themeButton = fixture.nativeElement.querySelector('.theme-toggle-button');
    themeButton.click();

    expect(themeServiceSpy.toggleTheme).toHaveBeenCalled();
  });

  it('deve acionar logout e navegar para /login ao clicar no botão de logout', () => {
    const logoutButton = fixture.nativeElement.querySelector('.logout-button');
    logoutButton.click();

    expect(authServiceSpy.logout).toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
