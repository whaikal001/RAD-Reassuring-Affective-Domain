import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { trigger, transition, style, animate, query, group } from '@angular/animations';
import { NavbarComponent } from './components/navbar/navbar.component';
import { UiFeedbackService } from './services/ui-feedback.service';
import { TranslatePipe } from './pipes/t.pipe';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NavbarComponent, TranslatePipe],
  template: `
    <app-navbar></app-navbar>

    @if (!uiFeedback.online()) {
      <div class="offline-banner" role="status" aria-live="polite">
        <i class="bi bi-wifi-off me-2"></i>
        {{ 'app.offline' | t }}
      </div>
    }

    <main class="container-fluid px-4 py-3">
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
    }

    @media (max-width: 768px) {
      main {
        padding: 0.55rem !important;
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
      color: #ffdf9b;
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
      border-radius: 14px;
      padding: 0.8rem 0.9rem;
      border: 1px solid var(--border-color);
      background: rgba(11, 14, 24, 0.92);
      backdrop-filter: blur(10px);
      box-shadow: var(--shadow-soft);
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
  title = 'SocializerAI';

  constructor(public uiFeedback: UiFeedbackService) {}

  prepareRoute(outlet: RouterOutlet) {
    return outlet && outlet.activatedRouteData && outlet.activatedRouteData['animation'];
  }
}
