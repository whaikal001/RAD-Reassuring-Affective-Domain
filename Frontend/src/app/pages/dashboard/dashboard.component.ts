import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ChatService } from '../../services/chat.service';
import { ProfileService } from '../../services/profile.service';
import { AuthService } from '../../services/auth.service';
import { DashboardData, UserProfile } from '../../models';
import { TranslatePipe } from '../../pipes/t.pipe';

type DashTab = 'overview' | 'progress' | 'sessions' | 'badges';

interface EqSkill { name: string; value: number; color: string; }
interface Kpi { icon: string; label: string; value: string; sub: string; tone: string; }
interface RecentItem { title: string; icon: string; color: string; when: string; minutes: number; score: number; }
interface Badge { name: string; icon: string; hint: string; unlocked: boolean; }
interface WeeklyGoal { label: string; points: number; done: boolean; }

/**
 * Personal growth dashboard (distinct from the admin's aggregate view).
 *
 * Real metrics come from getDashboard() + the user's profile. The EQ-skill scores,
 * badges, levels, streak and XP have no backend source yet, so they're derived
 * DETERMINISTICALLY from the real metrics + a per-user seed (see `seed()`), which keeps
 * them stable per user instead of flickering. Swap these computed blocks for real data
 * once an EQ-scoring endpoint exists — the template/styling won't need to change.
 *
 * Theming: every colour comes from the global CSS variables, so light/dark is automatic.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  private chatService = inject(ChatService);
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);

  loading = signal(true);
  error = signal<string | null>(null);
  dashboardData = signal<DashboardData | null>(null);
  profile = signal<UserProfile | null>(null);
  activeTab = signal<DashTab>('overview');

  // ---- Identity -------------------------------------------------------------
  displayName = computed(() =>
    this.profile()?.fullName?.trim()
    || this.authService.displayName()
    || this.profile()?.username?.trim()
    || 'Friend'
  );

  initials = computed(() => {
    const parts = this.displayName().split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? '') + (parts[1]?.[0] ?? '')).toUpperCase() || 'U';
  });

  verified = computed(() => !!this.profile()?.isVerified || this.authService.verified());

  memberSince = computed(() => {
    const created = this.profile()?.createdAt;
    if (!created) return '—';
    const d = new Date(created);
    return Number.isNaN(d.getTime()) ? '—' : d.toLocaleDateString(undefined, { month: 'short', year: 'numeric' });
  });

  shortId = computed(() => {
    const id = this.authService.userId() || this.profile()?.id || '';
    return id ? id.replace(/-/g, '').slice(0, 6).toUpperCase() : '—';
  });

  // ---- Core counts ----------------------------------------------------------
  totalSessions = computed(() => this.dashboardData()?.totalSessions ?? 0);

  sessionsThisWeek = computed(() => {
    const weekAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
    return (this.dashboardData()?.recentSessions ?? []).filter(s => {
      const t = s.startedAt ? new Date(s.startedAt).getTime() : NaN;
      return !Number.isNaN(t) && t >= weekAgo;
    }).length;
  });

  avgSessionMinutes = computed(() => {
    const durations = (this.dashboardData()?.recentSessions ?? [])
      .map(s => s.sessionDuration)
      .filter((m): m is number => typeof m === 'number' && m > 0);
    if (!durations.length) return 22;
    return Math.round(durations.reduce((a, b) => a + b, 0) / durations.length);
  });

  totalHours = computed(() => ((this.avgSessionMinutes() * this.totalSessions()) / 60).toFixed(1));

  // ---- EQ skills (illustrative, deterministic) ------------------------------
  eqSkills = computed<EqSkill[]>(() => {
    const base = [
      { name: 'Empathy', color: '#2fd199', lift: 12 },
      { name: 'Active listening', color: '#4f9bff', lift: 2 },
      { name: 'Clarity of expression', color: '#35d6c1', lift: 9 },
      { name: 'De-escalation', color: '#ee9b6f', lift: -5 },
      { name: 'Validation', color: '#e879b9', lift: 6 },
      { name: 'Tone awareness', color: '#f5c451', lift: 14 },
    ];
    // Skill grows a little with experience (more sessions => higher floor).
    const experience = Math.min(18, Math.floor(this.totalSessions() / 2));
    return base.map((b) => {
      const noise = this.seed(`skill-${b.name}`, 0, 10);
      const value = Math.max(40, Math.min(98, 64 + b.lift + experience + noise));
      return { name: b.name, color: b.color, value: Math.round(value) };
    });
  });

  overallScore = computed(() => {
    const skills = this.eqSkills();
    if (!skills.length) return 0;
    return Math.round(skills.reduce((a, s) => a + s.value, 0) / skills.length);
  });

  overallScoreDash = computed(() => {
    // Circumference of r=52 ring ≈ 326.7; fill proportional to score.
    const c = 2 * Math.PI * 52;
    return `${(this.overallScore() / 100) * c} ${c}`;
  });

  scoreGrowth = computed(() => {
    const trend = this.dashboardData()?.emotionTrend;
    const span = 6 + this.seed('growth', 0, 60) / 10; // 6.0–12.0
    if (trend === 'declining') return -(span / 2);
    return Number(span.toFixed(1));
  });

  level = computed(() => {
    const s = this.totalSessions();
    if (s >= 50) return { num: 5, title: 'Mentor' };
    if (s >= 30) return { num: 4, title: 'Empath' };
    if (s >= 15) return { num: 3, title: 'Connector' };
    if (s >= 5) return { num: 2, title: 'Explorer' };
    return { num: 1, title: 'Newcomer' };
  });

  streak = computed(() => {
    // Count consecutive calendar days with a session, from today backwards.
    const days = new Set(
      (this.dashboardData()?.recentSessions ?? [])
        .map(s => (s.startedAt ? new Date(s.startedAt) : null))
        .filter((d): d is Date => !!d && !Number.isNaN(d.getTime()))
        .map(d => d.toDateString())
    );
    if (!days.size) return 0;
    let count = 0;
    const cursor = new Date();
    for (let i = 0; i < 60; i++) {
      if (days.has(cursor.toDateString())) {
        count++;
        cursor.setDate(cursor.getDate() - 1);
      } else if (i === 0) {
        // allow streak to count even if no session yet *today*
        cursor.setDate(cursor.getDate() - 1);
      } else {
        break;
      }
    }
    return count;
  });

  // ---- KPI cards ------------------------------------------------------------
  kpis = computed<Kpi[]>(() => [
    {
      icon: 'bi-chat-heart-fill', tone: 'mint', label: 'Total sessions',
      value: String(this.totalSessions()),
      sub: this.sessionsThisWeek() > 0 ? `+${this.sessionsThisWeek()} this week` : 'Start one today',
    },
    {
      icon: 'bi-clock-history', tone: 'blue', label: 'Time engaged',
      value: `${this.totalHours()}h`,
      sub: `avg ${this.avgSessionMinutes()} min/session`,
    },
    {
      icon: 'bi-graph-up-arrow', tone: 'teal', label: 'Score growth',
      value: `${this.scoreGrowth() >= 0 ? '+' : ''}${this.scoreGrowth()}`,
      sub: 'since first session',
    },
    {
      icon: 'bi-award-fill', tone: 'amber', label: 'Badges earned',
      value: `${this.badgesEarned()} / ${this.badges().length}`,
      sub: `${this.badges().length - this.badgesEarned()} remaining`,
    },
  ]);

  // ---- Score history chart --------------------------------------------------
  /** "My score" trajectory: rises from (overall - growth) to overall across N points. */
  mySeries = computed<number[]>(() => {
    const n = 10;
    const end = this.overallScore();
    const start = Math.max(40, end - Math.abs(this.scoreGrowth()) - 3);
    return Array.from({ length: n }, (_, i) => {
      const t = i / (n - 1);
      const wobble = (this.seed(`my-${i}`, 0, 30) - 15) / 10; // ±1.5
      return Math.max(0, Math.min(100, start + (end - start) * t + wobble));
    });
  });

  /** "Avg user" comparison line: gentler, a few points lower. */
  avgSeries = computed<number[]>(() =>
    this.mySeries().map((_, i, arr) => {
      const t = i / (arr.length - 1);
      return Math.max(0, Math.min(100, 70 + t * 4));
    })
  );

  myPath = computed(() => this.buildPath(this.mySeries()));
  myArea = computed(() => this.buildArea(this.mySeries()));
  avgPath = computed(() => this.buildPath(this.avgSeries()));
  myDots = computed(() => this.points(this.mySeries()));

  /** Per-session points used for the hover comparison (viewBox coords + raw scores). */
  chartPoints = computed(() => {
    const my = this.mySeries();
    const avg = this.avgSeries();
    const n = my.length;
    if (n < 2) return [];
    const stepX = 100 / (n - 1);
    const toY = (v: number) => 37 - (v / 100) * 31;
    return my.map((m, i) => ({
      x: stepX * i,
      myY: toY(m),
      avgY: toY(avg[i]),
      my: Math.round(m),
      avg: Math.round(avg[i]),
      label: `S${i + 1}`,
    }));
  });

  /** Index of the session the cursor is over (null when not hovering). */
  hoveredIndex = signal<number | null>(null);

  /** The hovered point enriched with clamped tooltip coordinates (% of plot box). */
  hoveredPoint = computed(() => {
    const idx = this.hoveredIndex();
    if (idx === null) return null;
    const p = this.chartPoints()[idx];
    if (!p) return null;
    return {
      ...p,
      tipLeft: Math.max(13, Math.min(87, p.x)),       // keep the box inside the plot
      tipTop: Math.max(2, (p.myY / 40) * 100),         // follow the "my score" dot
    };
  });

  // ---- Recent sessions ------------------------------------------------------
  recentItems = computed<RecentItem[]>(() => {
    const palette = [
      { icon: 'bi-shield-check', color: '#2fd199' },
      { icon: 'bi-heart-pulse-fill', color: '#e879b9' },
      { icon: 'bi-arrow-repeat', color: '#f5c451' },
      { icon: 'bi-ear-fill', color: '#4f9bff' },
      { icon: 'bi-emoji-smile-fill', color: '#ee9b6f' },
    ];
    return (this.dashboardData()?.recentSessions ?? []).slice(0, 5).map((s, i) => {
      const p = palette[i % palette.length];
      return {
        title: s.sessionTopic?.trim() || (s.dominantEmotion ? `${this.cap(s.dominantEmotion)} support` : 'Support session'),
        icon: p.icon,
        color: p.color,
        when: this.relativeWhen(s.startedAt),
        minutes: typeof s.sessionDuration === 'number' ? s.sessionDuration : this.avgSessionMinutes(),
        score: 60 + this.seed(`sess-${s.id ?? i}`, 0, 38),
      };
    });
  });

  // ---- Badges (illustrative, unlocked from real milestones) -----------------
  badges = computed<Badge[]>(() => {
    const s = this.totalSessions();
    const streak = this.streak();
    const score = this.overallScore();
    return [
      { name: 'Empath Score 80+', icon: 'bi-heart-fill', hint: 'Reach an 80 EQ score', unlocked: score >= 80 },
      { name: 'On Fire', icon: 'bi-fire', hint: '10-day streak', unlocked: streak >= 10 },
      { name: 'Listener', icon: 'bi-soundwave', hint: '25 sessions', unlocked: s >= 25 },
      { name: 'Rising Star', icon: 'bi-star-fill', hint: 'Top 20%', unlocked: score >= 75 },
      { name: 'First Steps', icon: 'bi-flag-fill', hint: 'Finish your 1st session', unlocked: s >= 1 },
      { name: 'Consistent', icon: 'bi-calendar-check-fill', hint: '5 sessions', unlocked: s >= 5 },
      { name: 'Reflective', icon: 'bi-journal-text', hint: '15 sessions', unlocked: s >= 15 },
      { name: 'Master Score 95+', icon: 'bi-gem', hint: 'Reach a 95 EQ score', unlocked: score >= 95 },
      { name: 'Connector', icon: 'bi-people-fill', hint: '50 sessions', unlocked: s >= 50 },
      { name: 'Week Warrior', icon: 'bi-lightning-charge-fill', hint: '7-day streak', unlocked: streak >= 7 },
      { name: 'Balanced', icon: 'bi-peace-fill', hint: 'All skills above 60', unlocked: this.eqSkills().every(k => k.value >= 60) },
      { name: 'Verified', icon: 'bi-patch-check-fill', hint: 'Verify your email', unlocked: this.verified() },
    ];
  });

  badgesEarned = computed(() => this.badges().filter(b => b.unlocked).length);

  // ---- Weekly goals + XP ----------------------------------------------------
  weeklyGoals = computed<WeeklyGoal[]>(() => {
    const score = this.overallScore();
    const streak = this.streak();
    return [
      { label: 'Complete 3 sessions', points: 20, done: this.sessionsThisWeek() >= 3 },
      { label: 'Score above 80 twice', points: 15, done: score >= 80 },
      { label: 'Try de-escalation', points: 10, done: this.eqSkills().some(k => k.name === 'De-escalation' && k.value >= 60) },
      { label: 'Maintain 7-day streak', points: 25, done: streak >= 7 },
      { label: 'Improve listening score', points: 30, done: this.eqSkills().some(k => k.name === 'Active listening' && k.value >= 75) },
    ];
  });

  weeklyDone = computed(() => this.weeklyGoals().filter(g => g.done).length);
  weeklyXp = computed(() => Math.min(100, this.weeklyGoals().filter(g => g.done).reduce((a, g) => a + g.points, 0)));

  // ---------------------------------------------------------------------------
  ngOnInit(): void {
    this.loadData();
  }

  setTab(tab: DashTab): void {
    this.activeTab.set(tab);
  }

  /** Snap the hover to the nearest session as the cursor moves across the plot. */
  onChartMove(event: MouseEvent): void {
    const el = event.currentTarget as HTMLElement;
    const rect = el.getBoundingClientRect();
    const n = this.chartPoints().length;
    if (n < 2 || rect.width === 0) return;
    const ratio = (event.clientX - rect.left) / rect.width;
    const idx = Math.max(0, Math.min(n - 1, Math.round(ratio * (n - 1))));
    this.hoveredIndex.set(idx);
  }

  onChartLeave(): void {
    this.hoveredIndex.set(null);
  }

  refreshData(): void {
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    this.profileService.getCurrentUser().subscribe({
      next: (user) => this.profile.set(user),
      error: () => { /* identity falls back to auth signals */ },
    });

    this.chatService.getDashboard().subscribe({
      next: (data) => {
        this.dashboardData.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Dashboard error:', err);
        this.error.set('Unable to load dashboard data. Please try again.');
        this.loading.set(false);
      }
    });
  }

  // ---- chart helpers (viewBox 0..100 x, 0..40 y; score 0..100 -> y) ---------
  private toXY(values: number[]): { x: number; y: number }[] {
    if (values.length < 2) return [];
    const stepX = 100 / (values.length - 1);
    return values.map((v, i) => ({ x: stepX * i, y: 37 - (v / 100) * 31 }));
  }

  private buildPath(values: number[]): string {
    const pts = this.toXY(values);
    if (pts.length < 2) return '';
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

  private buildArea(values: number[]): string {
    const path = this.buildPath(values);
    if (!path) return '';
    const pts = this.toXY(values);
    return `${path} L ${pts[pts.length - 1].x.toFixed(2)} 40 L ${pts[0].x.toFixed(2)} 40 Z`;
  }

  private points(values: number[]): { x: number; y: number }[] {
    return this.toXY(values);
  }

  // ---- misc helpers ---------------------------------------------------------
  /** Deterministic pseudo-random in [min,max] from a userId-seeded string key. */
  private seed(key: string, min: number, max: number): number {
    const str = (this.authService.userId() ?? 'anon') + ':' + key;
    let h = 2166136261;
    for (let i = 0; i < str.length; i++) {
      h ^= str.charCodeAt(i);
      h = Math.imul(h, 16777619);
    }
    const unit = ((h >>> 0) % 1000) / 1000;
    return Math.round(min + unit * (max - min));
  }

  private cap(s: string): string {
    return s ? s.charAt(0).toUpperCase() + s.slice(1) : s;
  }

  private relativeWhen(dateValue: string | null): string {
    if (!dateValue) return '—';
    const d = new Date(dateValue);
    if (Number.isNaN(d.getTime())) return '—';
    const diffDays = Math.floor((Date.now() - d.getTime()) / (24 * 60 * 60 * 1000));
    if (diffDays <= 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
  }
}
