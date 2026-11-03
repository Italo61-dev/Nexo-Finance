import { TestBed } from '@angular/core/testing';
import { ThemeService, ThemeMode } from './theme.service';

describe('ThemeService', () => {
  let themeService: ThemeService;
  const STORAGE_KEY = 'nexo_theme_preference';

  beforeEach(() => {
    localStorage.removeItem(STORAGE_KEY);
    document.documentElement.removeAttribute('data-theme');
    document.body.classList.remove('dark-theme', 'light-theme');

    TestBed.configureTestingModule({
      providers: [ThemeService]
    });
    themeService = TestBed.inject(ThemeService);
  });

  afterEach(() => {
    localStorage.removeItem(STORAGE_KEY);
    document.documentElement.removeAttribute('data-theme');
    document.body.classList.remove('dark-theme', 'light-theme');
  });

  it('deve ser instanciado corretamente com o tema padrão dark', () => {
    expect(themeService).toBeTruthy();
    expect(themeService.currentTheme()).toBe('dark');
    expect(themeService.isDarkMode()).toBeTrue();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.body.classList.contains('dark-theme')).toBeTrue();
  });

  it('deve alternar entre temas dark e light com toggleTheme()', () => {
    expect(themeService.currentTheme()).toBe('dark');

    themeService.toggleTheme();
    expect(themeService.currentTheme()).toBe('light');
    expect(themeService.isDarkMode()).toBeFalse();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(document.body.classList.contains('light-theme')).toBeTrue();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('light');

    themeService.toggleTheme();
    expect(themeService.currentTheme()).toBe('dark');
    expect(themeService.isDarkMode()).toBeTrue();
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(document.body.classList.contains('dark-theme')).toBeTrue();
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark');
  });

  it('deve definir o tema explicitamente com setTheme()', () => {
    themeService.setTheme('light');
    expect(themeService.currentTheme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('light');

    themeService.setTheme('dark');
    expect(themeService.currentTheme()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    expect(localStorage.getItem(STORAGE_KEY)).toBe('dark');
  });

  it('deve carregar o tema salvo previamente no localStorage ao iniciar', () => {
    localStorage.setItem(STORAGE_KEY, 'light');

    const freshThemeService = new ThemeService();
    expect(freshThemeService.currentTheme()).toBe('light');
    expect(freshThemeService.isDarkMode()).toBeFalse();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
