import { Injectable, signal, computed } from '@angular/core';

export type ThemeMode = 'light' | 'dark';

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_STORAGE_KEY = 'nexo_theme_preference';

  readonly currentTheme = signal<ThemeMode>(this.getInitialTheme());
  readonly isDarkMode = computed(() => this.currentTheme() === 'dark');

  constructor() {
    this.applyThemeToDocument(this.currentTheme());
  }

  toggleTheme(): void {
    const nextTheme: ThemeMode = this.currentTheme() === 'dark' ? 'light' : 'dark';
    this.setTheme(nextTheme);
  }

  setTheme(themeMode: ThemeMode): void {
    this.currentTheme.set(themeMode);
    try {
      localStorage.setItem(this.THEME_STORAGE_KEY, themeMode);
    } catch {
      // Ignora falha silenciosa se localStorage não estiver acessível
    }
    this.applyThemeToDocument(themeMode);
  }

  private getInitialTheme(): ThemeMode {
    try {
      const savedTheme = localStorage.getItem(this.THEME_STORAGE_KEY) as ThemeMode | null;
      if (savedTheme === 'light' || savedTheme === 'dark') {
        return savedTheme;
      }
    } catch {
      // Fallback para detecção do sistema
    }

    if (typeof window !== 'undefined' && window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    // O modo escuro é o padrão institucional e oficial do Nexo Finance
    return 'dark';
  }

  private applyThemeToDocument(themeMode: ThemeMode): void {
    if (typeof document === 'undefined') {
      return;
    }
    const rootElement = document.documentElement;
    const bodyElement = document.body;

    rootElement.setAttribute('data-theme', themeMode);
    if (themeMode === 'dark') {
      bodyElement.classList.add('dark-theme');
      bodyElement.classList.remove('light-theme');
    } else {
      bodyElement.classList.add('light-theme');
      bodyElement.classList.remove('dark-theme');
    }
  }
}
