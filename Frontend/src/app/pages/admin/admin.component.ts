import { Component, AfterViewInit, ViewChild, ElementRef, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { environment } from '../../../environments/environment';
import Chart from 'chart.js/auto';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, TranslatePipe],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent implements OnInit, AfterViewInit {
  @ViewChild('activityTrendChart', { static: false }) activityTrendChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('emotionChart', { static: false }) emotionChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('intensityChart', { static: false }) intensityChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('activityTypesChart', { static: false }) activityTypesChart!: ElementRef<HTMLCanvasElement>;

  users: Array<any> = [];
  dashboardStats: any = {
    totalUsers: 0,
    activeTodayCount: 0,
    totalActivitiesCount: 0,
    averageEmotionScore: 0
  };
  recentActivities: any[] = [];
  topCountries: any[] = [];

  private charts: Map<string, any> = new Map();

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.fetchDashboardStats();
    this.fetchUsers();
  }

  ngAfterViewInit(): void {
    setTimeout(() => {
      this.renderCharts();
      this.fetchRecentActivities();
    }, 500);
  }

  private fetchDashboardStats(): void {
    this.http.get<any>(`${environment.apiUrl}/admin/dashboard/stats`).subscribe({
      next: (stats) => {
        this.dashboardStats = stats;
      },
      error: (err) => console.error('Failed to load dashboard stats:', err)
    });
  }

  private renderCharts(): void {
    this.renderActivityTrendChart();
    this.renderEmotionChart();
    this.renderIntensityChart();
    this.renderActivityTypesChart();
    this.fetchTopCountries();
  }

  private renderActivityTrendChart(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/dashboard/activity-trend?days=30`).subscribe({
      next: (data) => {
        const ctx = this.activityTrendChart?.nativeElement;
        if (!ctx) return;

        // Destroy existing chart
        if (this.charts.has('activityTrend')) {
          this.charts.get('activityTrend').destroy();
        }

        const dates = data.map(d => d.date).reverse();
        const counts = data.map(d => d.count).reverse();

        const chart = new Chart(ctx, {
          type: 'line',
          data: {
            labels: dates,
            datasets: [{
              label: 'Daily Activities',
              data: counts,
              borderColor: '#5566c4',
              backgroundColor: 'rgba(85, 102, 196, 0.14)',
              borderWidth: 2.5,
              fill: true,
              tension: 0.45,
              pointRadius: 0,
              pointHoverRadius: 6
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                display: true,
                labels: { color: 'rgba(61, 71, 107, 0.85)' }
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                ticks: { color: 'rgba(61, 71, 107, 0.85)' },
                grid: { color: 'rgba(85, 102, 196, 0.12)' }
              },
              x: {
                ticks: { color: 'rgba(61, 71, 107, 0.85)' },
                grid: { color: 'rgba(85, 102, 196, 0.12)' }
              }
            }
          }
        });
        this.charts.set('activityTrend', chart);
      }
    });
  }

  private renderEmotionChart(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/dashboard/emotions`).subscribe({
      next: (data) => {
        const ctx = this.emotionChart?.nativeElement;
        if (!ctx) return;

        if (this.charts.has('emotion')) {
          this.charts.get('emotion').destroy();
        }

        const emotions = data.map(d => d.primary_emotion);
        const counts = data.map(d => d.count);
        const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#FFA07A', '#98D8C8', '#F7DC6F'];

        const chart = new Chart(ctx, {
          type: 'doughnut',
          data: {
            labels: emotions,
            datasets: [{
              data: counts,
              backgroundColor: colors.slice(0, emotions.length),
              borderColor: '#ffffff',
              borderWidth: 2
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                display: true,
                labels: { color: 'rgba(61, 71, 107, 0.85)' }
              }
            }
          }
        });
        this.charts.set('emotion', chart);
      }
    });
  }

  private renderIntensityChart(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/dashboard/intensity`).subscribe({
      next: (data) => {
        const ctx = this.intensityChart?.nativeElement;
        if (!ctx) return;

        if (this.charts.has('intensity')) {
          this.charts.get('intensity').destroy();
        }

        const levels = data.map(d => d.intensity_level);
        const counts = data.map(d => d.count);

        const chart = new Chart(ctx, {
          type: 'bar',
          data: {
            labels: levels,
            datasets: [{
              label: 'User Intensity Levels',
              data: counts,
              backgroundColor: ['#45B7D1', '#FFA07A', '#FF6B6B'],
              borderColor: ['#3498db', '#e67e22', '#e74c3c'],
              borderWidth: 2
            }]
          },
          options: {
            indexAxis: 'x',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                display: true,
                labels: { color: 'rgba(61, 71, 107, 0.85)' }
              }
            },
            scales: {
              y: {
                beginAtZero: true,
                ticks: { color: 'rgba(61, 71, 107, 0.85)' },
                grid: { color: 'rgba(85, 102, 196, 0.12)' }
              },
              x: {
                ticks: { color: 'rgba(61, 71, 107, 0.85)' },
                grid: { color: 'rgba(85, 102, 196, 0.12)' }
              }
            }
          }
        });
        this.charts.set('intensity', chart);
      }
    });
  }

  private renderActivityTypesChart(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/dashboard/activity-types`).subscribe({
      next: (data) => {
        const ctx = this.activityTypesChart?.nativeElement;
        if (!ctx) return;

        if (this.charts.has('activityTypes')) {
          this.charts.get('activityTypes').destroy();
        }

        const types = data.map(d => d.activity_type);
        const counts = data.map(d => d.count);

        const chart = new Chart(ctx, {
          type: 'pie',
          data: {
            labels: types,
            datasets: [{
              data: counts,
              backgroundColor: [
                '#00d0ce', '#007bff', '#a78bfa', '#fbbf24', '#10b981', '#ef4444'
              ],
              borderColor: '#ffffff',
              borderWidth: 2
            }]
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
              legend: {
                display: true,
                labels: { color: 'rgba(61, 71, 107, 0.85)' }
              }
            }
          }
        });
        this.charts.set('activityTypes', chart);
      }
    });
  }

  private fetchRecentActivities(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/dashboard/recent-activity?hours=24`).subscribe({
      next: (activities) => {
        this.recentActivities = activities.slice(0, 10);
      },
      error: (err) => console.error('Failed to load recent activities:', err)
    });
  }

  private fetchTopCountries(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/dashboard/countries`).subscribe({
      next: (countries) => {
        this.topCountries = countries;
      },
      error: (err) => console.error('Failed to load top countries:', err)
    });
  }

  private fetchUsers(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/users`).subscribe({
      next: (u) => this.users = u || [],
      error: (err) => {
        console.error('Unable to load users:', err);
        this.users = [];
      }
    });
  }

  banUser(userId: string): void {
    this.http.post(`${environment.apiUrl}/admin/users/${userId}/ban`, {}).subscribe({
      next: () => this.fetchUsers(),
      error: (err) => console.error('Ban failed', err)
    });
  }

  unbanUser(userId: string): void {
    this.http.post(`${environment.apiUrl}/admin/users/${userId}/unban`, {}).subscribe({
      next: () => this.fetchUsers(),
      error: (err) => console.error('Unban failed', err)
    });
  }

  deleteUser(userId: string): void {
    if (!confirm('Are you sure you want to delete this user?')) return;
    this.http.delete(`${environment.apiUrl}/admin/users/${userId}`).subscribe({
      next: () => this.fetchUsers(),
      error: (err) => console.error('Delete failed', err)
    });
  }

  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  }
}
