import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  ChatContext,
  ChatRequest,
  ChatResponse,
  DashboardData,
  EmotionalJourneyPoint,
  Message,
  PaginatedSessions,
  SessionDetails
} from '../models';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private readonly API_URL = `${environment.apiUrl}/chat/flow`;
  private readonly LANGUAGE_KEY = 'socializerai_language';
  
  private _messages = signal<Message[]>([]);
  private _isLoading = signal<boolean>(false);
  private _conversationId = signal<string | null>(null);
  private _currentEmotion = signal<string | null>(null);
  private _currentIntensity = signal<number>(5);
  private _language = signal<string>('en');
  private _lastResponse = signal<ChatResponse | null>(null);

  messages = this._messages.asReadonly();
  isLoading = this._isLoading.asReadonly();
  conversationId = this._conversationId.asReadonly();
  currentEmotion = this._currentEmotion.asReadonly();
  currentIntensity = this._currentIntensity.asReadonly();
  language = this._language.asReadonly();
  lastResponse = this._lastResponse.asReadonly();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    const storedLanguage = localStorage.getItem(this.LANGUAGE_KEY);
    if (storedLanguage === 'en' || storedLanguage === 'ms') {
      this._language.set(storedLanguage);
    }
  }

  sendMessage(userMessage: string, intensity: number, useAI: boolean = true, appendUserMessage: boolean = true): Observable<ChatResponse> {
    this._isLoading.set(true);
    
    const request: ChatRequest = {
      userMessage,
      intensityScore: intensity,
      language: this._language()
    };

    const headers = this.getHeaders();
    const endpoint = useAI ? 'process-with-ai' : 'process';

    if (appendUserMessage) {
      this.addMessage({
        id: this.generateId(),
        content: userMessage,
        isUser: true,
        timestamp: new Date()
      });
    }

    return this.http.post<ChatResponse>(`${this.API_URL}/${endpoint}`, request, { headers })
      .pipe(
        tap(response => {
          this._isLoading.set(false);
          this._conversationId.set(response.conversationId);
          this._currentEmotion.set(response.emotion);
          this._currentIntensity.set(response.intensity);
          this._lastResponse.set(response);

          // Add bot response
          this.addMessage({
            id: this.generateId(),
            content: this.buildMessageContent(response),
            isUser: false,
            timestamp: new Date(),
            emotion: response.emotion,
            intensity: response.intensity,
            tips: this.extractTips(response.strategies)
          });
        }),
        catchError(error => {
          this._isLoading.set(false);
          console.error('Chat error:', error);
          
          // Add error message
          this.addMessage({
            id: this.generateId(),
            content: 'Sorry, I encountered an error. Please try again.',
            isUser: false,
            timestamp: new Date()
          });

          throw error;
        })
      );
  }

  endSession(): Observable<void> {
    const headers = this.getHeaders();
    const conversationId = this._conversationId();
    
    if (!conversationId) {
      return of(undefined);
    }

    const headersWithConv = headers.set('X-Conversation-ID', conversationId);

    return this.http.post<void>(`${this.API_URL}/end-session`, {}, { headers: headersWithConv })
      .pipe(
        tap(() => {
          this.clearChat();
        })
      );
  }

  resetSession(): Observable<void> {
    const headers = this.getHeaders();

    return this.http.post<void>(`${this.API_URL}/reset-session`, {}, { headers })
      .pipe(
        tap(() => this.clearChat()),
        catchError(error => {
          console.warn('Failed to reset backend session. Clearing local chat state only.', error);
          this.clearChat();
          return of(undefined);
        })
      );
  }

  getEmotionTrends(): Observable<any> {
    const headers = this.getHeaders();
    return this.http.get(`${this.API_URL}/emotion-trends`, { headers });
  }

  getDashboard(): Observable<DashboardData> {
    const headers = this.getHeaders();
    return this.http.get<DashboardData>(`${this.API_URL}/dashboard`, { headers });
  }

  getEmotionHistory(): Observable<any[]> {
    const headers = this.getHeaders();
    return this.http.get<any[]>(`${this.API_URL}/emotion-history`, { headers });
  }

  getSessions(page: number = 0, size: number = 10): Observable<PaginatedSessions> {
    const headers = this.getHeaders();
    return this.http.get<PaginatedSessions>(`${this.API_URL}/sessions?page=${page}&size=${size}`, { headers });
  }

  getSessionDetails(sessionId: string): Observable<SessionDetails> {
    return this.http.get<SessionDetails>(`${this.API_URL}/session/${sessionId}`, {
      headers: this.getHeaders()
    });
  }

  getEmotionalJourney(): Observable<EmotionalJourneyPoint[]> {
    return this.http.get<EmotionalJourneyPoint[]>(`${this.API_URL}/emotional-journey`, {
      headers: this.getHeaders()
    });
  }

  getContext(): Observable<ChatContext | null> {
    return this.http.get<ChatContext>(`${this.API_URL}/context`, {
      headers: this.getHeaders()
    }).pipe(
      tap(context => {
        this._conversationId.set(context?.conversationId ?? null);
        this._currentEmotion.set(context?.currentEmotion ?? null);
        this._currentIntensity.set(context?.intensityScore ?? 5);
      }),
      catchError(() => of(null))
    );
  }

  getGoals(): Observable<{ goals: string[] }> {
    return this.http.get<{ goals: string[] }>(`${this.API_URL}/goals`, {
      headers: this.getHeaders()
    });
  }

  addGoal(goal: string): Observable<{ goals: string[] }> {
    return this.http.post<{ goals: string[] }>(`${this.API_URL}/goals`, { goal }, {
      headers: this.getHeaders()
    });
  }

  clearGoals(): Observable<{ goals: string[] }> {
    return this.http.delete<{ goals: string[] }>(`${this.API_URL}/goals`, {
      headers: this.getHeaders()
    });
  }

  setLanguage(lang: string): void {
    this._language.set(lang);
    localStorage.setItem(this.LANGUAGE_KEY, lang);
  }

  clearChat(): void {
    this._messages.set([]);
    this._conversationId.set(null);
    this._currentEmotion.set(null);
    this._currentIntensity.set(5);
    this._lastResponse.set(null);
  }

  addLocalBotMessage(content: string, emotion = 'neutral', intensity = 4): void {
    this.addMessage({
      id: this.generateId(),
      content,
      isUser: false,
      timestamp: new Date(),
      emotion,
      intensity
    });
  }

  addLocalUserMessage(content: string): void {
    this.addMessage({
      id: this.generateId(),
      content,
      isUser: true,
      timestamp: new Date(),
      emotion: 'neutral',
      intensity: 0
    });
  }

  private addMessage(message: Message): void {
    this._messages.update(messages => [...messages, message]);
  }

  private getHeaders(): HttpHeaders {
    const userId = this.authService.getUserId() || '';
    const conversationId = this._conversationId() || '';
    
    let headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'X-User-ID': userId
    });

    if (conversationId) {
      headers = headers.set('X-Conversation-ID', conversationId);
    }

    // Include client-side screening flags for anonymous users so backend interceptor can enforce gating
    try {
      const screeningCompleted = localStorage.getItem('screening_completed');
      const screeningAction = localStorage.getItem('screening_action');
      if (screeningCompleted) {
        headers = headers.set('X-Screening-Completed', screeningCompleted);
      }
      if (screeningAction) {
        headers = headers.set('X-Screening-Action', screeningAction);
      }
    } catch (e) {
      // ignore storage errors
    }

    return headers;
  }

  private generateId(): string {
    return Math.random().toString(36).substring(2, 15);
  }

  private buildMessageContent(response: ChatResponse): string {
    const parts = [response.greeting, response.mainContent, response.followUp]
      .map(part => part?.trim())
      .filter((part): part is string => !!part);

    if (parts.length > 0) {
      return parts.join('\n\n');
    }

    return response.fullResponse || 'I am here with you.';
  }

  private extractTips(strategies?: string): string[] | undefined {
    if (!strategies?.trim()) {
      return undefined;
    }

    const tips = strategies
      .replace(/\*\*/g, '')
      .split('\n')
      .map(line => line.trim())
      .filter(line => !!line)
      .filter(line => !line.endsWith(':'))
      .map(line => line.replace(/^[•\-]\s*/, '').trim())
      .filter(line => !!line);

    return tips.length > 0 ? tips : undefined;
  }
}
