import { Injectable, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export interface TtsRequest {
  text: string;
  emotion?: string;
  rate?: string;
  pitch?: string;
}

export interface TtsResponse {
  ssml: string;
  plainText: string;
}

@Injectable({
  providedIn: 'root'
})
export class TtsService {
  private readonly API_URL = `${environment.apiUrl}/tts`;
  private readonly AUTO_TTS_KEY = 'radai_tts_enabled';
  private synth: SpeechSynthesis | null = null;
  private readonly speakingState = signal(false);
  private readonly autoSpeakState = signal(true);

  readonly speaking = this.speakingState.asReadonly();
  readonly autoSpeakEnabled = this.autoSpeakState.asReadonly();

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {
    const stored = localStorage.getItem(this.AUTO_TTS_KEY);
    this.autoSpeakState.set(stored == null ? true : stored === 'true');

    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      this.synth = window.speechSynthesis;
    }
  }

  generateSsml(request: TtsRequest): Observable<TtsResponse> {
    const headers = new HttpHeaders({
      'Content-Type': 'application/json'
    });

    return this.http.post<TtsResponse>(`${this.API_URL}/generate`, request, { headers });
  }

  // language: 'en' | 'ms' | other codes
  speak(text: string, language: string = 'en', emotion?: string): void {
    // Clean text to avoid speaking emoji names (e.g. "smile") or reading shortcodes like :smile:
    const cleaned = this.removeEmojisAndNames(text);
    if (!cleaned || cleaned.trim().length === 0) return;
    if (!this.autoSpeakState()) {
      this.stop();
      return;
    }

    // If language is Malay (ms), prefer browser Web Speech API for local speech
    const langLower = (language || 'en').toLowerCase();
    if (langLower === 'ms' || langLower === 'malay') {
      if (!this.synth) {
        console.warn('Speech synthesis not available for browser TTS');
        this.speakingState.set(false);
        return;
      }

      // Cancel any ongoing speech
      this.synth.cancel();
      this.speakingState.set(false);

      const utterance = new SpeechSynthesisUtterance(cleaned);

      utterance.onstart = () => this.speakingState.set(true);
      utterance.onend = () => this.speakingState.set(false);
      utterance.onerror = () => this.speakingState.set(false);
      utterance.onpause = () => this.speakingState.set(false);
      utterance.onresume = () => this.speakingState.set(true);

      // Adjust voice parameters based on emotion
      switch (emotion?.toLowerCase()) {
        case 'calm':
        case 'relaxed':
          utterance.rate = 0.9;
          utterance.pitch = 0.9;
          break;
        case 'happy':
          utterance.rate = 1.1;
          utterance.pitch = 1.1;
          break;
        case 'sad':
        case 'hopeless':
          utterance.rate = 0.8;
          utterance.pitch = 0.8;
          break;
        case 'anxious':
        case 'stressed':
          utterance.rate = 1.0;
          utterance.pitch = 1.0;
          break;
        case 'angry':
        case 'frustrated':
          utterance.rate = 1.0;
          utterance.pitch = 0.9;
          break;
        default:
          utterance.rate = 1.0;
          utterance.pitch = 1.0;
      }

      // Try to find a good Malay voice first, then fallback to generic
      const voices = this.synth.getVoices();
      const preferredVoice = voices.find(v => v.lang.startsWith('ms')) || voices.find(v => v.lang.startsWith('id')) || voices.find(v => v.lang.startsWith('en'));
      if (preferredVoice) {
        utterance.voice = preferredVoice;
      }

      this.synth.speak(utterance);
      return;
    }

    // For non-Malay languages (default English), request server-side TTS
    // Build headers (include auth token if present)
    const token = this.authService.getToken();
    const headers: any = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const params = {
      text: cleaned,
      language: language
    };

    // Server returns audio/mpeg bytes — request as blob
    this.http.post(`${this.API_URL}/generate`, null, { headers, params, responseType: 'blob' as 'json' })
      .subscribe({
        next: (blob: any) => {
          try {
            const url = URL.createObjectURL(blob);
            const audio = new Audio(url);
            audio.onended = () => {
              URL.revokeObjectURL(url);
              this.speakingState.set(false);
            };
            audio.onerror = () => {
              URL.revokeObjectURL(url);
              this.speakingState.set(false);
            };
            this.speakingState.set(true);
            audio.play().catch(err => {
              console.error('Failed to play TTS audio', err);
              this.speakingState.set(false);
            });
          } catch (e) {
            console.error('Error handling TTS audio blob', e);
            this.speakingState.set(false);
          }
        },
        error: err => {
          console.error('Server TTS request failed', err);
          this.speakingState.set(false);
        }
      });

    // Server TTS handled via audio blob playback above. No browser utterance for server route.
  }

  /**
   * Remove emoji characters and colon-style emoji names from a string.
   */
  private removeEmojisAndNames(input: string): string {
    if (!input) return input;
    let s = input;

    // Remove colon-wrapped emoji names like :smile:
    s = s.replace(/:[a-zA-Z0-9_+\-]+:/g, '');

    // Remove surrogate pair emojis and common ranges
    s = s.replace(/([\uD800-\uDBFF][\uDC00-\uDFFF])/g, '');
    s = s.replace(/[\u2600-\u26FF]/g, '');
    s = s.replace(/[\u2700-\u27BF]/g, '');
    s = s.replace(/[\uFE0F\u200D]/g, '');

    // Collapse whitespace
    return s.replace(/\s+/g, ' ').trim();
  }

  stop(): void {
    if (this.synth) {
      this.synth.cancel();
    }

    this.speakingState.set(false);
  }

  isSpeaking(): boolean {
    return this.speaking();
  }

  setAutoSpeakEnabled(enabled: boolean): void {
    this.autoSpeakState.set(enabled);
    localStorage.setItem(this.AUTO_TTS_KEY, String(enabled));

    if (!enabled) {
      this.stop();
    }
  }

  isAutoSpeakEnabled(): boolean {
    return this.autoSpeakState();
  }
}
