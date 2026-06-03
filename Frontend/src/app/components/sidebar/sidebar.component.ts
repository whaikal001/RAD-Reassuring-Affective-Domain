import { Component, signal, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ProfileService } from '../../services/profile.service';
import { SidebarService } from '../../services/sidebar.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  template: `
    <aside class="sidebar" [class.show]="sidebarService.isSidebarOpen()" [class.collapsed]="sidebarService.collapsed()">
      <button class="sidebar-collapse-btn" type="button" (click)="sidebarService.toggleCollapsed()" aria-label="Collapse sidebar" title="Collapse">
        <i class="bi bi-chevron-left"></i>
      </button>
      <!-- Branding (use user avatar for registered users, heart for anonymous) -->
      <div class="sidebar-branding">
        <div class="branding-logo">
          <div class="logo-circle">
            <ng-container *ngIf="authService.isAuthenticated(); else guestBranding">
              <ng-container *ngIf="userAvatar() as avatar; else fallbackAvatar">
                <img [src]="avatar" alt="User avatar" class="brand-logo" />
              </ng-container>
              <ng-template #fallbackAvatar>
                <i class="bi bi-person-circle"></i>
              </ng-template>
            </ng-container>
            <ng-template #guestBranding>
              <i class="bi bi-heart-fill"></i>
            </ng-template>
          </div>
          <div class="logo-text">
            <h4 class="app-name mb-0">RadAI</h4>
            <small class="text-muted">Your Mental Wellness Companion</small>
          </div>
        </div>
      </div>

      <nav class="sidebar-nav">
        <div class="nav-section">
          <h3 class="nav-section-title">{{ 'sidebar.main' | t }}</h3>

          <ng-container *ngIf="authService.isAuthenticated()">
            <a routerLink="/chat" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item">
              <i class="bi bi-chat-left-dots"></i>
              <span class="nav-label">{{ 'sidebar.chat' | t }}</span>
            </a>

            <a routerLink="/history" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item">
              <i class="bi bi-clock-history"></i>
              <span class="nav-label">{{ 'sidebar.history' | t }}</span>
            </a>

            <a routerLink="/dashboard" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item">
              <i class="bi bi-graph-up"></i>
              <span class="nav-label">{{ 'sidebar.dashboard' | t }}</span>
            </a>

            <a routerLink="/reports" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item">
              <i class="bi bi-file-earmark-text"></i>
              <span class="nav-label">{{ 'sidebar.reports' | t }}</span>
            </a>
          </ng-container>
        </div>

        <div class="nav-section">
          <h3 class="nav-section-title">{{ 'sidebar.system' | t }}</h3>

          <a routerLink="/profile" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item">
            <i class="bi bi-person-circle"></i>
            <span class="nav-label">{{ 'sidebar.profile' | t }}</span>
          </a>

          <button (click)="toggleTheme()" class="nav-item nav-button" [title]="isDarkMode() ? 'Switch to Light Mode' : 'Switch to Dark Mode'">
            <i class="bi" [class]="isDarkMode() ? 'bi-sun' : 'bi-moon-stars'"></i>
            <span class="nav-label">{{ isDarkMode() ? 'Light' : 'Dark' }}</span>
          </button>

          <a routerLink="/profile" fragment="settings" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item">
            <i class="bi bi-gear"></i>
            <span class="nav-label">{{ 'sidebar.settings' | t }}</span>
          </a>

          <ng-container *ngIf="isAdmin()">
            <a routerLink="/admin" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item admin-item">
              <i class="bi bi-shield-lock"></i>
              <span class="nav-label">{{ 'sidebar.admin' | t }}</span>
            </a>
          </ng-container>
        </div>
      </nav>

      <button (click)="sidebarService.close()" class="sidebar-close" aria-label="Close sidebar">
        <i class="bi bi-x-lg"></i>
      </button>
    </aside>

    <div class="sidebar-overlay" [class.show]="sidebarService.isSidebarOpen()" (click)="sidebarService.close()"></div>
  `,
  styles: [`
    .sidebar {
      position: fixed;
      left: 0;
      top: 0;
      width: 280px;
      height: 100vh;
      background: linear-gradient(180deg, rgba(244, 252, 249, 0.98) 0%, rgba(230, 245, 240, 0.98) 100%);
      border-right: 1px solid rgba(42, 157, 143, 0.10);
      overflow-y: auto;
      z-index: 1040;
      transition: transform 0.3s ease;
      padding: 1.5rem 1rem;
      display: flex;
      flex-direction: column;

      @media (max-width: 768px) {
        width: 100%;
        transform: translateX(-100%);

        &.show {
          transform: translateX(0);
        }
      }

      /* Desktop collapse — slide out so the page reclaims full width */
      @media (min-width: 769px) {
        transition: transform 0.32s cubic-bezier(0.22, 1, 0.36, 1);

        &.collapsed {
          transform: translateX(-100%);
        }
      }
    }

    .sidebar-collapse-btn {
      position: absolute;
      top: 0.85rem;
      right: 0.7rem;
      width: 30px;
      height: 30px;
      border-radius: 8px;
      border: 1px solid rgba(42, 157, 143, 0.18);
      background: rgba(255, 255, 255, 0.7);
      color: #1b7a6e;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: background 0.2s ease, transform 0.2s ease;
      z-index: 2;

      &:hover {
        background: rgba(208, 240, 232, 0.85);
        transform: translateY(-1px);
      }

      @media (max-width: 768px) {
        display: none;
      }
    }

    .sidebar-nav {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .nav-section {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .nav-section-title {
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: #7dc4a8;
      padding: 0 0.75rem;
      margin: 0;
      opacity: 0.7;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.78rem 0.9rem;
      border-radius: 999px;
      color: var(--text-secondary);
      text-decoration: none;
      border: 1px solid transparent;
      transition: all 0.25s ease;
      background: transparent;
      border: none;
      cursor: pointer;
      font-family: inherit;
      font-size: inherit;
      text-align: left;

      &:hover {
        color: var(--text-primary);
        background: rgba(208, 240, 232, 0.55);
        border-color: rgba(42, 157, 143, 0.14);
      }

      &.active {
        color: #1b7a6e;
        background: rgba(208, 240, 232, 0.95);
        border-color: rgba(42, 157, 143, 0.22);
        font-weight: 700;

        i {
          color: #2a9d8f;
        }
      }

      i {
        font-size: 1.25rem;
        min-width: 1.5rem;
        text-align: center;
        color: #2a9d8f;
        transition: color 0.25s ease;
      }
    }

    .nav-button {
      padding: 0.78rem 0.9rem;
      cursor: pointer;
    }

    .nav-label {
      flex: 1;
      white-space: nowrap;
    }

    .admin-item {
      border-color: rgba(42, 157, 143, 0.20);
      color: #1b7a6e;

      &:hover {
        background: rgba(208, 240, 232, 0.8);
      }

      &.active {
        background: rgba(208, 240, 232, 1);
      }
    }

    .sidebar-close {
      display: none;
      background: none;
      border: none;
      color: #1b7a6e;
      font-size: 1.5rem;
      cursor: pointer;
      position: absolute;
      top: 1rem;
      right: 1rem;
      padding: 0;
      width: 2rem;
      height: 2rem;
      align-items: center;
      justify-content: center;

      @media (max-width: 768px) {
        display: flex;
      }
    }

    .sidebar-overlay {
      display: none;
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(42, 157, 143, 0.18);
      z-index: 1035;

      @media (max-width: 768px) {
        display: block;
        opacity: 0;
        pointer-events: none;
        transition: opacity 0.3s ease;

        &.show {
          opacity: 1;
          pointer-events: auto;
        }
      }
    }

    ::-webkit-scrollbar {
      width: 6px;
    }

    ::-webkit-scrollbar-track {
      background: transparent;
    }

    ::-webkit-scrollbar-thumb {
      background: rgba(42, 157, 143, 0.22);
      border-radius: 3px;

      &:hover {
        background: rgba(42, 157, 143, 0.38);
      }
    }
  `]
})
export class SidebarComponent implements OnInit, OnDestroy {
  isDarkMode = signal(localStorage.getItem('radai-theme') === 'dark');
  userAvatar = signal<string | null>(null);

  authService = inject(AuthService);
  profileService = inject(ProfileService);
  sidebarService = inject(SidebarService);
  router = inject(Router);

  ngOnInit() {
    // Load theme preference from profile
    this.profileService.getOrCreatePreferences().subscribe({
      next: (preferences) => {
        this.isDarkMode.set(preferences.theme === 'dark');
        this.applyTheme(preferences.theme);
      },
      error: () => {
        // Fallback to dark theme if preference fetch fails
        this.isDarkMode.set(true);
        this.applyTheme('dark');
      }
    });

    // Close sidebar on route change
    this.router.events.subscribe(() => {
      this.sidebarService.close();
    });

    // Load current user avatar for registered users (photo is stored client-side,
    // so fall back to the auth service's cached avatar).
    if (this.authService.isAuthenticated()) {
      this.userAvatar.set(this.authService.avatarUrl());
      this.profileService.getCurrentUser().subscribe({
        next: (u) => {
          this.userAvatar.set(u?.avatarUrl || this.authService.avatarUrl());
        },
        error: () => {
          this.userAvatar.set(this.authService.avatarUrl());
        }
      });
    }
  }

  ngOnDestroy() {
    // Cleanup
  }

  toggleTheme() {
    const newTheme = this.isDarkMode() ? 'light' : 'dark';
    this.isDarkMode.set(!this.isDarkMode());
    this.applyTheme(newTheme);
    
    // Save preference
    this.profileService.updatePreferences({ theme: newTheme }).subscribe();
  }

  applyTheme(theme: string) {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('radai-theme', theme);
  }

  isAdmin(): boolean {
    return this.authService.hasRole('admin');
  }
}
