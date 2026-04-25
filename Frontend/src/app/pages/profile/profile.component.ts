import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, of, switchMap } from 'rxjs';
import { ProfileService } from '../../services/profile.service';
import { UserPreferences, UserProfile } from '../../models';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { LanguageService } from '../../services/language.service';
import { TtsService } from '../../services/tts.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule, TranslatePipe],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  loading = signal(true);
  saving = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  originalUsername = '';

  currentPassword = '';
  newPassword = '';
  confirmNewPassword = '';
  autoTtsEnabled = true;

  profileForm: Partial<UserProfile> = {
    username: '',
    email: '',
    fullName: '',
    phone: '',
    avatarUrl: '',
    bio: '',
    currentEmotionalState: ''
  };

  preferencesForm: UserPreferences = {
    userId: '',
    language: 'en',
    theme: 'light',
    timezone: 'UTC',
    notificationEnabled: true,
    emailNotifications: true,
    dataCollectionConsent: false
  };

  constructor(
    private profileService: ProfileService,
    private chatService: ChatService,
    public authService: AuthService,
    private languageService: LanguageService,
    private ttsService: TtsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      user: this.profileService.getCurrentUser(),
      preferences: this.profileService.getOrCreatePreferences()
    }).subscribe({
      next: ({ user, preferences }) => {
        this.profileForm = {
          username: user.username,
          email: user.email,
          fullName: user.fullName ?? '',
          phone: user.phone ?? '',
          avatarUrl: user.avatarUrl ?? '',
          bio: user.bio ?? '',
          currentEmotionalState: user.currentEmotionalState ?? ''
        };
        this.originalUsername = user.username;
        this.preferencesForm = {
          ...preferences,
          notificationEnabled: preferences.notificationEnabled ?? true,
          emailNotifications: preferences.emailNotifications ?? true,
          dataCollectionConsent: preferences.dataCollectionConsent ?? false
        };
        this.languageService.setLanguage(this.preferencesForm.language);
        this.chatService.setLanguage(this.preferencesForm.language);
        this.autoTtsEnabled = this.ttsService.isAutoSpeakEnabled();
        this.loading.set(false);
      },
      error: error => {
        console.error('Failed to load profile data', error);
        this.error.set('Unable to load your profile right now.');
        this.loading.set(false);
      }
    });
  }

  save(): void {
    const passwordValidationError = this.validatePasswordForm();
    if (passwordValidationError) {
      this.error.set(passwordValidationError);
      return;
    }

    const accountPayload: { username?: string; currentPassword?: string; newPassword?: string } = {};
    const requestedUsername = this.profileForm.username?.trim();
    const wantsPasswordChange = !!this.newPassword.trim();

    if (requestedUsername && requestedUsername !== this.originalUsername) {
      accountPayload.username = requestedUsername;
    }

    if (wantsPasswordChange) {
      accountPayload.currentPassword = this.currentPassword;
      accountPayload.newPassword = this.newPassword;
    }

    const hasAccountChanges = Object.keys(accountPayload).length > 0;

    this.saving.set(true);
    this.error.set(null);
    this.success.set(null);

    const userUpdatePayload: any = {
      fullName: this.profileForm.fullName?.trim() || null,
      phone: this.profileForm.phone?.trim() || null
    };

    const preferencesPayload: any = {
      language: this.preferencesForm.language,
      notificationEnabled: this.preferencesForm.notificationEnabled,
      emailNotifications: this.preferencesForm.emailNotifications,
      dataCollectionConsent: this.preferencesForm.dataCollectionConsent
    };

    forkJoin({
      user: this.profileService.updateCurrentUser(userUpdatePayload),
      preferences: this.profileService.updatePreferences(preferencesPayload),
      account: hasAccountChanges ? this.profileService.updateAccount(accountPayload) : of(null)
    }).pipe(
      switchMap(({ preferences, account }) => {
        this.chatService.setLanguage(preferences.language);
        this.languageService.setLanguage(preferences.language);

        if (account?.username) {
          this.profileForm.username = account.username;
          this.originalUsername = account.username;
        }

        return this.authService.refreshUserState();
      })
    ).subscribe({
      next: () => {
        this.success.set('Profile updated successfully.');
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmNewPassword = '';
        this.saving.set(false);
      },
      error: error => {
        console.error('Failed to save profile', error);
        this.error.set(error?.error?.message || 'Unable to save changes. Please try again.');
        this.saving.set(false);
      }
    });
  }

  onLanguageChanged(language: 'en' | 'ms'): void {
    this.languageService.setLanguage(language);
    this.chatService.setLanguage(language);
  }

  onAutoTtsChanged(enabled: boolean): void {
    this.ttsService.setAutoSpeakEnabled(enabled);
  }

  private validatePasswordForm(): string | null {
    const hasCurrent = !!this.currentPassword.trim();
    const hasNew = !!this.newPassword.trim();
    const hasConfirm = !!this.confirmNewPassword.trim();

    if (!hasCurrent && !hasNew && !hasConfirm) {
      return null;
    }

    if (!hasCurrent) {
      return 'Current password is required to change password.';
    }

    if (!hasNew) {
      return 'New password is required.';
    }

    if (this.newPassword.length < 6) {
      return 'New password must be at least 6 characters.';
    }

    if (this.newPassword !== this.confirmNewPassword) {
      return 'New password and confirmation do not match.';
    }

    return null;
  }

  goBack(): void {
    this.router.navigate(['/chat']);
  }
}
