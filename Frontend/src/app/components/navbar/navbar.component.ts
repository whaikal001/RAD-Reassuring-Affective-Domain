import { Component, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LanguageService } from '../../services/language.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  template: `
    <nav class="navbar navbar-expand-lg navbar-dark sticky-top">
      <div class="container">
        <a class="navbar-brand d-flex align-items-center gap-2" routerLink="/">
          <img class="brand-logo" src="/assets/Gemini_Generated_Image_ym28zxym28zxym28-removebg-preview.png" alt="SocializerAI" width="56" height="56" />
          <span class="fw-bold">SocializerAI</span>
        </a>
        
        <button class="navbar-toggler" type="button" (click)="toggleSidebar()">
          <i class="bi" [class]="sidebarOpen() ? 'bi-x-lg' : 'bi-list'"></i>
        </button>
        
        <div class="navbar-collapse" [class.show]="sidebarOpen()">
          <ul class="navbar-nav me-auto">
            @if (authService.isAuthenticated()) {
              @if (!authService.hasRole('ADMIN')) {
                <li class="nav-item">
                  <a class="nav-link" routerLink="/chat" routerLinkActive="active">
                    <i class="bi bi-chat-dots me-1"></i>{{ 'nav.chat' | t }}
                  </a>
                </li>
              }
              @if (!authService.isAnonymous()) {
                @if (authService.hasRole('ADMIN')) {
                  <li class="nav-item">
                    <a class="nav-link" routerLink="/admin" routerLinkActive="active">
                      <i class="bi bi-graph-up me-1"></i>Admin
                    </a>
                  </li>
                }
                <li class="nav-item">
                  <a class="nav-link" routerLink="/history" routerLinkActive="active">
                    <i class="bi bi-clock-history me-1"></i>{{ 'nav.recentChat' | t }}
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" routerLink="/reports" routerLinkActive="active">
                    <i class="bi bi-file-earmark-text me-1"></i>{{ 'nav.report' | t }}
                  </a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" routerLink="/profile" routerLinkActive="active">
                    <i class="bi bi-person-gear me-1"></i>{{ 'nav.profile' | t }}
                  </a>
                </li>
              }
            }
          </ul>
          
          <ul class="navbar-nav">
            @if (!isChatPage) {
              <li class="nav-item dropdown me-2">
                <a class="nav-link dropdown-toggle d-flex align-items-center" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                  <span class="flag me-1">{{ languageService.language() === 'en' ? '🇬🇧' : '🇲🇾' }}</span>
                  <span class="lang-label">{{ languageService.language() === 'en' ? ('common.english' | t) : ('common.malay' | t) }}</span>
                </a>
                <ul class="dropdown-menu dropdown-menu-end">
                  <li><button class="dropdown-item d-flex align-items-center" (click)="setLanguage('en')"><span class="me-2">🇬🇧</span>{{ 'common.english' | t }}</button></li>
                  <li><button class="dropdown-item d-flex align-items-center" (click)="setLanguage('ms')"><span class="me-2">🇲🇾</span>{{ 'common.malay' | t }}</button></li>
                </ul>
              </li>
            }
            @if (authService.isAuthenticated()) {
              <li class="nav-item d-flex align-items-center me-2">
                <span class="session-pill" [class.anonymous]="authService.isAnonymous()">
                  {{ authService.isAnonymous() ? ('nav.sessionAnonymous' | t) : ('nav.sessionRegistered' | t) }}
                </span>
              </li>
              <li class="nav-item">
                <button class="btn btn-outline-light btn-sm" (click)="logout()">
                  <i class="bi bi-box-arrow-right me-1"></i>{{ 'nav.logout' | t }}
                </button>
              </li>
            } @else {
              @if (!isAuthPage()) {
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
  `,
  styles: [`
    .navbar {
      padding: 0.9rem 0;
    }

    .navbar-brand {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
    }

    .brand-mark {
      width: 2.4rem;
      height: 2.4rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 0.85rem;
      background: linear-gradient(145deg, rgba(111, 124, 255, 0.24), rgba(53, 214, 193, 0.22));
      border: 1px solid rgba(165, 178, 224, 0.18);
      color: #f7f9ff;
      -webkit-text-fill-color: initial;
      box-shadow: 0 10px 24px rgba(25, 38, 89, 0.22);
    }
    
    .nav-link {
      color: var(--text-secondary) !important;
      transition: color 0.25s ease, background 0.25s ease, border-color 0.25s ease;
      border-radius: 999px;
      padding: 0.55rem 0.95rem !important;
      border: 1px solid transparent;
      
      &:hover, &.active {
        color: var(--text-primary) !important;
        background: rgba(255, 255, 255, 0.05);
        border-color: rgba(165, 178, 224, 0.12);
      }
    }

    .session-pill {
      display: inline-flex;
      align-items: center;
      padding: 0.45rem 0.85rem;
      border-radius: 999px;
      background: rgba(24, 199, 143, 0.14);
      border: 1px solid rgba(24, 199, 143, 0.28);
      color: #baf8df;
      font-size: 0.85rem;
      white-space: nowrap;
    }

    .session-pill.anonymous {
      background: rgba(245, 158, 11, 0.14);
      border-color: rgba(245, 158, 11, 0.4);
      color: #fcd34d;
    }

    @media (max-width: 991px) {
      .navbar-collapse {
        margin-top: 1rem;
        padding: 1rem;
        border-radius: 1.25rem;
        background: rgba(13, 17, 29, 0.88);
        border: 1px solid rgba(165, 178, 224, 0.1);
        max-height: 0;
        overflow: hidden;
        opacity: 0;
        transition: max-height 0.3s ease, opacity 0.3s ease, padding 0.3s ease;
      }

      .navbar-collapse.show {
        max-height: 600px;
        opacity: 1;
        padding: 1rem;
      }

      .navbar-toggler {
        border: none;
        color: var(--text-secondary);
        font-size: 1.25rem;
        transition: color 0.2s ease;

        &:focus {
          box-shadow: none;
          color: var(--text-primary);
        }

        &:hover {
          color: var(--text-primary);
        }
      }

      .nav-link {
        padding: 0.7rem 0.9rem !important;
        font-size: 0.98rem;
      }

      .btn {
        min-height: 44px;
      }

      .session-pill {
        margin: 0.35rem 0 0.6rem;
      }
    }

    @media (max-width: 576px) {
      .navbar-brand {
        gap: 0.55rem;
        font-size: 1.18rem;
      }

      .brand-mark {
        width: 2.1rem;
        height: 2.1rem;
      }
      .brand-logo { width: 40px; height: 40px; }
    }

    .brand-logo {
      width: 56px;
      height: 56px;
      border-radius: 10px;
      background: transparent;
      object-fit: cover;
      box-shadow: 0 8px 24px rgba(31, 44, 115, 0.18);
    }
  `]
})
export class NavbarComponent implements OnInit {
  sidebarOpen = signal(false);

  constructor(public authService: AuthService, public languageService: LanguageService, public router: Router) {}

  ngOnInit(): void {
    // Load sidebar state from localStorage, default to false (hidden)
    const saved = localStorage.getItem('sidebarOpen');
    this.sidebarOpen.set(saved ? JSON.parse(saved) : false);

    // Close sidebar on route change
    this.router.events.subscribe(() => {
      if (this.sidebarOpen()) {
        this.sidebarOpen.set(false);
      }
    });
  }

  toggleSidebar(): void {
    this.sidebarOpen.set(!this.sidebarOpen());
    // Save to localStorage
    localStorage.setItem('sidebarOpen', JSON.stringify(this.sidebarOpen()));
  }

  logout(): void {
    // Reset sidebar on logout
    this.sidebarOpen.set(false);
    localStorage.removeItem('sidebarOpen');
    this.authService.logout();
  }

  setLanguage(lang: 'en' | 'ms'): void {
    this.languageService.setLanguage(lang);
  }

  get isChatPage(): boolean {
    return this.router.url?.startsWith('/chat');
  }

  isAuthPage(): boolean {
    const url = this.router.url || '';
    return url.startsWith('/login') || url.startsWith('/register');
  }
}
