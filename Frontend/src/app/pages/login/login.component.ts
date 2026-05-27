import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);
  
  private returnUrl = '/chat';

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    // Get return URL from query params, or use default based on role
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/chat';
  }

  onSubmit(): void {
    if (!this.email || !this.password) return;

    this.loading.set(true);
    this.error.set(null);

    this.authService.login({ email: this.email, password: this.password })
      .subscribe({
        next: () => {
          this.chatService.resetSession().subscribe({
            next: () => this.redirectBasedOnRole(),
            error: () => this.redirectBasedOnRole()
          });
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set(err.error?.message || 'Invalid email or password');
        }
      });
  }

  loginAnonymously(): void {
    this.loading.set(true);
    this.error.set(null);

    this.authService.loginAnonymously()
      .subscribe({
        next: () => {
          this.router.navigate(['/chat']);
        },
        error: (err) => {
          this.loading.set(false);
          this.error.set('Unable to start anonymous session. Please try again.');
        }
      });
  }

  private redirectBasedOnRole(): void {
    // Check if return URL was explicitly provided
    if (this.route.snapshot.queryParams['returnUrl']) {
      this.router.navigate([this.returnUrl]);
      return;
    }

    // Otherwise, determine destination based on user role
    if (this.authService.hasRole('admin')) {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/chat']);
    }
  }
}
