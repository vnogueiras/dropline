import { inject, Injectable, signal } from '@angular/core';
import { LocalStorageService } from './local-storage.service';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private static readonly STORAGE_KEY = 'dropline-theme';
  private readonly localStorageService = inject(LocalStorageService);

  readonly theme = signal<Theme>('light');

  constructor() {
    const savedTheme = this.localStorageService.getItem<unknown>(ThemeService.STORAGE_KEY);

    if (savedTheme === 'light' || savedTheme === 'dark') {
      this.theme.set(savedTheme);
    } else {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)');

      this.theme.set(prefersDark.matches ? 'dark' : 'light');

      // Follow system preference until the user selects a theme.
      prefersDark.addEventListener('change', (event) => {
        if (this.localStorageService.getItem<unknown>(ThemeService.STORAGE_KEY) === null) {
          this.theme.set(event.matches ? 'dark' : 'light');
          this.applyTheme(this.theme());
        }
      });
    }

    this.applyTheme(this.theme());
  }

  toggleTheme(): void {
    this.theme.update((current) => (current === 'light' ? 'dark' : 'light'));
    this.localStorageService.setItem(ThemeService.STORAGE_KEY, this.theme());
    this.applyTheme(this.theme());
  }

  private applyTheme(theme: Theme): void {
    document.documentElement.style.colorScheme = theme;
  }
}
