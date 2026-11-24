import { inject, Injectable, signal } from '@angular/core';
import { LocalStorageService } from './local-storage.service';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private static readonly STORAGE_KEY = 'dropline-theme';

  readonly theme = signal<Theme>('light');

  private readonly localStorageService = inject(LocalStorageService);

  constructor() {
    const savedTheme = this.localStorageService.getItem(ThemeService.STORAGE_KEY);

    if (savedTheme === 'light' || savedTheme === 'dark') {
      this.theme.set(savedTheme);
    }

    this.applyTheme(this.theme());
  }

  toggleTheme(): void {
    this.theme.update((current) => (current === 'light' ? 'dark' : 'light'));
    this.localStorageService.setItem(ThemeService.STORAGE_KEY, this.theme());
    this.applyTheme(this.theme());
  }

  private applyTheme(theme: Theme): void {
    document.documentElement.classList.toggle('dark-theme', theme === 'dark');
  }
}
