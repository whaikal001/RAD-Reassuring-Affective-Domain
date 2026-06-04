import {
  Component, ElementRef, EventEmitter, Input, NgZone, OnInit, Output, ViewChild, signal
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';

// Google Identity Services is loaded at runtime from the gsi/client script.
declare const google: any;

/**
 * Renders the official Google Identity Services button and exchanges the resulting
 * ID token for a RadAI session via AuthService. Drop it into login/register.
 *
 * If `environment.googleClientId` is blank it shows a disabled placeholder instead of
 * breaking the page, so the UI still looks complete before Google is configured.
 */
@Component({
  selector: 'app-google-signin-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (clientId) {
      <div class="gsi-wrapper">
        <div #googleBtn class="gsi-host"></div>
        @if (loading()) {
          <div class="gsi-loading"><span class="spinner-border spinner-border-sm me-2"></span>Signing in…</div>
        }
      </div>
    } @else {
      <button type="button" class="btn btn-outline-light w-100" disabled
              title="Google sign-in isn't configured yet">
        <i class="bi bi-google me-2"></i>Google sign-in unavailable
      </button>
    }
  `,
  styles: [`
    .gsi-wrapper { display: flex; flex-direction: column; align-items: center; gap: 0.5rem; }
    .gsi-host { display: flex; justify-content: center; width: 100%; }
    .gsi-loading { font-size: 0.85rem; color: var(--text-secondary); display: flex; align-items: center; }
  `]
})
export class GoogleSignInButtonComponent implements OnInit {
  @ViewChild('googleBtn') googleBtn?: ElementRef<HTMLDivElement>;

  /** Where to go after a successful sign-in (defaults to /chat, honouring ?returnUrl). */
  @Input() returnUrl?: string;
  /** Emits a user-facing error message when sign-in fails. */
  @Output() failed = new EventEmitter<string>();

  clientId = environment.googleClientId;
  loading = signal(false);

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private router: Router,
    private route: ActivatedRoute,
    private zone: NgZone
  ) {}

  ngOnInit(): void {
    if (!this.clientId) return;
    this.loadGisScript()
      .then(() => this.renderButton())
      .catch(() => this.failed.emit('Could not load Google sign-in. Check your connection and try again.'));
  }

  private loadGisScript(): Promise<void> {
    if (typeof google !== 'undefined' && google?.accounts?.id) {
      return Promise.resolve();
    }
    const existing = document.getElementById('google-gsi-script') as HTMLScriptElement | null;
    if (existing) {
      return new Promise((resolve, reject) => {
        existing.addEventListener('load', () => resolve());
        existing.addEventListener('error', () => reject());
        if ((existing as any).dataset['loaded']) resolve();
      });
    }
    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.id = 'google-gsi-script';
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => { (script as any).dataset['loaded'] = 'true'; resolve(); };
      script.onerror = () => reject();
      document.head.appendChild(script);
    });
  }

  private renderButton(): void {
    if (typeof google === 'undefined' || !google?.accounts?.id || !this.googleBtn) return;

    google.accounts.id.initialize({
      client_id: this.clientId,
      callback: (response: { credential: string }) => this.handleCredential(response),
    });

    google.accounts.id.renderButton(this.googleBtn.nativeElement, {
      theme: 'outline',
      size: 'large',
      type: 'standard',
      text: 'continue_with',
      shape: 'pill',
      logo_alignment: 'left',
      width: 320,
    });
  }

  private handleCredential(response: { credential: string }): void {
    // GIS invokes this outside Angular's zone — re-enter so signals/navigation update.
    this.zone.run(() => {
      if (!response?.credential) {
        this.failed.emit('Google sign-in was cancelled.');
        return;
      }
      this.loading.set(true);
      this.authService.loginWithGoogle(response.credential).subscribe({
        next: () => {
          this.chatService.resetSession().subscribe({
            next: () => this.redirect(),
            error: () => this.redirect(),
          });
        },
        error: (err) => {
          this.loading.set(false);
          this.failed.emit(err?.error?.message || 'Google sign-in failed. Please try again.');
        },
      });
    });
  }

  private redirect(): void {
    this.loading.set(false);
    if (this.authService.hasRole('ADMIN')) {
      this.router.navigate(['/admin']);
      return;
    }
    const target = this.returnUrl || this.route.snapshot.queryParams['returnUrl'] || '/chat';
    this.router.navigate([target]);
  }
}
