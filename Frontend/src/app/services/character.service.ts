import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, forkJoin } from 'rxjs';
import { map, tap } from 'rxjs/operators';

/**
 * CharacterService integrates chat responses with r3f character animations.
 * Uses existing Deepseek chat + TTS to enrich responses with:
 * - Facial expressions (smile, sad, angry, surprised, etc.)
 * - Animations (Talking_0/1/2, Laughing, Crying, Idle, etc.)
 * - Audio from your existing TTS API
 */
@Injectable({
  providedIn: 'root'
})
export class CharacterService {
  private apiUrl = '/api/character';
  private ttsUrl = '/api/tts';
  
  // Emit character responses for r3f character to display
  characterResponse$ = new BehaviorSubject<any>(null);

  constructor(private http: HttpClient) {}

  /**
   * Enrich a chat flow response with character metadata
   * @param flowResponse Response from /api/chat/flow/process
   * @param language Language for TTS (en, ms, etc.)
   * @returns Observable with character animation metadata added
   */
  enrichResponseWithCharacter(flowResponse: any, language: string = 'en'): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/enrich?language=${language}`,
      flowResponse
    );
  }

  /**
   * Get full character response: enriched chat + audio
   * Combines your existing chat flow with character metadata and TTS
   * @param flowResponse Response from /api/chat/flow/process
   * @param language Language for TTS
   * @returns Observable with character metadata and base64 audio
   */
  getFullCharacterResponse(flowResponse: any, language: string = 'en'): Observable<any> {
    // Call character enrichment endpoint
    const enriched$ = this.enrichResponseWithCharacter(flowResponse, language);

    // For each enriched response, get audio via TTS
    return enriched$.pipe(
      map(enriched => {
        // If you need audio, call TTS separately in your component:
        // this.requestAudio(enriched.mainContent, language);
        return enriched;
      }),
      tap(enriched => {
        // Emit to r3f character panel
        this.characterResponse$.next(enriched);
      })
    );
  }

  /**
   * Request TTS audio (separate call, allows parallel requests)
   * @param text Text to convert to speech
   * @param language Language code
   * @returns Observable with audio blob/base64
   */
  requestAudio(text: string, language: string = 'en'): Observable<Blob> {
    return this.http.post(
      `${this.ttsUrl}/generate?text=${encodeURIComponent(text)}&language=${language}`,
      {},
      { responseType: 'blob' }
    );
  }

  /**
   * Get available character expressions and animations
   */
  getCharacterHealth(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/health`);
  }

  /**
   * Convert Blob audio to base64 (for playback)
   */
  blobToBase64(blob: Blob): Promise<string> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onloadend = () => resolve(reader.result as string);
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }
}
