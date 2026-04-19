import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { UserAccountUpdateRequest, UserPreferences, UserProfile } from '../models';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private readonly USERS_API_URL = `${environment.apiUrl}/users`;
  private readonly PREFERENCES_API_URL = `${environment.apiUrl}/user/preferences`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getCurrentUser(): Observable<UserProfile> {
    const userId = this.requireUserId();
    return this.http.get<UserProfile>(`${this.USERS_API_URL}/${userId}`);
  }

  updateCurrentUser(changes: Partial<UserProfile>): Observable<UserProfile> {
    const userId = this.requireUserId();

    return this.getCurrentUser().pipe(
      switchMap(user => this.http.put<UserProfile>(`${this.USERS_API_URL}/${userId}`, {
        ...user,
        ...changes,
        id: user.id,
        username: user.username,
        email: user.email
      }))
    );
  }

  updateAccount(changes: UserAccountUpdateRequest): Observable<UserProfile> {
    const userId = this.requireUserId();
    return this.http.put<UserProfile>(`${this.USERS_API_URL}/${userId}/account`, changes);
  }

  getPreferences(): Observable<UserPreferences> {
    const userId = this.requireUserId();
    return this.http.get<UserPreferences>(`${this.PREFERENCES_API_URL}/${userId}`);
  }

  getOrCreatePreferences(): Observable<UserPreferences> {
    return this.getPreferences().pipe(
      catchError(error => {
        if (error.status === 400 || error.status === 404 || error.status === 500) {
          return this.createPreferences(this.buildDefaultPreferences());
        }

        return throwError(() => error);
      })
    );
  }

  updatePreferences(changes: Partial<UserPreferences>): Observable<UserPreferences> {
    const userId = this.requireUserId();

    return this.getOrCreatePreferences().pipe(
      switchMap(preferences => this.http.put<UserPreferences>(`${this.PREFERENCES_API_URL}/${userId}`, {
        ...preferences,
        ...changes,
        userId,
        notificationEnabled: changes.notificationEnabled ?? preferences.notificationEnabled,
        emailNotifications: changes.emailNotifications ?? preferences.emailNotifications,
        dataCollectionConsent: changes.dataCollectionConsent ?? preferences.dataCollectionConsent
      }))
    );
  }

  private createPreferences(preferences: UserPreferences): Observable<UserPreferences> {
    return this.http.post<UserPreferences>(this.PREFERENCES_API_URL, preferences).pipe(
      map(created => ({
        ...created,
        notificationEnabled: created.notificationEnabled ?? true,
        emailNotifications: created.emailNotifications ?? true,
        dataCollectionConsent: created.dataCollectionConsent ?? false
      }))
    );
  }

  private buildDefaultPreferences(): UserPreferences {
    return {
      userId: this.requireUserId(),
      language: 'en',
      theme: 'light',
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC',
      notificationEnabled: true,
      emailNotifications: true,
      dataCollectionConsent: false
    };
  }

  private requireUserId(): string {
    const userId = this.authService.getUserId();

    if (!userId) {
      throw new Error('User is not authenticated.');
    }

    return userId;
  }
}
