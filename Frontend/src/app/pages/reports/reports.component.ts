import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ReportsService } from '../../services/reports.service';
import { ReportSummary } from '../../models';
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
  imports: [CommonModule, FormsModule, TranslatePipe],
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

  reportType = 'weekly';
  startDate = this.getDefaultStartDate();
  endDate = this.getToday();

  constructor(private reportsService: ReportsService) {}

  ngOnInit(): void {
    this.loadReports();
  }

  loadReports(): void {
    this.loading.set(true);
    this.error.set(null);

    this.reportsService.getReports().subscribe({
      next: reports => {
        this.reports.set(reports);
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
      },
      error: error => {
        console.error('Failed to load report detail', error);
        this.error.set('Unable to load the selected report.');
      }
    });
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

  getIntensityTrendPoints(): string {
    const summary = this.getParsedSummary();
    const trend = (summary?.intensityTrend || []).filter(point => Number.isFinite(point.intensity));

    if (trend.length < 2) {
      return '';
    }

    return trend
      .map((point, index) => {
        const x = (index / (trend.length - 1)) * 100;
        const clamped = Math.max(1, Math.min(10, Number(point.intensity) || 1));
        const y = 36 - ((clamped - 1) / 9) * 34;
        return `${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');
  }

  hasTrendData(): boolean {
    return !!this.getIntensityTrendPoints();
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
}
