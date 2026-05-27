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
    <aside class="sidebar" [class.show]="sidebarService.isSidebarOpen()">
      <nav class="sidebar-nav">
        <div class="nav-section">
          <h3 class="nav-section-title">{{ 'sidebar.main' | t }}</h3>
          
          @if (authService.isAuthenticated()) {
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
          }
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

          @if (isAdmin()) {
            <a routerLink="/admin" routerLinkActive="active" (click)="sidebarService.close()" class="nav-item admin-item">
              <i class="bi bi-shield-lock"></i>
              <span class="nav-label">{{ 'sidebar.admin' | t }}</span>
            </a>
          }
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
      background: var(--sidebar-bg);
      border-right: 1px solid var(--border-color);
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
      color: var(--text-secondary);
      padding: 0 0.75rem;
      margin: 0;
      opacity: 0.7;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem 0.75rem;
      border-radius: 0.75rem;
      color: var(--text-secondary);
      text-decoration: none;
      border: 1px solid transparent;
      transition: all 0.25s ease;
      background: none;
      border: none;
      cursor: pointer;
      font-family: inherit;
      font-size: inherit;
      text-align: left;

      &:hover {
        color: var(--text-primary);
        background: var(--nav-hover-bg);
        border-color: var(--border-color);
      }

      &.active {
        color: var(--primary-color);
        background: var(--nav-active-bg);
        border-color: var(--primary-color);
        font-weight: 600;

        i {
          color: var(--primary-color);
        }
      }

      i {
        font-size: 1.25rem;
        min-width: 1.5rem;
        text-align: center;
        color: var(--text-secondary);
        transition: color 0.25s ease;
      }
    }

    .nav-button {
      padding: 0.75rem;
      cursor: pointer;
    }

    .nav-label {
      flex: 1;
      white-space: nowrap;
    }

    .admin-item {
      border-color: var(--danger-color);
      color: var(--danger-color);

      &:hover {
        background: rgba(239, 93, 108, 0.1);
      }

      &.active {
        background: rgba(239, 93, 108, 0.15);
      }
    }

    .sidebar-close {
      display: none;
      background: none;
      border: none;
      color: var(--text-primary);
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
      background: rgba(0, 0, 0, 0.5);
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
      background: var(--border-color);
      border-radius: 3px;

      &:hover {
        background: var(--text-secondary);
      }
    }
  `]
})
export class SidebarComponent implements OnInit, OnDestroy {
  isDarkMode = signal(true);

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
