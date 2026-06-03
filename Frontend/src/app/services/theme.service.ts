import { Injectable, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { ProfileService } from './profile.service';

/**
 * Single source of truth for the light/dark theme across the entire UI.
 *
 * The theme is applied as `data-theme` on the document root (which all component styles key off)
 * and persisted to `localStorage`. For registered users it is also synced to/from their profile
 * preferences so the choice follows them across devices.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private static readonly STORAGE_KEY = 'radai-theme';

  private authService = inject(AuthService);
  private profileService = inject(ProfileService);

  /** true = dark mode. Components read this signal to render the correct toggle icon. */
  readonly isDark = signal<boolean>(this.readStoredDark());

  constructor() {
    // Apply immediately so the whole app renders in the right mode on first paint.
    this.applyToDom(this.isDark());
  }

  /** Flip the theme, apply it everywhere, and persist (local + server for registered users). */
  toggle(): void {
    this.setDark(!this.isDark(), true);
  }

  /** Apply a theme value ('dark' | 'light'), e.g. when loaded from profile preferences. */
  setTheme(theme: string | null | undefined, persist = false): void {
    this.setDark(theme === 'dark', persist);
  }

  /** For registered users, load the saved theme from their profile preferences. */
  syncFromProfile(): void {
    if (!this.canPersist()) {
      return;
    }
    this.profileService.getOrCreatePreferences().subscribe({
      next: (prefs) => {
        if (prefs?.theme) {
          this.setTheme(prefs.theme, false);
        }
      },
      error: () => { /* keep the locally stored theme */ }
    });
  }

  private setDark(dark: boolean, persist: boolean): void {
    this.isDark.set(dark);
    this.applyToDom(dark);
    if (persist && this.canPersist()) {
      this.profileService.updatePreferences({ theme: dark ? 'dark' : 'light' })
        .subscribe({ error: () => { /* local theme already applied; ignore */ } });
    }
  }

  private applyToDom(dark: boolean): void {
    const value = dark ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', value);
    try {
      localStorage.setItem(ThemeService.STORAGE_KEY, value);
    } catch { /* storage unavailable */ }
  }

  private readStoredDark(): boolean {
    try {
      return localStorage.getItem(ThemeService.STORAGE_KEY) === 'dark';
    } catch {
      return false;
    }
  }

  private canPersist(): boolean {
    return this.authService.isAuthenticated() && !this.authService.isAnonymous();
  }
}
