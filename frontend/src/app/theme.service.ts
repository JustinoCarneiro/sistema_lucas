// frontend/src/app/theme.service.ts
import { Injectable, signal } from '@angular/core';

/**
 * Gerencia o tema claro/escuro do app.
 * - Persiste a escolha em localStorage ('theme' = 'light' | 'dark').
 * - Aplica/remove a classe `.dark` no <html> (variante `dark:` do Tailwind).
 * O index.html já aplica o tema antes do render para evitar flash.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly storageKey = 'theme';
  readonly isDark = signal<boolean>(this.readInitial());

  constructor() {
    this.apply(this.isDark());
  }

  toggle(): void {
    this.setDark(!this.isDark());
  }

  setDark(dark: boolean): void {
    this.isDark.set(dark);
    localStorage.setItem(this.storageKey, dark ? 'dark' : 'light');
    this.apply(dark);
  }

  private readInitial(): boolean {
    const saved = localStorage.getItem(this.storageKey);
    if (saved) return saved === 'dark';
    return window.matchMedia('(prefers-color-scheme: dark)').matches;
  }

  private apply(dark: boolean): void {
    document.documentElement.classList.toggle('dark', dark);
  }
}
