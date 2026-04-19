import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface TemplateOption { label: string; value: number; }
export interface TemplateItem {
  id: string;
  anonymous: boolean;
  intro: string;
  prompt: string;
  options: TemplateOption[];
  followUp: { [key: number]: string };
}

@Injectable({ providedIn: 'root' })
export class TemplatesService {
  private readonly ASSET_URL = '/assets/templates/dass_templates.json';
  private readonly TRACK_URL = `${environment.apiUrl}/templates/track`;

  constructor(private http: HttpClient) {}

  fetchTemplates(): Observable<TemplateItem[]> {
    return this.http.get<TemplateItem[]>(this.ASSET_URL);
  }

  trackResponse(scenarioId: string, value: number, messageText?: string) {
    const payload = { scenarioId, value, messageText };
    return this.http.post(this.TRACK_URL, payload).subscribe(() => {}, () => {});
  }
}
