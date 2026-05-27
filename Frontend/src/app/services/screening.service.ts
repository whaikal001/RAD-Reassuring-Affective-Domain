import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ScreeningResponse {
  action: 'allow' | 'prevention' | 'intervention' | string;
  score: number;
  band: string;
  message?: string;
  resources?: string[];
  emotion?: string;      // NEW: Detected emotion
  intensity?: number;    // NEW: Emotion intensity (1-10)
}

@Injectable({ providedIn: 'root' })
export class ScreeningService {
  private readonly SCREENING_URL = `${environment.apiUrl}/screening`;

  constructor(private http: HttpClient) {}

  submitScreening(consent: boolean, language: string, dass21_answers: number[]): Observable<ScreeningResponse> {
    const payload = {
      consent,
      language,
      dass21_answers
    };

    return this.http.post<ScreeningResponse>(this.SCREENING_URL, payload);
  }
}
