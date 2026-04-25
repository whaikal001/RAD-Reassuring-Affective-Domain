import { Component, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
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
export class AdminComponent implements AfterViewInit {
  @ViewChild('chartCanvas', { static: true }) chartCanvas!: ElementRef<HTMLCanvasElement>;
  private chart: any;

  users: Array<any> = [];

  constructor(private http: HttpClient, private router: Router) {}

  ngAfterViewInit(): void {
    this.http.get<any[]>(`${environment.apiUrl}/admin/emotions`).subscribe({
      next: (rows) => this.renderChart(rows),
      error: () => this.renderChart([])
    });
    this.fetchUsers();
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
    if (!confirm('Delete user? This action is permanent.')) return;
    this.http.delete(`${environment.apiUrl}/admin/users/${userId}`).subscribe({
      next: () => this.fetchUsers(),
      error: (err) => console.error('Delete failed', err)
    });
  }

  private renderChart(rows: any[]) {
    const labels = rows.map(r => r.date ?? '');
    const joy = rows.map(r => Number(r.joy ?? 0));
    const sadness = rows.map(r => Number(r.sadness ?? 0));
    const anger = rows.map(r => Number(r.anger ?? 0));

    const ctx = this.chartCanvas.nativeElement.getContext('2d')!;
    if (this.chart) { this.chart.destroy(); }

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels,
        datasets: [
          { label: 'Joy', data: joy, borderColor: '#7c3aed', backgroundColor: 'rgba(124,58,237,0.08)', tension: 0.3 },
          { label: 'Sadness', data: sadness, borderColor: '#0369a1', backgroundColor: 'rgba(3,105,161,0.06)', tension: 0.3 },
          { label: 'Anger', data: anger, borderColor: '#ef4444', backgroundColor: 'rgba(239,68,68,0.06)', tension: 0.3 }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          y: { beginAtZero: true, max: 1 }
        },
        plugins: {
          legend: { position: 'top' }
        }
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/chat']);
  }
}
