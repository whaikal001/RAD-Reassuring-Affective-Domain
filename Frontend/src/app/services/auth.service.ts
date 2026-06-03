import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, map, of, switchMap, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthMode, JwtResponse, LoginRequest, RegisterRequest, UserPreferences, UserProfile } from '../models';
import { LanguageService } from './language.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = `${environment.apiUrl}/auth`;
  private readonly USERS_API_URL = `${environment.apiUrl}/users`;
  private readonly PREFERENCES_API_URL = `${environment.apiUrl}/user/preferences`;
  private readonly TOKEN_KEY = 'radai_token';
  private readonly USER_ID_KEY = 'radai_user_id';
  private readonly AUTH_MODE_KEY = 'radai_auth_mode';
  private readonly DISPLAY_NAME_KEY = 'radai_display_name';
  private readonly AVATAR_KEY = 'radai_avatar_url';

  private _isAuthenticated = signal<boolean>(false);
  private _userId = signal<string | null>(null);
  private _authMode = signal<AuthMode | null>(null);
  private _displayName = signal<string | null>(null);
  private _avatarUrl = signal<string | null>(null);

  isAuthenticated = computed(() => this._isAuthenticated());
  userId = computed(() => this._userId());
  isAnonymous = computed(() => this._authMode() === 'anonymous');
  isRegistered = computed(() => this._authMode() === 'registered');
  displayName = this._displayName.asReadonly();
  avatarUrl = this._avatarUrl.asReadonly();

  constructor(
    private http: HttpClient,
    private router: Router,
    private languageService: LanguageService
  ) {
    this.clearInvalidData();
    this._isAuthenticated.set(this.hasValidToken());
    this._userId.set(this.getStoredUserId());
    this._authMode.set(this.getStoredAuthMode());
    this._displayName.set(this.getStoredDisplayName());
    this._avatarUrl.set(localStorage.getItem(this.AVATAR_KEY));

    if (this._isAuthenticated() && this._userId()) {
      this.syncUserState().subscribe();
    }
  }

  private clearInvalidData(): void {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const userId = localStorage.getItem(this.USER_ID_KEY);

    if (!token || !userId || !this.isUuid(userId) || !this.hasValidToken()) {
      this.clearStoredAuth();
    }
  }

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.API_URL}/login`, credentials)
      .pipe(
        tap(response => this.handleAuthSuccess(response, 'registered')),
        switchMap(response => this.syncUserState().pipe(map(() => response))),
        catchError(error => {
          console.error('Login failed:', error);
          throw error;
        })
      );
  }

  register(data: RegisterRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.API_URL}/register`, data)
      .pipe(
        tap(response => this.handleAuthSuccess(response, 'registered')),
        catchError(error => {
          console.error('Registration failed:', error);
          throw error;
        })
      );
  }

  loginAnonymously(): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.API_URL}/anonymous`, {})
      .pipe(
        tap(response => this.handleAuthSuccess(response, 'anonymous')),
        catchError(error => {
          console.error('Anonymous login failed:', error);
          throw error;
        })
      );
  }

  logout(): void {
    this.clearStoredAuth();
    this._isAuthenticated.set(false);
    this._userId.set(null);
    this._authMode.set(null);
    this._displayName.set(null);
    this._avatarUrl.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getUserId(): string | null {
    return localStorage.getItem(this.USER_ID_KEY);
  }

  getAuthMode(): AuthMode | null {
    return this._authMode();
  }

  getDisplayName(): string | null {
    return this._displayName();
  }

  hasRole(role: string): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const roles = payload.roles || payload.role || null;
      if (!roles) return false;
      const arr = typeof roles === 'string' ? roles.split(',').map((r: string) => r.trim()) : roles;
      return arr.includes(role) || arr.includes('ROLE_' + role);
    } catch {
      return false;
    }
  }

  refreshUserState(): Observable<void> {
    return this.syncUserState();
  }

  /** Persist the user's profile photo client-side (data URLs are too large for the
      user record's avatar column) and update the reactive avatar used across the UI. */
  setAvatar(url: string | null): void {
    const value = url?.trim() || null;
    this._avatarUrl.set(value);
    try {
      if (value) {
        localStorage.setItem(this.AVATAR_KEY, value);
      } else {
        localStorage.removeItem(this.AVATAR_KEY);
      }
    } catch (e) { /* storage unavailable / quota */ }
  }

  private handleAuthSuccess(response: JwtResponse, authMode: AuthMode): void {
    // Use English as the immediate fallback until user preferences are loaded.
    this.languageService.setLanguage('en');

    localStorage.setItem(this.TOKEN_KEY, response.token);
    // Store canonical UUID userId when provided; fall back to username if missing
    const uid = (response as any).userId || response.username;
    localStorage.setItem(this.USER_ID_KEY, uid);
    localStorage.setItem(this.AUTH_MODE_KEY, authMode);
    this._isAuthenticated.set(true);
    this._userId.set(uid);
    this._authMode.set(authMode);

    if (authMode === 'anonymous') {
      localStorage.removeItem(this.DISPLAY_NAME_KEY);
      this._displayName.set(null);
    }
  }

  private hasValidToken(): boolean {
    const token = this.getToken();
    if (!token) return false;
    
    // Basic JWT expiry check
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000;
      return Date.now() < expiry;
    } catch {
      return false;
    }
  }

  private getStoredUserId(): string | null {
    return localStorage.getItem(this.USER_ID_KEY);
  }

  private getStoredAuthMode(): AuthMode | null {
    const authMode = localStorage.getItem(this.AUTH_MODE_KEY);
    return authMode === 'anonymous' || authMode === 'registered' ? authMode : null;
  }

  private getStoredDisplayName(): string | null {
    const value = localStorage.getItem(this.DISPLAY_NAME_KEY);
    return value && value.trim() ? value.trim() : null;
  }

  private clearStoredAuth(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_ID_KEY);
    localStorage.removeItem(this.AUTH_MODE_KEY);
    localStorage.removeItem(this.DISPLAY_NAME_KEY);
    localStorage.removeItem(this.AVATAR_KEY);
  }

  private syncUserState(): Observable<void> {
    const userId = this.getStoredUserId();

    if (!userId) {
      return of(undefined);
    }

    return this.http.get<UserProfile>(`${this.USERS_API_URL}/${userId}`).pipe(
      switchMap(user => {
        const authMode: AuthMode = user.isAnonymous ? 'anonymous' : 'registered';
        localStorage.setItem(this.AUTH_MODE_KEY, authMode);
        this._authMode.set(authMode);

        const resolvedDisplayName = user.firstName?.trim()
          || user.fullName?.trim()
          || user.username?.trim()
          || null;

        if (resolvedDisplayName) {
          localStorage.setItem(this.DISPLAY_NAME_KEY, resolvedDisplayName);
          this._displayName.set(resolvedDisplayName);
        }

        // Adopt the backend avatar only if it has one; otherwise keep the locally
        // stored photo (avatars are persisted client-side, not in the user record).
        const backendAvatar = user.avatarUrl?.trim();
        if (backendAvatar) {
          this._avatarUrl.set(backendAvatar);
          try { localStorage.setItem(this.AVATAR_KEY, backendAvatar); } catch { /* quota */ }
        }

        if (authMode === 'anonymous') {
          return of(undefined);
        }

        return this.http.get<UserPreferences>(`${this.PREFERENCES_API_URL}/${userId}`).pipe(
          tap(preferences => {
            const language = preferences?.language === 'ms' ? 'ms' : 'en';
            this.languageService.setLanguage(language);
          }),
          map(() => undefined),
          catchError(() => of(undefined))
        );
      }),
      catchError(error => {
        console.warn('Unable to sync auth state from profile:', error);
        return of(undefined);
      })
    );
  }

  private isUuid(value: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
  }
}
