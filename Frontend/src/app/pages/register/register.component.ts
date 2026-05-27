import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { TranslatePipe } from '../../pipes/t.pipe';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  username = '';
  email = '';
  password = '';
  confirmPassword = '';
  loading = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private router: Router,
    public languageService: LanguageService
  ) {
    // Don't redirect - allow users to stay on register/landing pages
  }

  onSubmit(): void {
    if (!this.username || !this.email || !this.password) return;
    if (this.password !== this.confirmPassword) return;

    this.loading.set(true);
    this.error.set(null);

    this.authService.register({ 
      username: this.username, 
      email: this.email, 
      password: this.password 
    }).subscribe({
      next: () => {
        this.success.set('Account created successfully! Redirecting...');
        this.chatService.resetSession().subscribe({
          next: () => {
            setTimeout(() => {
              this.redirectBasedOnRole();
            }, 1500);
          },
          error: () => {
            setTimeout(() => {
              this.redirectBasedOnRole();
            }, 1500);
          }
        });
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.error?.message || 'Registration failed. Please try again.');
      }
    });
  }

  private redirectBasedOnRole(): void {
    // Determine destination based on user role
    if (this.authService.hasRole('admin')) {
      this.router.navigate(['/admin']);
    } else {
      this.router.navigate(['/chat']);
    }
  }
}
