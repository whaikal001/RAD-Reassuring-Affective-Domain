import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PbotPortInfo {
  name: string;
  descriptiveName: string;
  portDescription: string;
}

export interface PbotStatus {
  connected: boolean;
  port: string;
  baudRate: number;
}

@Injectable({
  providedIn: 'root'
})
export class PbotService {
  private readonly API_URL = `${environment.apiUrl}/pbot`;

  constructor(private http: HttpClient) {}

  listPorts(): Observable<PbotPortInfo[]> {
    return this.http.get<PbotPortInfo[]>(`${this.API_URL}/ports`);
  }

  status(): Observable<PbotStatus> {
    return this.http.get<PbotStatus>(`${this.API_URL}/status`);
  }

  connect(port: string, baudRate = 115200): Observable<PbotStatus> {
    return this.http.post<PbotStatus>(`${this.API_URL}/connect`, { port, baudRate });
  }

  /** Connect using the backend's configured default port (COM4). */
  connectDefault(): Observable<PbotStatus> {
    return this.http.post<PbotStatus>(`${this.API_URL}/connect`, {});
  }

  disconnect(): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${this.API_URL}/disconnect`, {});
  }

  sendEmotion(emotion: string, intensity = 5): Observable<Record<string, unknown>> {
    return this.http.post<Record<string, unknown>>(`${this.API_URL}/emotion`, { emotion, intensity });
  }
}
