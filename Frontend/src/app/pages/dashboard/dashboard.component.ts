import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ChatService } from '../../services/chat.service';
import { DashboardData, EMOTIONS } from '../../models';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  loading = signal(true);
  error = signal<string | null>(null);
  dashboardData = signal<DashboardData | null>(null);
  
  private maxCount = 1;

  emotionDistributionEntries = computed(() => {
    const distribution = this.dashboardData()?.emotionDistribution ?? {};
    return Object.entries(distribution)
      .map(([emotion, count]) => ({ emotion, count }))
      .sort((left, right) => right.count - left.count);
  });

  recommendation = computed(() => {
    const trend = this.dashboardData()?.emotionTrend;

    if (trend === 'declining') {
      return 'Review history and generate a report';
    }

    if (trend === 'improving') {
      return 'Keep the current support routine';
    }

    return 'Continue tracking with regular sessions';
  });

  trendBadgeClass = computed(() => {
    const trend = this.dashboardData()?.emotionTrend;

    if (trend === 'improving') {
      return 'bg-success';
    }

    if (trend === 'declining') {
      return 'bg-danger';
    }

    return 'bg-secondary';
  });

  recentSessionSeries = computed(() => {
    const sessions = this.dashboardData()?.recentSessions ?? [];
    return [...sessions]
      .slice(-7)
      .map(session => ({
        label: this.formatShortDate(session.startedAt),
        value: session.messageCount || 0
      }));
  });

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {
    this.loadData();
  }

  refreshData(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.chatService.getDashboard().subscribe({
      next: (data) => {
        this.dashboardData.set(data);
        const distributionCounts = Object.values(data.emotionDistribution ?? {});

        if (distributionCounts.length) {
          this.maxCount = Math.max(...distributionCounts);
        } else {
          this.maxCount = 1;
        }
        
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Dashboard error:', err);
        this.error.set('Unable to load dashboard data. Please try again.');
        this.loading.set(false);
      }
    });
  }

  getPercentage(count: number): number {
    return (count / this.maxCount) * 100;
  }

  formatIntensity(value?: number): string {
    return typeof value === 'number' ? value.toFixed(1) : '0.0';
  }

  getEmotionIcon(emotion: string): string {
    const found = EMOTIONS.find(e => e.id.toLowerCase() === emotion.toLowerCase());
    return found?.icon || 'bi-emoji-neutral';
  }

  getEmotionColor(emotion: string): string {
    const found = EMOTIONS.find(e => e.id.toLowerCase() === emotion.toLowerCase());
    return found?.color || '#6366f1';
  }

  buildTrendPoints(): string {
    const series = this.recentSessionSeries();

    if (!series.length) {
      return '';
    }

    const max = Math.max(...series.map(item => item.value), 1);
    const stepX = 100 / Math.max(series.length - 1, 1);

    return series
      .map((item, index) => {
        const x = stepX * index;
        const normalized = item.value / max;
        const y = 32 - normalized * 28;
        return `${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(' ');
  }

  private formatShortDate(dateValue: string | null): string {
    if (!dateValue) {
      return '-';
    }

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) {
      return '-';
    }

    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }
}
