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
  private readonly AUTO_TTS_KEY = 'socializerai_tts_enabled';
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

  speak(text: string, emotion?: string): void {
    if (!this.autoSpeakState()) {
      this.stop();
      return;
    }

    if (!this.synth) {
      console.warn('Speech synthesis not available');
      this.speakingState.set(false);
      return;
    }

    // Cancel any ongoing speech
    this.synth.cancel();
    this.speakingState.set(false);

    const utterance = new SpeechSynthesisUtterance(text);

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

    // Try to find a good voice
    const voices = this.synth.getVoices();
    const preferredVoice = voices.find(v => 
      v.lang.startsWith('en') && v.name.includes('Natural')
    ) || voices.find(v => 
      v.lang.startsWith('en')
    );
    
    if (preferredVoice) {
      utterance.voice = preferredVoice;
    }

    this.synth.speak(utterance);
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
