import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';

type VerifyState = 'verifying' | 'success' | 'error' | 'no-token';

/**
 * Landing page for the link in the verification email (/verify-email?token=...).
 * Auto-confirms the token, shows the outcome, and offers a resend if it failed
 * or no token was supplied.
 */
@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="verify-container">
      <div class="verify-card">
        <img src="/assets/RadAIBrandMark.png" alt="RadAI" class="verify-brand" width="64" height="64" />

        @switch (state()) {
          @case ('verifying') {
            <div class="verify-icon"><span class="spinner-border"></span></div>
            <h1 class="verify-title">Verifying your email…</h1>
            <p class="verify-text">Hang tight, this only takes a moment.</p>
          }
          @case ('success') {
            <div class="verify-icon success"><i class="bi bi-check-circle-fill"></i></div>
            <h1 class="verify-title">Email verified!</h1>
            <p class="verify-text">Your account is all set. Thanks for confirming your email.</p>
            <a class="btn btn-primary btn-lg w-100" routerLink="/chat">Continue to RadAI</a>
          }
          @case ('error') {
            <div class="verify-icon error"><i class="bi bi-x-circle-fill"></i></div>
            <h1 class="verify-title">Verification failed</h1>
            <p class="verify-text">{{ message() }}</p>
            <ng-container *ngTemplateOutlet="resendForm"></ng-container>
          }
          @case ('no-token') {
            <div class="verify-icon"><i class="bi bi-envelope-paper-fill"></i></div>
            <h1 class="verify-title">Verify your email</h1>
            <p class="verify-text">Enter your email and we'll send you a fresh verification link.</p>
            <ng-container *ngTemplateOutlet="resendForm"></ng-container>
          }
        }

        <p class="verify-footer">
          <a routerLink="/login">Back to sign in</a>
        </p>
      </div>
    </div>

    <ng-template #resendForm>
      @if (resendSent()) {
        <div class="alert alert-success w-100"><i class="bi bi-check-circle me-2"></i>{{ resendMessage() }}</div>
      } @else {
        <div class="resend-form w-100">
          <input type="email" class="form-control form-control-lg mb-2" placeholder="your@email.com"
                 [(ngModel)]="email" name="email" />
          <button type="button" class="btn btn-outline-primary btn-lg w-100"
                  [disabled]="resending() || !email" (click)="resend()">
            @if (resending()) { <span class="spinner-border spinner-border-sm me-2"></span> }
            Resend verification email
          </button>
        </div>
      }
    </ng-template>
  `,
  styles: [`
    .verify-container {
      min-height: calc(100vh - 70px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
    }
    .verify-card {
      width: 100%;
      max-width: 440px;
      background: rgba(255, 255, 255, 0.94);
      border: 1px solid rgba(95, 231, 191, 0.18);
      border-radius: 18px;
      padding: 2.4rem 2rem;
      box-shadow: var(--shadow-soft);
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
    }
    .verify-brand {
      border-radius: 16px;
      margin-bottom: 0.75rem;
      box-shadow: 0 10px 28px rgba(31, 44, 115, 0.18);
    }
    .verify-icon { font-size: 3rem; line-height: 1; margin-bottom: 0.25rem; color: var(--primary-color); }
    .verify-icon.success { color: #18c78f; }
    .verify-icon.error { color: #ef5d6c; }
    .verify-title {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--text-primary);
      margin: 0;
    }
    .verify-text { color: var(--text-secondary); margin-bottom: 1rem; }
    .btn-primary {
      background: linear-gradient(135deg, rgba(95, 231, 191, 0.25), rgba(95, 231, 191, 0.4));
      border: none;
      color: #08493f;
      font-weight: 700;
      border-radius: 0.8rem;
    }
    .btn-outline-primary {
      border: 2px solid rgba(95, 231, 191, 0.3);
      color: var(--text-primary);
      border-radius: 0.8rem;
      font-weight: 600;
    }
    .resend-form { display: flex; flex-direction: column; }
    .form-control-lg { border-radius: 0.8rem; }
    .verify-footer { margin-top: 1rem; font-size: 0.9rem; }
    .verify-footer a { color: var(--primary-color); text-decoration: none; }
    .verify-footer a:hover { text-decoration: underline; }
  `]
})
export class VerifyEmailComponent implements OnInit {
  state = signal<VerifyState>('verifying');
  message = signal<string>('');
  email = '';
  resending = signal(false);
  resendSent = signal(false);
  resendMessage = signal<string>('');

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // Pre-fill the email if we know it (e.g. user is logged in but unverified).
    this.email = this.authService.email() ?? '';

    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.state.set('no-token');
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: (res) => {
        if (res?.verified) {
          this.state.set('success');
        } else {
          this.message.set(res?.message || 'This verification link is invalid or has expired.');
          this.state.set('error');
        }
      },
      error: (err) => {
        this.message.set(err?.error?.message || 'This verification link is invalid or has expired.');
        this.state.set('error');
      }
    });
  }

  resend(): void {
    if (!this.email) return;
    this.resending.set(true);
    this.authService.resendVerification(this.email).subscribe({
      next: (res) => {
        this.resending.set(false);
        this.resendSent.set(true);
        this.resendMessage.set(res?.message || 'If that email needs verifying, a new link is on its way.');
      },
      error: () => {
        this.resending.set(false);
        this.resendSent.set(true);
        this.resendMessage.set('If that email needs verifying, a new link is on its way.');
      }
    });
  }
}
