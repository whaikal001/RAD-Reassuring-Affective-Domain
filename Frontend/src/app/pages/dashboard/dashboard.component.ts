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

  /** Average-intensity gauge for the header ring (real metric, 0–10). */
  intensityPercent = computed(() => {
    const v = this.dashboardData()?.averageIntensity ?? 0;
    return Math.max(0, Math.min(100, (v / 10) * 100));
  });

  intensityZoneColor = computed(() => {
    const v = this.dashboardData()?.averageIntensity ?? 0;
    if (v >= 7) return '#ef5d6c';   // high — needs attention
    if (v >= 4) return '#f5a623';   // moderate
    return '#2fd199';               // calm
  });

  intensityZoneLabel = computed(() => {
    const v = this.dashboardData()?.averageIntensity ?? 0;
    if (v >= 7) return 'High';
    if (v >= 4) return 'Moderate';
    return 'Calm';
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

  /** Normalised points across a 0..100 x / 0..36 y viewBox, with soft top/bottom padding. */
  private trendPoints(): { x: number; y: number }[] {
    const series = this.recentSessionSeries();
    if (series.length < 2) {
      return [];
    }

    const max = Math.max(...series.map(item => item.value), 1);
    const stepX = 100 / (series.length - 1);

    return series.map((item, index) => ({
      x: stepX * index,
      // y in [6, 32]: leaves headroom at the top and a baseline gap at the bottom
      y: 32 - (item.value / max) * 26
    }));
  }

  /** Smooth curve through the points (Catmull-Rom converted to cubic beziers). */
  buildTrendPath(): string {
    const pts = this.trendPoints();
    if (pts.length < 2) {
      return '';
    }

    let d = `M ${pts[0].x.toFixed(2)} ${pts[0].y.toFixed(2)}`;
    for (let i = 0; i < pts.length - 1; i++) {
      const p0 = pts[i - 1] ?? pts[i];
      const p1 = pts[i];
      const p2 = pts[i + 1];
      const p3 = pts[i + 2] ?? p2;
      const cp1x = p1.x + (p2.x - p0.x) / 6;
      const cp1y = p1.y + (p2.y - p0.y) / 6;
      const cp2x = p2.x - (p3.x - p1.x) / 6;
      const cp2y = p2.y - (p3.y - p1.y) / 6;
      d += ` C ${cp1x.toFixed(2)} ${cp1y.toFixed(2)}, ${cp2x.toFixed(2)} ${cp2y.toFixed(2)}, ${p2.x.toFixed(2)} ${p2.y.toFixed(2)}`;
    }
    return d;
  }

  /** Same curve, closed to the baseline so it can be filled with a soft gradient. */
  buildTrendArea(): string {
    const path = this.buildTrendPath();
    if (!path) {
      return '';
    }
    const pts = this.trendPoints();
    const last = pts[pts.length - 1];
    const first = pts[0];
    return `${path} L ${last.x.toFixed(2)} 36 L ${first.x.toFixed(2)} 36 Z`;
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
