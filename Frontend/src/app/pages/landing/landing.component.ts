import { Component, signal, effect, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { ChatService } from '../../services/chat.service';
import { LanguageService } from '../../services/language.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent implements AfterViewInit {
  isLoading = false;
  showLanguageDropdown = false;
  
  // Stats animation signals
  activeUsers = signal(0);
  supportiveMessages = signal(0);
  satisfaction = signal(0);

  @ViewChild('statsSection') statsSection!: ElementRef;

  constructor(
    private authService: AuthService,
    private chatService: ChatService,
    private router: Router,
    public languageService: LanguageService
  ) {
    // Don't redirect - allow all users to view landing page
    // Users will decide when to login
  }

  ngAfterViewInit(): void {
    this.setupIntersectionObserver();
  }

  setupIntersectionObserver(): void {
    if (!this.statsSection) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            this.animateStats();
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.5 }
    );

    observer.observe(this.statsSection.nativeElement);
  }

  animateStats(): void {
    this.animateCounter(0, 2000, 1500, (value) => this.activeUsers.set(value));
    this.animateCounter(0, 2500, 1500, (value) => this.supportiveMessages.set(value));
    this.animateCounter(0, 95, 1500, (value) => this.satisfaction.set(value));
  }

  private animateCounter(
    start: number,
    end: number,
    duration: number,
    callback: (value: number) => void
  ): void {
    const startTime = Date.now();
    
    const update = () => {
      const now = Date.now();
      const progress = Math.min((now - startTime) / duration, 1);
      const current = Math.floor(start + (end - start) * progress);
      callback(current);

      if (progress < 1) {
        requestAnimationFrame(update);
      }
    };

    requestAnimationFrame(update);
  }

  loginAnonymously(): void {
    this.isLoading = true;
    this.authService.loginAnonymously()
      .subscribe({
        next: () => {
          this.chatService.resetSession().subscribe({
            next: () => this.router.navigate(['/chat']),
            error: () => this.router.navigate(['/chat'])
          });
        },
        error: (error) => {
          console.error('Anonymous login failed:', error);
          this.isLoading = false;
        }
      });
  }

  toggleLanguageDropdown(): void {
    this.showLanguageDropdown = !this.showLanguageDropdown;
  }

  setLanguage(lang: 'en' | 'ms'): void {
    this.languageService.setLanguage(lang);
    this.showLanguageDropdown = false;
  }

  formatNumber(num: number): string {
    if (num >= 1000) {
      return (num / 1000).toFixed(1) + 'k';
    }
    return num + '';
  }
}
