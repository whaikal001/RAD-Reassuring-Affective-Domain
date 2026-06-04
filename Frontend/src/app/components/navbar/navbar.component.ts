import { Component, signal, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LanguageService } from '../../services/language.service';
import { SidebarService } from '../../services/sidebar.service';
import { ThemeService } from '../../services/theme.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  template: `
    @if (!isLandingPage()) {
      <nav class="navbar navbar-expand-lg sticky-top">
        <div class="container-fluid px-3">
          <button class="navbar-menu-btn" type="button" (click)="toggleSidebar()" aria-label="Toggle sidebar">
            <i class="bi bi-list"></i>
          </button>

          <a class="navbar-brand d-flex align-items-center gap-2" routerLink="/">
            <img class="brand-logo" src="/assets/RadAIBrandMark.png" alt="RadAI" width="40" height="40" />
            <span class="fw-bold">RadAI</span>
          </a>
          
          <div class="navbar-collapse ms-auto">
            <ul class="navbar-nav">
              <li class="nav-item">
                <button
                  class="theme-toggle-btn"
                  type="button"
                  (click)="themeService.toggle()"
                  [attr.aria-label]="themeService.isDark() ? 'Switch to light mode' : 'Switch to dark mode'"
                  [title]="themeService.isDark() ? 'Light mode' : 'Dark mode'"
                >
                  <i class="bi" [class.bi-sun-fill]="themeService.isDark()" [class.bi-moon-stars-fill]="!themeService.isDark()"></i>
                </button>
              </li>
              <!-- Account controls are hidden on the login/register pages: a leftover
                   session can still report authenticated there, which made a stale
                   "recent account" avatar + Logout appear on the auth screens. -->
              @if (!isAuthPage()) {
                @if (authService.isAuthenticated()) {
                  @if (authService.isRegistered() && !authService.hasRole('ADMIN')) {
                    <li class="nav-item">
                      <a class="profile-chip" routerLink="/profile" title="Profile">
                        @if (authService.avatarUrl()) {
                          <img [src]="authService.avatarUrl()" alt="Profile" />
                        } @else {
                          <span class="profile-initials">{{ navInitials() }}</span>
                        }
                      </a>
                    </li>
                  }
                  <li class="nav-item">
                    <button class="btn btn-outline-light btn-sm" (click)="logout()">
                      <i class="bi bi-box-arrow-right me-1"></i>{{ 'nav.logout' | t }}
                    </button>
                  </li>
                } @else {
                  <li class="nav-item">
                    <a class="nav-link" routerLink="/login">{{ 'nav.login' | t }}</a>
                  </li>
                  <li class="nav-item">
                    <a class="btn btn-primary btn-sm ms-2" routerLink="/register">{{ 'nav.signUp' | t }}</a>
                  </li>
                }
              }
            </ul>
          </div>
        </div>
      </nav>
    }
  `,
  styles: [`
    .navbar {
      padding: 0.75rem 0;
      background: var(--card-bg);
      border-bottom: 1px solid var(--border-color);
    }

    .profile-chip {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      overflow: hidden;
      text-decoration: none;
      background: linear-gradient(135deg, var(--mint-500), var(--mint-600));
      border: 2px solid #ffffff;
      box-shadow: 0 4px 14px rgba(27, 122, 110, 0.28);
      transition: transform 0.2s ease, box-shadow 0.2s ease;

      &:hover {
        transform: translateY(-1px) scale(1.05);
        box-shadow: 0 6px 18px rgba(27, 122, 110, 0.36);
      }

      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .profile-initials {
        color: #ffffff;
        font-weight: 800;
        font-size: 0.95rem;
      }
    }

    .navbar-menu-btn {
      background: none;
      border: none;
      color: var(--text-primary);
      font-size: 1.5rem;
      cursor: pointer;
      padding: 0.5rem;
      border-radius: 0.5rem;
      transition: all 0.25s ease;
      display: none;

      &:hover {
        background: var(--nav-hover-bg);
        color: var(--primary-color);
      }

      @media (max-width: 768px) {
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }

    .navbar-brand {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      color: var(--text-primary);
      text-decoration: none;
    }

    .brand-logo {
      width: 40px;
      height: 40px;
      border-radius: 11px;
      background: transparent;
      object-fit: cover;
      box-shadow: 0 6px 18px rgba(31, 44, 115, 0.18);
    }
    
    .navbar-collapse {
      display: flex !important;
    }

    .navbar-nav {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .theme-toggle-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      border-radius: 50%;
      border: 1px solid var(--border-color);
      background: var(--nav-hover-bg, transparent);
      color: var(--text-secondary);
      font-size: 1.15rem;
      cursor: pointer;
      transition: color 0.25s ease, background 0.25s ease, border-color 0.25s ease, transform 0.2s ease;

      &:hover {
        color: var(--primary-color);
        border-color: var(--primary-color);
        transform: translateY(-1px);
      }

      &:focus-visible {
        outline: 2px solid var(--primary-color);
        outline-offset: 2px;
      }
    }

    .nav-link {
      color: var(--text-secondary) !important;
      transition: color 0.25s ease, background 0.25s ease, border-color 0.25s ease;
      border-radius: 999px;
      padding: 0.55rem 0.95rem !important;
      border: 1px solid transparent;
      
      &:hover, &.active {
        color: var(--text-primary) !important;
        background: var(--nav-hover-bg);
        border-color: var(--border-color);
      }
    }

    @media (max-width: 768px) {
      .navbar {
        padding: 0.6rem 0;
      }

      .navbar-brand {
        gap: 0.55rem;
        font-size: 1.1rem;
      }

      .brand-logo { 
        width: 40px; 
        height: 40px; 
      }

      .nav-link {
        padding: 0.6rem 0.8rem !important;
        font-size: 0.9rem;
      }
    }
  `]
})
export class NavbarComponent implements OnInit {
  authService = inject(AuthService);
  languageService = inject(LanguageService);
  sidebarService = inject(SidebarService);
  themeService = inject(ThemeService);
  router = inject(Router);

  ngOnInit(): void {
    // ThemeService already applied the locally-stored theme on construction.
    // For registered users, pull their saved preference so it follows them across devices.
    this.themeService.syncFromProfile();
  }

  navInitials(): string {
    const name = (this.authService.getDisplayName() || '').trim();
    if (!name) {
      return 'U';
    }
    const parts = name.split(/\s+/);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'U';
  }

  toggleSidebar(): void {
    this.sidebarService.toggle();
  }

  logout(): void {
    // Reset sidebar on logout
    this.sidebarService.close();
    this.authService.logout();
  }

  get isChatPage(): boolean {
    return this.router.url?.startsWith('/chat');
  }

  isAuthPage(): boolean {
    const url = this.router.url || '';
    return url.startsWith('/login') || url.startsWith('/register');
  }

  isLandingPage(): boolean {
    const url = this.router.url || '';
    // Check if on landing page (including hash sections like /#features or /#how-it-works)
    return url === '/' || url === '' || url.startsWith('/#') || url.startsWith('?');
  }
}
