import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ReportSummary } from '../models';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class ReportsService {
  private readonly API_URL = `${environment.apiUrl}/reports`;

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  getReports(): Observable<ReportSummary[]> {
    const userId = this.requireUserId();
    return this.http.get<ReportSummary[]>(`${this.API_URL}/user/${userId}`);
  }

  getReport(reportId: string): Observable<ReportSummary> {
    return this.http.get<ReportSummary>(`${this.API_URL}/${reportId}`);
  }

  generateReport(reportType: string, startDate: string, endDate: string): Observable<ReportSummary> {
    const userId = this.requireUserId();

    return this.http.post<ReportSummary>(`${this.API_URL}/generate/${userId}`, {
      reportType,
      startDate: `${startDate}T00:00:00`,
      endDate: `${endDate}T23:59:59`
    });
  }

  private requireUserId(): string {
    const userId = this.authService.getUserId();

    if (!userId) {
      throw new Error('User is not authenticated.');
    }

    return userId;
  }
}
