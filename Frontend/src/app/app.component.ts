import { Component, inject } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { trigger, transition, style, animate, query } from '@angular/animations';
import { CommonModule } from '@angular/common';
import { NavbarComponent } from './components/navbar/navbar.component';
import { SidebarComponent } from './components/sidebar/sidebar.component';
import { UiFeedbackService } from './services/ui-feedback.service';
import { SidebarService } from './services/sidebar.service';
import { AuthService } from './services/auth.service';
import { TtsService } from './services/tts.service';
import { ChatService } from './services/chat.service';
import { TranslatePipe } from './pipes/t.pipe';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, CommonModule, NavbarComponent, SidebarComponent, TranslatePipe],
  template: `
    <app-navbar></app-navbar>
    @if (shouldShowSidebar()) {
      @if (!authService.isAnonymous()) {
        <app-sidebar></app-sidebar>
      } @else {
        <aside class="emotion-sidebar" [class.collapsed]="sidebarService.collapsed()">
          <div class="sidebar-header d-flex justify-content-between align-items-center">
            <h5 class="mb-0">
              <i class="bi bi-gear me-2"></i>
              <span>Settings</span>
            </h5>
            <button class="sidebar-toggle-btn" type="button" (click)="sidebarService.toggleCollapsed()" aria-label="Collapse settings panel" title="Collapse">
              <i class="bi bi-chevron-left"></i>
            </button>
          </div>
          <div class="anonymous-preferences mt-3 p-3">
            <div class="mb-3">
              <label class="form-label small mb-1">Theme</label>
              <div>
                <button class="btn btn-sm btn-outline-secondary" (click)="toggleTheme()">Toggle Dark / Light</button>
              </div>
            </div>
            <div class="mb-3">
              <label class="form-label small mb-1">Text-to-speech</label>
              <div>
                <input type="checkbox" [checked]="ttsService.isAutoSpeakEnabled()" (change)="onAutoTtsChanged($event)" id="anon-tts-app" />
                <label for="anon-tts-app" class="ms-2 small">Enable auto TTS</label>
              </div>
            </div>
            <div class="mb-3 language-section">
              <label class="form-label small mb-1">Language</label>
              <div class="btn-group w-100">
                <button class="btn btn-sm" [class.btn-primary]="chatService.language() === 'en'" [class.btn-outline-secondary]="chatService.language() !== 'en'" (click)="setLanguage('en')">English</button>
                <button class="btn btn-sm" [class.btn-primary]="chatService.language() === 'ms'" [class.btn-outline-secondary]="chatService.language() !== 'ms'" (click)="setLanguage('ms')">Malay</button>
              </div>
            </div>
          </div>
        </aside>
      }
      @if (sidebarService.collapsed()) {
        <button class="sidebar-reopen-btn" type="button" (click)="sidebarService.toggleCollapsed()" aria-label="Open side panel" title="Open panel">
          <i class="bi bi-layout-sidebar-inset"></i>
        </button>
      }
    }

    @if (!uiFeedback.online()) {
      <div class="offline-banner" role="status" aria-live="polite">
        <i class="bi bi-wifi-off me-2"></i>
        {{ 'app.offline' | t }}
      </div>
    }

    <main class="container-fluid px-4 py-3" [class.with-sidebar]="shouldShowSidebar() && !sidebarService.collapsed()">
      <div [@routeAnimations]="prepareRoute(outlet)">
        <router-outlet #outlet="outlet"></router-outlet>
      </div>
    </main>

    <div class="toast-stack" aria-live="assertive" aria-atomic="true">
      @for (toast of uiFeedback.toasts(); track toast.id) {
        <div class="toast-card" [class]="'toast-' + toast.type" role="alert">
          <div class="toast-copy">
            <strong>{{ toast.title }}</strong>
            <span>{{ toast.message }}</span>
          </div>
          <button
            type="button"
            class="btn btn-sm btn-link p-0"
            (click)="uiFeedback.dismiss(toast.id)"
            [attr.aria-label]="'app.dismiss' | t"
          >
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
      }
    </div>
  `,
  animations: [
    trigger('routeAnimations', [
      transition('* <=> *', [
        // Leaving page: fade out
        query(':leave', [
          style({ opacity: 1 }),
          animate('250ms ease-out', style({ opacity: 0 }))
        ], { optional: true }),
        // Entering page: fade in
        query(':enter', [
          style({ opacity: 0 }),
          animate('250ms 50ms ease-in', style({ opacity: 1 }))
        ], { optional: true })
      ])
    ])
  ],
  styles: [`
    main {
      min-height: calc(100vh - 70px);
      transition: margin-left 0.3s ease;
    }

    main.with-sidebar {
      margin-left: 280px;
    }

    /* Anonymous settings panel — rendered here in the app shell, so it needs
       its own fixed-left-sidebar styling (chat.component's .emotion-sidebar
       styles are component-scoped and don't reach this element). */
    .emotion-sidebar {
      position: fixed;
      left: 0;
      top: 0;
      width: 280px;
      height: 100vh;
      overflow-y: auto;
      z-index: 1040;
      padding: 1.5rem 1.1rem;
      background: linear-gradient(180deg, rgba(244, 252, 249, 0.98) 0%, rgba(230, 245, 240, 0.98) 100%);
      border-right: 1px solid rgba(42, 157, 143, 0.12);
      box-shadow: 4px 0 24px rgba(24, 66, 60, 0.05);
      transition: transform 0.32s var(--ease-soft);
    }

    .emotion-sidebar.collapsed {
      transform: translateX(-100%);
    }

    /* collapse / re-open controls */
    .sidebar-toggle-btn,
    .sidebar-reopen-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      border-radius: 10px;
      border: 1px solid var(--border-color);
      background: rgba(255, 255, 255, 0.85);
      color: var(--primary-color);
      cursor: pointer;
      transition: transform 0.2s ease, background 0.2s ease, box-shadow 0.2s ease;
    }

    .sidebar-toggle-btn:hover,
    .sidebar-reopen-btn:hover {
      background: var(--nav-hover-bg);
      transform: translateY(-1px);
    }

    .sidebar-reopen-btn {
      position: fixed;
      left: 14px;
      top: 88px;
      z-index: 1041;
      width: 42px;
      height: 42px;
      font-size: 1.1rem;
      box-shadow: var(--shadow-soft);
      animation: softEntrance 0.3s var(--ease-soft) both;
    }

    .emotion-sidebar .sidebar-header h5 {
      color: var(--text-primary);
      font-weight: 700;
    }

    .emotion-sidebar .anonymous-preferences {
      background: rgba(255, 255, 255, 0.72);
      border: 1px solid var(--border-color);
      border-radius: 16px;
    }

    .emotion-sidebar .form-label {
      color: var(--text-secondary);
      font-weight: 600;
    }

    .emotion-sidebar .language-section .btn-group .btn {
      font-weight: 600;
    }

    @media (max-width: 768px) {
      main {
        margin-left: 0;
        padding: 0.55rem !important;
      }

      /* On small screens it stacks at the top instead of covering the screen */
      .emotion-sidebar {
        position: static;
        width: 100%;
        height: auto;
        border-right: none;
        border-bottom: 1px solid rgba(42, 157, 143, 0.12);
        box-shadow: none;
      }

      .emotion-sidebar.collapsed {
        display: none;
        transform: none;
      }

      .sidebar-reopen-btn {
        top: 72px;
      }
    }

    @media (max-width: 480px) {
      main {
        padding: 0.4rem !important;
      }
    }

    .offline-banner {
      margin: 0.75rem 1rem 0;
      padding: 0.75rem 1rem;
      border-radius: 14px;
      border: 1px solid rgba(245, 158, 11, 0.45);
      background: rgba(245, 158, 11, 0.16);
      color: #8a5a00;
      font-weight: 600;
    }

    .toast-stack {
      position: fixed;
      right: 1rem;
      bottom: 1rem;
      z-index: 1300;
      display: grid;
      gap: 0.7rem;
      width: min(360px, calc(100vw - 2rem));
    }

    .toast-card {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 0.75rem;
      border-radius: 16px;
      padding: 0.85rem 1rem;
      border: 1px solid var(--border-color);
      background: linear-gradient(180deg, rgba(255, 255, 255, 0.97), rgba(240, 250, 247, 0.99));
      color: var(--text-primary);
      backdrop-filter: blur(10px);
      box-shadow: var(--shadow-lift);
      animation: softEntrance 0.42s var(--ease-soft) both;
    }

    .toast-copy {
      display: grid;
      gap: 0.2rem;
      line-height: 1.4;
    }

    .toast-copy strong {
      font-size: 0.88rem;
    }

    .toast-copy span {
      color: var(--text-secondary);
      font-size: 0.82rem;
    }

    .toast-success {
      border-color: rgba(24, 199, 143, 0.35);
    }

    .toast-danger {
      border-color: rgba(239, 93, 108, 0.4);
    }

    .toast-warning {
      border-color: rgba(245, 158, 11, 0.4);
    }

    .toast-info {
      border-color: rgba(53, 214, 193, 0.35);
    }
  `]
})
export class AppComponent {
  title = 'RadAI';
  uiFeedback = inject(UiFeedbackService);
  router = inject(Router);
  authService = inject(AuthService);
  ttsService = inject(TtsService);
  chatService = inject(ChatService);
  sidebarService = inject(SidebarService);

  constructor() {
    // Apply the saved theme as early as possible so every page renders in the right mode.
    try {
      const theme = localStorage.getItem('radai-theme') === 'dark' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', theme);
    } catch (e) {}
  }

  shouldShowSidebar(): boolean {
    const path = this.router.url.split(/[?#]/)[0] || '';
    return path === '/chat';
  }

  prepareRoute(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData['animation'];
  }

  toggleTheme(): void {
    try {
      const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
      const next = isDark ? 'light' : 'dark';
      document.documentElement.setAttribute('data-theme', next);
      try { localStorage.setItem('radai-theme', next); } catch (e) {}
      this.uiFeedback.success('Theme', next === 'dark' ? 'Dark mode' : 'Light mode');
    } catch (e) {
      console.warn('Unable to toggle theme', e);
      this.uiFeedback.error('Theme', 'Unable to change theme.');
    }
  }

  onAutoTtsChanged(event: Event): void {
    const checked = (event.target as HTMLInputElement)?.checked ?? false;
    this.ttsService.setAutoSpeakEnabled(checked);
    this.uiFeedback.success('Settings', checked ? 'Auto TTS enabled.' : 'Auto TTS disabled.');
  }

  setLanguage(lang: string): void {
    this.chatService.setLanguage(lang);
    try { localStorage.setItem('socializer_language', lang); } catch (e) {}
    this.uiFeedback.success('Language', lang === 'en' ? 'English selected' : 'Malay selected');
  }
}
