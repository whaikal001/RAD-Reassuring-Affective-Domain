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
          avatarUrl: user.avatarUrl || this.authService.avatarUrl() || '',
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

    const account$ = hasAccountChanges ? this.profileService.updateAccount(accountPayload) : of(null);

    // Apply the account change (username / password) FIRST, then the profile + preferences.
    // updateCurrentUser does a read-modify-write of the whole user record, so if it ran in
    // parallel with the username change it would reload and re-save the OLD username and
    // revert it. Sequencing makes its read see the new username.
    account$.pipe(
      switchMap(account => {
        if (account?.username) {
          this.profileForm.username = account.username;
          this.originalUsername = account.username;
        }
        return forkJoin({
          user: this.profileService.updateCurrentUser(userUpdatePayload),
          preferences: this.profileService.updatePreferences(preferencesPayload)
        });
      }),
      switchMap(({ preferences }) => {
        this.chatService.setLanguage(preferences.language);
        this.languageService.setLanguage(preferences.language);
        return this.authService.refreshUserState();
      })
    ).subscribe({
      next: () => {
        // The profile photo is stored client-side: a base64 data URL is far larger than the
        // user record's avatar_url column, so we persist it locally rather than failing the save.
        this.authService.setAvatar(this.profileForm.avatarUrl || null);
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

  /** Read the chosen image, crop-to-square and downscale to 256px so it stays
      small enough to persist as a data URL on the existing user record. */
  onAvatarSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.error.set('Please choose an image file (JPG, PNG, etc.).');
      input.value = '';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      const img = new Image();
      img.onload = () => {
        const size = 256;
        const canvas = document.createElement('canvas');
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          return;
        }
        // cover-crop the image into the square canvas
        const scale = Math.max(size / img.width, size / img.height);
        const w = img.width * scale;
        const h = img.height * scale;
        ctx.drawImage(img, (size - w) / 2, (size - h) / 2, w, h);
        this.profileForm.avatarUrl = canvas.toDataURL('image/jpeg', 0.82);
        this.error.set(null);
      };
      img.src = reader.result as string;
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  removeAvatar(): void {
    this.profileForm.avatarUrl = '';
  }

  getInitials(): string {
    const name = (this.profileForm.fullName || this.profileForm.username || '').trim();
    if (!name) {
      return 'U';
    }
    const parts = name.split(/\s+/);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'U';
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
