import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { ReportsService } from '../../services/reports.service';
import { ChatService } from '../../services/chat.service';
import { ReportSummary, EmotionalJourneyPoint, SessionSummary } from '../../models';
import { TranslatePipe } from '../../pipes/t.pipe';

interface ReportTrendPoint {
  timestamp: string;
  intensity: number;
  sentiment?: number;
  emotion?: string;
}

interface ParsedReportSummary {
  entries?: number;
  totalSessions?: number;
  averageIntensity?: number;
  averageMoodScore?: number;
  dominantEmotion?: string;
  emotionDistribution?: Record<string, number>;
  intensityTrend?: ReportTrendPoint[];
  timeRange?: string;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.scss'
})
export class ReportsComponent implements OnInit {
  loading = signal(true);
  generating = signal(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  reports = signal<ReportSummary[]>([]);
  selectedReport = signal<ReportSummary | null>(null);
  
  // New signals for wellbeing activities and graph logs
  wellbeingActivities = signal<SessionSummary[]>([]);
  graphLogs = signal<EmotionalJourneyPoint[]>([]);
  loadingDetails = signal(false);

  reportType = 'weekly';
  startDate = this.getDefaultStartDate();
  endDate = this.getToday();

  constructor(
    private reportsService: ReportsService,
    private chatService: ChatService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading.set(true);
    this.error.set(null);

    this.reportsService.getReports().subscribe({
      next: reports => {
        // Filter out reports with 0 sessions
        const filteredReports = reports.filter(r => (r.totalSessions || 0) > 0);
        this.reports.set(filteredReports);
        this.loading.set(false);
      },
      error: error => {
        console.error('Failed to load reports', error);
        this.error.set('Unable to load reports right now.');
        this.loading.set(false);
      }
    });
  }

  generate(): void {
    if (!this.startDate || !this.endDate) {
      return;
    }

    this.generating.set(true);
    this.error.set(null);
    this.success.set(null);

    this.reportsService.generateReport(this.reportType, this.startDate, this.endDate).subscribe({
      next: report => {
        this.success.set('Report generated successfully.');
        this.reports.update(reports => [report, ...reports.filter(existing => existing.id !== report.id)]);
        this.selectedReport.set(report);
        this.generating.set(false);
      },
      error: error => {
        console.error('Failed to generate report', error);
        this.error.set('Unable to generate the report. Check your date range and try again.');
        this.generating.set(false);
      }
    });
  }

  selectReport(reportId: string): void {
    this.reportsService.getReport(reportId).subscribe({
      next: report => {
        this.selectedReport.set(report);
        this.loadReportDetails(report);
      },
      error: error => {
        console.error('Failed to load report detail', error);
        this.error.set('Unable to load the selected report.');
      }
    });
  }

  /**
   * Load wellbeing activities and graph logs for the selected report
   */
  private loadReportDetails(report: ReportSummary): void {
    this.loadingDetails.set(true);
    
    forkJoin({
      sessions: this.chatService.getSessions(0, 100).pipe(
        catchError(() => of({ sessions: [], currentPage: 0, totalPages: 0, totalItems: 0, pageSize: 0 }))
      ),
      journey: this.chatService.getEmotionalJourney().pipe(
        catchError(() => of([] as EmotionalJourneyPoint[]))
      )
    }).subscribe({
      next: ({ sessions, journey }) => {
        // Filter sessions within report date range
        const reportStart = new Date(report.startDate).getTime();
        const reportEnd = new Date(report.endDate).getTime();
        
        const filteredActivities = sessions.sessions.filter(session => {
          const sessionDate = new Date(session.startedAt || 0).getTime();
          return sessionDate >= reportStart && sessionDate <= reportEnd;
        });
        
        // Filter journey points within report date range
        const filteredJourney = journey.filter(point => {
          const pointDate = new Date(point.timestamp).getTime();
          return pointDate >= reportStart && pointDate <= reportEnd;
        });
        
        this.wellbeingActivities.set(filteredActivities);
        this.graphLogs.set(filteredJourney);
        this.loadingDetails.set(false);
      },
      error: error => {
        console.error('Failed to load report details', error);
        this.loadingDetails.set(false);
      }
    });
  }

  /**
   * Navigate to resume a past chat session
   */
  resumeSession(sessionId: string): void {
    this.router.navigate(['/chat', { sessionId }]);
  }

  formatScore(value?: number): string {
    return typeof value === 'number' ? value.toFixed(2) : '0.00';
  }

  getParsedSummary(): ParsedReportSummary | null {
    const report = this.selectedReport();

    if (!report?.summary) {
      return null;
    }

    try {
      return JSON.parse(report.summary) as ParsedReportSummary;
    } catch {
      return null;
    }
  }

  getEmotionBars(): Array<{ emotion: string; count: number; width: number; color: string }> {
    const summary = this.getParsedSummary();
    const distribution = summary?.emotionDistribution;

    if (!distribution || !Object.keys(distribution).length) {
      return [];
    }

    const entries = Object.entries(distribution)
      .map(([emotion, count]) => ({ emotion, count: Number(count) || 0 }))
      .sort((a, b) => b.count - a.count);

    const max = entries[0]?.count || 1;

    return entries.map(entry => ({
      emotion: entry.emotion,
      count: entry.count,
      width: Math.max(8, Math.round((entry.count / max) * 100)),
      color: this.getEmotionColor(entry.emotion)
    }));
  }


  getSummaryHighlights(): Array<{ label: string; value: string }> {
    const summary = this.getParsedSummary();

    if (!summary) {
      return [];
    }

    const items: Array<{ label: string; value: string }> = [];

    if (typeof summary.entries === 'number') {
      items.push({ label: 'Emotion Entries', value: String(summary.entries) });
    }

    if (typeof summary.averageIntensity === 'number') {
      items.push({ label: 'Avg Intensity', value: summary.averageIntensity.toFixed(2) });
    }

    if (summary.dominantEmotion) {
      items.push({ label: 'Dominant Emotion', value: summary.dominantEmotion });
    }

    if (typeof summary.totalSessions === 'number') {
      items.push({ label: 'Sessions in Range', value: String(summary.totalSessions) });
    }

    return items;
  }

  getReadableSummary(): string {
    const summary = this.getParsedSummary();

    if (!summary) {
      return 'No structured summary is available for this report.';
    }

    const dominantEmotion = summary.dominantEmotion || 'neutral';
    const entries = typeof summary.entries === 'number' ? summary.entries : 0;
    const avgIntensity = typeof summary.averageIntensity === 'number' ? summary.averageIntensity.toFixed(2) : '0.00';

    return `Across ${entries} emotion entries, the dominant emotion was ${dominantEmotion} with an average intensity of ${avgIntensity}.`;
  }

  formatTimeRange(timeRange?: string): string {
    if (!timeRange) {
      return 'N/A';
    }

    const parts = timeRange.split(' to ');
    if (parts.length !== 2) {
      return timeRange;
    }

    return `${this.formatDateLabel(parts[0])} - ${this.formatDateLabel(parts[1])}`;
  }

  private getEmotionColor(emotion: string): string {
    const key = (emotion || '').toLowerCase();

    if (key.includes('joy') || key.includes('happy')) {
      return '#f7b955';
    }
    if (key.includes('calm') || key.includes('relax') || key.includes('neutral')) {
      return '#35d6c1';
    }
    if (key.includes('sad') || key.includes('lonely')) {
      return '#60a5fa';
    }
    if (key.includes('anx') || key.includes('stress') || key.includes('worry')) {
      return '#fb923c';
    }
    if (key.includes('anger') || key.includes('angry') || key.includes('frustr')) {
      return '#ef5d6c';
    }
    if (key.includes('exhaust') || key.includes('tired') || key.includes('fatigue')) {
      return '#a78bfa';
    }

    return '#94a3b8';
  }

  private formatDateLabel(value?: string): string {
    if (!value) {
      return 'N/A';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return value;
    }

    return date.toLocaleString();
  }

  private getToday(): string {
    return new Date().toISOString().slice(0, 10);
  }

  private getDefaultStartDate(): string {
    const date = new Date();
    date.setDate(date.getDate() - 7);
    return date.toISOString().slice(0, 10);
  }

  /**
   * Get emoji for emotion display
   */
  getEmotionEmoji(emotion: string): string {
    const key = (emotion || '').toLowerCase();
    
    if (key.includes('joy') || key.includes('happy')) return '😊';
    if (key.includes('calm') || key.includes('relax') || key.includes('neutral')) return '😌';
    if (key.includes('sad') || key.includes('lonely')) return '😢';
    if (key.includes('anx') || key.includes('stress') || key.includes('worry')) return '😰';
    if (key.includes('anger') || key.includes('angry') || key.includes('frustr')) return '😠';
    if (key.includes('exhaust') || key.includes('tired') || key.includes('fatigue')) return '😴';
    
    return '😐';
  }

  /**
   * Get intensity badge color
   */
  getIntensityBadgeClass(intensity: number): string {
    if (intensity >= 7) return 'badge bg-danger';
    if (intensity >= 4) return 'badge bg-warning';
    return 'badge bg-success';
  }

  /**
   * Get intensity label
   */
  getIntensityLabel(intensity: number): string {
    if (intensity >= 7) return 'High';
    if (intensity >= 4) return 'Moderate';
    return 'Low';
  }

  /**
   * Group graph logs by date for better visualization
   */
  getGroupedGraphLogs(): Array<{ date: string; points: EmotionalJourneyPoint[] }> {
    const grouped = new Map<string, EmotionalJourneyPoint[]>();
    
    this.graphLogs().forEach(point => {
      const date = new Date(point.timestamp).toLocaleDateString();
      if (!grouped.has(date)) {
        grouped.set(date, []);
      }
      grouped.get(date)!.push(point);
    });
    
    return Array.from(grouped.entries()).map(([date, points]) => ({
      date,
      points: points.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime())
    })).sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }

  /**
   * Get graph log summary statistics
   */
  getGraphLogStats(): { totalPoints: number; avgIntensity: number; maxIntensity: number; minIntensity: number } {
    if (!this.graphLogs().length) {
      return { totalPoints: 0, avgIntensity: 0, maxIntensity: 0, minIntensity: 0 };
    }
    
    const intensities = this.graphLogs().map(p => p.intensity);
    return {
      totalPoints: this.graphLogs().length,
      avgIntensity: Math.round(intensities.reduce((a, b) => a + b, 0) / intensities.length * 10) / 10,
      maxIntensity: Math.max(...intensities),
      minIntensity: Math.min(...intensities)
    };
  }

  /**
   * Get the most common emotion from graph logs
   */
  getMostCommonEmotion(): string | null {
    if (!this.graphLogs().length) {
      return null;
    }

    const emotionCounts = new Map<string, number>();
    
    this.graphLogs().forEach(point => {
      const emotion = (point.emotion || 'neutral').toLowerCase();
      emotionCounts.set(emotion, (emotionCounts.get(emotion) || 0) + 1);
    });

    let maxEmotion: string | null = null;
    let maxCount = 0;

    emotionCounts.forEach((count, emotion) => {
      if (count > maxCount) {
        maxCount = count;
        maxEmotion = emotion;
      }
    });

    return maxEmotion;
  }

  goBack(): void {
    this.router.navigate(['/chat']);
  }
}
