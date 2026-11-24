import { Injectable, signal } from '@angular/core';
import { LocalStorageService } from './local-storage.service';

export type Theme = 'light' | 'dark';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private static readonly STORAGE_KEY = 'dropline-theme';

  // TODO: Initialize the signal using the saved theme from localStorage.
  readonly theme = signal<Theme>('light');

  constructor(private localStorageService: LocalStorageService) {
    // TODO: Apply the initial theme to the document.
    const savedTheme = this.localStorageService.getItem(ThemeService.STORAGE_KEY);

    if (savedTheme === 'light' || savedTheme === 'dark') {
      this.theme.set(savedTheme);
    }

    this.applyTheme(this.theme());
  }

  toggleTheme(): void {
    // TODO: Switch between light and dark.
    this.theme.update((current) => (current === 'light' ? 'dark' : 'light'));
    // TODO: Persist the selected theme.
    this.localStorageService.setItem(ThemeService.STORAGE_KEY, this.theme());
    // TODO: Apply the selected theme to the document.
    this.applyTheme(this.theme());
  }

  private applyTheme(theme: Theme): void {
    // TODO: Update the document's theme attribute/class.
    document.documentElement.classList.toggle('dark-theme', theme === 'dark');
  }
}
