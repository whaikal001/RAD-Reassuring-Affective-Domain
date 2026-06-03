import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { ChatService } from '../../services/chat.service';
import { EMOTIONS, EmotionalJourneyPoint, PaginatedSessions, SessionDetails } from '../../models';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './history.component.html',
  styleUrl: './history.component.scss'
})
export class HistoryComponent implements OnInit {
  loading = signal(true);
  error = signal<string | null>(null);
  sessionsData = signal<PaginatedSessions | null>(null);
  journey = signal<EmotionalJourneyPoint[]>([]);
  currentPage = signal(0);
  readonly pageSize = 8;
  sessionThemes = signal<Map<string, string>>(new Map());
  
  // Selected session for side panel
  selectedSessionId = signal<string | null>(null);
  sessionDetailsMap = signal<Map<string, SessionDetails>>(new Map());
  loadingSessionDetails = signal(false);
  
  // Chart stats
  emotionStats = signal<any>(null);
  trendIndicator = signal<string>('stable');
  averageIntensity = signal<number>(0);
  maxIntensity = signal<number>(0);
  minIntensity = signal<number>(10);
  intensityZoneStats = signal<any>(null);

  // Make Object available to template
  Object = Object;

  constructor(
    private chatService: ChatService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      sessions: this.chatService.getSessions(this.currentPage(), this.pageSize),
      journey: this.chatService.getEmotionalJourney().pipe(
        catchError(error => {
          console.warn('Unable to load emotional journey data', error);
          return of([] as EmotionalJourneyPoint[]);
        })
      )
    }).subscribe({
      next: ({ sessions, journey }) => {
        this.sessionsData.set(sessions);
        this.journey.set(journey);
        
        // Load themes for each session
        this.loadSessionThemes(sessions.sessions);

        // The trend graph now lives only on the Dashboard (analytics section).
        // Here we just compute the journey statistics used by the stat cards,
        // intensity zones, emotion distribution and coping tips.
        this.calculateStats(journey);

        this.loading.set(false);
      },
      error: (error) => {
        console.error('Failed to load history', error);
        this.error.set('Unable to load session history right now.');
        this.loading.set(false);
      }
    });
  }

  /**
   * Load chat themes for all sessions in parallel
   */
  private loadSessionThemes(sessions: any[]): void {
    const themes = new Map<string, string>();
    const requests = sessions.map(session =>
      this.chatService.getSessionDetails(session.id).pipe(
        catchError(() => of(null)) // Silently fail if theme can't be loaded
      )
    );

    forkJoin(requests).subscribe({
      next: (results) => {
        results.forEach((details, index) => {
          if (details && sessions[index]) {
            const theme = this.extractChatTheme(details);
            themes.set(sessions[index].id, theme);
          }
        });
        this.sessionThemes.set(themes);
      },
      error: () => {} // Silently fail
    });
  }

  goToPage(step: number): void {
    const nextPage = this.currentPage() + step;

    if (nextPage < 0) {
      return;
    }

    this.currentPage.set(nextPage);
    this.loadData();
  }

  canGoNext(): boolean {
    const sessions = this.sessionsData();
    return !!sessions && sessions.currentPage + 1 < sessions.totalPages;
  }

  getEmotionIcon(emotion: string): string {
    const found = EMOTIONS.find(item => item.id.toLowerCase() === emotion.toLowerCase());
    return found?.icon || 'bi-emoji-neutral';
  }

  journeyTail(): EmotionalJourneyPoint[] {
    return this.journey().slice(-6);
  }

  /**
   * Calculate emotional statistics
   */
  private calculateStats(journey: EmotionalJourneyPoint[]): void {
    if (journey.length === 0) return;

    // Average intensity
    const avgIntensity = journey.reduce((sum, p) => sum + p.intensity, 0) / journey.length;
    this.averageIntensity.set(Math.round(avgIntensity * 10) / 10);

    // Min/Max intensity
    const intensities = journey.map(p => p.intensity);
    this.maxIntensity.set(Math.max(...intensities));
    this.minIntensity.set(Math.min(...intensities));

    // Emotion distribution
    const emotionMap: Record<string, number> = {};
    journey.forEach(p => {
      emotionMap[p.emotion] = (emotionMap[p.emotion] || 0) + 1;
    });
    this.emotionStats.set(emotionMap);

    // Intensity zone statistics
    this.calculateIntensityZones(intensities);

    // Trend indicator
    const recentAvg = journey.slice(-3).reduce((sum, p) => sum + p.intensity, 0) / Math.min(3, journey.length);
    const earlierAvg = journey.slice(0, 3).reduce((sum, p) => sum + p.intensity, 0) / Math.min(3, journey.length);
    
    if (recentAvg < earlierAvg - 1) {
      this.trendIndicator.set('improving');
    } else if (recentAvg > earlierAvg + 1) {
      this.trendIndicator.set('declining');
    } else {
      this.trendIndicator.set('stable');
    }
  }

  /**
   * Calculate intensity zone distribution
   */
  private calculateIntensityZones(intensities: number[]): void {
    const zones = {
      highStress: intensities.filter(i => i >= 7).length,
      moderate: intensities.filter(i => i >= 4 && i < 7).length,
      low: intensities.filter(i => i < 4).length
    };
    this.intensityZoneStats.set(zones);
  }

  /**
   * Temporary method to access intensity percentages in template
   */

  /**
   * Get most common emotion from stats
   */
  getMostCommonEmotion(): string {
    const stats = this.emotionStats();
    if (!stats || Object.keys(stats).length === 0) return '';
    
    const maxEmotion = Object.keys(stats).reduce((a, b) => 
      stats[a] > stats[b] ? a : b
    );
    return maxEmotion;
  }

  /**
   * Get emotion entries for template
   */
  getEmotionEntries(): Array<[string, number]> {
    const stats = this.emotionStats();
    if (!stats) return [];
    return (Object.entries(stats) as Array<[string, number]>)
      .map(([emotion, count]) => [emotion, Number(count) || 0] as [string, number])
      .sort((a, b) => b[1] - a[1]);
  }

  /** Bar width (%) for an emotion-distribution count, relative to the most frequent emotion. */
  getEmotionWidth(count: number): number {
    const entries = this.getEmotionEntries();
    const max = entries.length ? Math.max(...entries.map(e => e[1])) : 0;
    if (!max) return 0;
    return Math.max(8, Math.round((count / max) * 100));
  }

  /**
   * Get emotion color for visualization
   */
  private getEmotionColor(emotion: string): string {
    const normalized = (emotion || '').toLowerCase();

    const colorMap: Record<string, string> = {
      stress: '#ef5d6c',
      stressed: '#ef5d6c',
      anxious: '#fb7185',
      anxiety: '#fb7185',
      worried: '#fb7185',
      overwhelmed: '#ff8a9d',
      sad: '#7d8bff',
      sadness: '#7d8bff',
      down: '#7d8bff',
      lonely: '#9ea8ff',
      isolated: '#a9b2ff',
      happy: '#25d7d7',
      happiness: '#25d7d7',
      excited: '#18c78f',
      joy: '#18c78f',
      joyful: '#18c78f',
      content: '#7ee7d5',
      calm: '#77a9ff',
      relaxed: '#77a9ff',
      peaceful: '#a7c4ff',
      angry: '#ff6b6b',
      frustrated: '#ff8a9d',
      irritable: '#ffb3ba',
      neutral: '#8f9bff',
      hopeless: '#ff0000',
      panic: '#ff0000',
      afraid: '#ff6b9d'
    };

    // Check for exact match
    if (colorMap[normalized]) {
      return colorMap[normalized];
    }

    // Partial match
    for (const [emotion, color] of Object.entries(colorMap)) {
      if (normalized.includes(emotion)) {
        return color;
      }
    }

    return '#8f9bff'; // Default
  }

  /**
   * Get emotion emoji
   */
  getEmotionEmoji(emotion: string): string {
    const normalized = (emotion || '').toLowerCase();
    
    if (normalized.includes('stress') || normalized.includes('anxious') || normalized.includes('worried')) return '😰';
    if (normalized.includes('sad') || normalized.includes('down') || normalized.includes('lonely')) return '😔';
    if (normalized.includes('happy') || normalized.includes('joy') || normalized.includes('excited')) return '😊';
    if (normalized.includes('calm') || normalized.includes('peaceful') || normalized.includes('relaxed')) return '😌';
    if (normalized.includes('angry') || normalized.includes('frustrated')) return '😠';
    if (normalized.includes('afraid') || normalized.includes('panic') || normalized.includes('hopeless')) return '🆘';
    
    return '😐';
  }

  /**
   * Public method for template to get emotion color
   */
  getEmotionColorForDist(emotion: string): string {
    return this.getEmotionColor(emotion);
  }

  /**
   * Get intensity zone label for UI
   */
  getIntensityZoneLabel(intensity: number): string {
    if (intensity >= 7) return 'High Stress';
    if (intensity >= 4) return 'Moderate';
    return 'Low Stress';
  }

  /**
   * Get intensity zone emoji
   */
  getIntensityZoneEmoji(intensity: number): string {
    if (intensity >= 7) return '🔴';
    if (intensity >= 4) return '🟡';
    return '🟢';
  }

  /**
   * Get intensity zone statistics for report
   */
  getIntensityZonePercentages(): any {
    const stats = this.intensityZoneStats();
    if (!stats) return null;
    const total = stats.highStress + stats.moderate + stats.low;
    return {
      highStress: Math.round((stats.highStress / total) * 100),
      moderate: Math.round((stats.moderate / total) * 100),
      low: Math.round((stats.low / total) * 100),
      counts: stats
    };
  }

  /**
   * Generate emotional wellbeing report
   */
  generateReport(): string {
    const journey = this.journey();
    if (journey.length === 0) return 'No data available';

    const zonePercentages = this.getIntensityZonePercentages();
    const mostCommon = this.getMostCommonEmotion();
    const trend = this.trendIndicator();
    const startDate = new Date(journey[0].timestamp).toLocaleDateString();
    const endDate = new Date(journey[journey.length - 1].timestamp).toLocaleDateString();

    let report = `EMOTIONAL WELLBEING REPORT\n`;
    report += `${'='.repeat(50)}\n\n`;
    
    report += `📊 PERIOD: ${startDate} to ${endDate}\n`;
    report += `📈 TOTAL DATA POINTS: ${journey.length}\n\n`;

    report += `INTENSITY STATISTICS\n`;
    report += `${'-'.repeat(50)}\n`;
    report += `Average Intensity: ${this.averageIntensity()}/10\n`;
    report += `Peak Intensity: ${this.maxIntensity()}/10\n`;
    report += `Lowest Intensity: ${this.minIntensity()}/10\n`;
    report += `Intensity Range: ${this.maxIntensity() - this.minIntensity()} points\n\n`;

    report += `INTENSITY DISTRIBUTION\n`;
    report += `🔴 High Stress (7-10): ${zonePercentages.counts.highStress} instances (${zonePercentages.highStress}%)\n`;
    report += `🟡 Moderate (4-6): ${zonePercentages.counts.moderate} instances (${zonePercentages.moderate}%)\n`;
    report += `🟢 Low Stress (0-3): ${zonePercentages.counts.low} instances (${zonePercentages.low}%)\n\n`;

    report += `EMOTIONAL PATTERNS\n`;
    report += `${'-'.repeat(50)}\n`;
    report += `Most Common Emotion: ${mostCommon}\n`;
    report += `Trend: ${trend === 'improving' ? '↗️ Improving' : trend === 'declining' ? '↘️ Declining' : '➡️ Stable'}\n\n`;

    report += `RECOMMENDATIONS\n`;
    report += `${'-'.repeat(50)}\n`;
    if (zonePercentages.highStress > 40) {
      report += `• Consider stress management techniques\n`;
      report += `• Schedule more relaxation time\n`;
    }
    if (trend === 'declining') {
      report += `• Intensity is increasing - be mindful of stressors\n`;
      report += `• Consider seeking support if needed\n`;
    }
    if (trend === 'improving') {
      report += `• Great progress! Continue current strategies\n`;
      report += `• Keep maintaining these positive patterns\n`;
    }
    report += `• Use the emotional insights to guide your day\n`;

    return report;
  }

  /**
   * Download report as text file
   */
  downloadReport(): void {
    const report = this.generateReport();
    const element = document.createElement('a');
    const file = new Blob([report], { type: 'text/plain' });
    element.href = URL.createObjectURL(file);
    element.download = `emotional-report-${new Date().toISOString().split('T')[0]}.txt`;
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }

  /**
   * Generate a meaningful session title based on chat content
   * Example: "Work Stress" instead of "Apr 14, 02:22 PM"
   */
  getSessionTitle(session: any): string {
    const theme = this.sessionThemes()?.get(session.id);
    if (theme) {
      return theme;
    }

    // Fallback to date if theme not yet loaded
    if (session.startedAt) {
      const date = new Date(session.startedAt);
      return date.toLocaleDateString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    }

    return 'Chat Session';
  }

  /**
   * Extract main theme/topic from session messages
   * Example: "I can't sleep" → "Sleep Issues", "Work is overwhelming" → "Work Stress"
   */
  private extractChatTheme(sessionDetails: any): string {
    if (!sessionDetails?.messages || sessionDetails.messages.length === 0) {
      return '💬 Chat Session';
    }

    // Get first user message (most revealing)
    const firstUserMsg = sessionDetails.messages.find((m: any) => m.sender?.toLowerCase() === 'user');
    if (!firstUserMsg?.content) {
      return '💬 Chat Session';
    }

    const text = firstUserMsg.content.toLowerCase();
    const theme = this.classifyTheme(text);
    return theme;
  }

  /**
   * Classify chat theme from text content
   * Examples: "Work Stress", "Sleep Issues", "Relationship Problems", etc.
   */
  private classifyTheme(text: string): string {
    // Work/Career related
    if (/(work|job|career|boss|deadline|project|meeting|office|stressed|pressure|workload)/.test(text)) {
      return '💼 Work & Career';
    }

    // Sleep/Rest
    if (/(sleep|insomnia|tired|exhausted|can't sleep|awake|night|rest|fatigue)/.test(text)) {
      return '😴 Sleep & Rest';
    }

    // Relationships
    if (/(relationship|boyfriend|girlfriend|partner|wife|husband|family|friend|breakup|arguing)/.test(text)) {
      return '💑 Relationships';
    }

    // School/Education
    if (/(school|exam|test|homework|study|grade|college|university|assignment)/.test(text)) {
      return '📚 School & Study';
    }

    // Anxiety/Panic
    if (/(anxiety|panic|attack|nervous|worried|anxious|fear|afraid)/.test(text)) {
      return '😰 Anxiety & Worry';
    }

    // Depression/Sadness
    if (/(depressed|depression|sad|down|blue|lonely|hopeless|suicide)/.test(text)) {
      return '😔 Mental Health';
    }

    // Health/Medical
    if (/(health|sick|illness|pain|body|doctor|medical|hospital|symptom)/.test(text)) {
      return '🏥 Health & Wellness';
    }

    // Money/Financial
    if (/(money|financial|debt|bill|expense|afford|cost|broke|income)/.test(text)) {
      return '💰 Financial';
    }

    // Goals/Motivation
    if (/(goal|motivation|improve|achieve|success|dream|aspiration|future)/.test(text)) {
      return '🎯 Goals & Growth';
    }

    // Social/Loneliness
    if (/(lonely|alone|isolation|isolated|social|friend|community)/.test(text)) {
      return '👥 Social Connection';
    }

    // Happiness/Positive
    if (/(happy|great|wonderful|excited|grateful|love|amazing|good)/.test(text)) {
      return '😊 Positive Vibes';
    }

    // General emotional
    if (/(feeling|emotion|mood|feel|how are you)/.test(text)) {
      return '💭 Emotional Check-in';
    }

    // Default
    return '💬 Chat Session';
  }

  /**
   * Toggle session expansion to show details
   */
  selectSession(sessionId: string): void {
    this.selectedSessionId.set(sessionId);
    this.loadSessionDetailsIfNeeded(sessionId);
  }

  closeSessionPanel(): void {
    this.selectedSessionId.set(null);
  }

  getCurrentSession() {
    const sessions = this.sessionsData()?.sessions || [];
    const selectedId = this.selectedSessionId();
    return sessions.find(s => s.id === selectedId);
  }

  /**
   * Load session details if not already loaded
   */
  private loadSessionDetailsIfNeeded(sessionId: string): void {
    const detailsMap = this.sessionDetailsMap();
    if (detailsMap.has(sessionId)) {
      return; // Already loaded
    }

    this.loadingSessionDetails.set(true);
    this.chatService.getSessionDetails(sessionId).subscribe({
      next: (details) => {
        const updated = new Map(detailsMap);
        updated.set(sessionId, details);
        this.sessionDetailsMap.set(updated);
        this.loadingSessionDetails.set(false);
      },
      error: (error) => {
        console.error('Failed to load session details', error);
        this.loadingSessionDetails.set(false);
      }
    });
  }

  /**
   * Get session details from map
   */
  getSessionDetails(sessionId: string): SessionDetails | undefined {
    return this.sessionDetailsMap().get(sessionId);
  }

  /**
   * Resume a chat session
   */
  resumeSession(sessionId: string): void {
    this.router.navigate(['/chat', { sessionId }]);
  }

  /**
   * Get session report
   */
  getSessionReport(sessionId: string): string {
    const details = this.getSessionDetails(sessionId);
    if (!details || !details.session) {
      return 'No report available';
    }

    const session = details.session;
    const messageCount = details.messageCount || 0;
    const emotions = details.sessionEmotions || [];

    let report = `SESSION REPORT\n`;
    report += `${'='.repeat(50)}\n\n`;
    
    report += `Session Topic: ${session.sessionTopic || 'General Chat'}\n`;
    report += `Started: ${new Date(session.startedAt || '').toLocaleString()}\n`;
    report += `Duration: ${session.sessionDuration || 0} minutes\n`;
    report += `Messages: ${messageCount}\n\n`;

    report += `EMOTIONAL INSIGHTS\n`;
    report += `${'-'.repeat(50)}\n`;
    report += `Dominant Emotion: ${session.dominantEmotion || 'Not recorded'}\n`;
    report += `Emotions Recorded: ${emotions.length > 0 ? emotions.join(', ') : 'None'}\n`;
    report += `Status: ${session.isActive ? 'Active' : 'Ended'}\n\n`;

    return report;
  }

  goBack(): void {
    this.router.navigate(['/chat']);
  }
}
