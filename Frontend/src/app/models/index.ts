// Auth Models
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  username: string;
}

export interface JwtResponse {
  token: string;
  username: string;
  type?: string;
}

export type AuthMode = 'anonymous' | 'registered';

// Chat Models
export interface ChatRequest {
  userMessage: string;
  intensityScore: number;
  language: string;
}

export interface ChatResponse {
  conversationId: string;
  greeting: string;
  assessment: string;
  mainContent: string;
  strategies: string;
  followUp: string;
  fullResponse: string;
  pathway: PathwayType;
  approach: ApproachType;
  intensity: number;
  emotion: string;
  cycleNumber: number;
  shouldContinueLoop: boolean;
  sessionEnding: boolean;
  endingReason: string;
  sessionDurationMinutes: number;
  metadata: Record<string, any>;
}

export type PathwayType = 'PREVENT' | 'INTERVENE' | 'SAFETY';
export type ApproachType = 'EMPATHIC' | 'SYMPATHETIC';

export interface ChatContext {
  conversationId: string;
  currentEmotion: string;
  intensityScore: number;
  pathway: PathwayType | string;
  approach: ApproachType | string;
  cycleCount: number;
  sessionDurationMinutes: number;
}

// Emotion Models
export interface EmotionTrend {
  emotion: string;
  count: number;
  avgIntensity: number;
  lastRecorded: string;
}

export interface EmotionHistory {
  id: string | number;
  emotion: string;
  intensity: number;
  userMessage: string;
  source: string;
  timestamp: string;
}

export interface SessionSummary {
  id: string;
  userId: string;
  sessionId: string;
  startedAt: string | null;
  endedAt: string | null;
  isActive: boolean;
  messageCount: number;
  sessionDuration: number | null;
  dominantEmotion?: string;
  sessionTopic?: string;
}

export interface DashboardData {
  totalSessions: number;
  totalEmotionRecords: number;
  hasActiveSession: boolean;
  activeSessionId?: string;
  dominantEmotion?: string;
  averageIntensity?: number;
  emotionTrend?: 'improving' | 'declining' | 'stable' | string;
  emotionDistribution?: Record<string, number>;
  recentSessions: SessionSummary[];
}

export interface PaginatedSessions {
  sessions: SessionSummary[];
  currentPage: number;
  totalPages: number;
  totalItems: number;
  pageSize: number;
}

export interface SessionMessage {
  id: string;
  sender: string;
  content: string;
  timestamp: string | null;
  emotion?: string | null;
  intensity?: number | null;
}

export interface SessionDetails {
  session: SessionSummary;
  messages: SessionMessage[];
  messageCount: number;
  userMessageCount?: number;
  botMessageCount?: number;
  sessionEmotions?: string[];
}

export interface EmotionalJourneyPoint {
  timestamp: string;
  emotion: string;
  intensity: number;
  sentiment: number;
}

// Message Model for Chat UI
export interface Message {
  id: string;
  content: string;
  isUser: boolean;
  timestamp: Date;
  emotion?: string;
  intensity?: number;
  tips?: string[];
}

export interface UserProfile {
  id: string;
  username: string;
  email: string;
  fullName?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  age?: number | null;
  phone?: string | null;
  avatarUrl?: string | null;
  bio?: string | null;
  currentEmotionalState?: string | null;
  dateOfBirth?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
  isActive: boolean;
  isAnonymous: boolean;
  isVerified: boolean;
}

export interface UserAccountUpdateRequest {
  username?: string;
  currentPassword?: string;
  newPassword?: string;
}

export interface UserPreferences {
  id?: string;
  userId: string;
  language: 'en' | 'ms';
  theme: string;
  timezone: string;
  notificationEnabled: boolean;
  emailNotifications: boolean;
  dataCollectionConsent: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReportSummary {
  id: string;
  userId: string;
  reportType: string;
  startDate: string;
  endDate: string;
  generatedAt: string;
  summary: string;
  totalSessions: number;
  averageMoodScore: number;
}

// Emotion Selection
export interface EmotionOption {
  id: string;
  name: string;
  icon: string;
  color: string;
  description: string;
}

export const EMOTIONS: EmotionOption[] = [
  { id: 'calm', name: 'Calm', icon: 'bi-peace', color: '#10b981', description: 'Feeling peaceful and balanced' },
  { id: 'happy', name: 'Happy', icon: 'bi-emoji-smile', color: '#fbbf24', description: 'Feeling joyful and positive' },
  { id: 'relaxed', name: 'Relaxed', icon: 'bi-cloud', color: '#06b6d4', description: 'Feeling at ease and comfortable' },
  { id: 'stressed', name: 'Stressed', icon: 'bi-lightning', color: '#f59e0b', description: 'Feeling overwhelmed or pressured' },
  { id: 'anxious', name: 'Anxious', icon: 'bi-exclamation-triangle', color: '#fb923c', description: 'Feeling worried or nervous' },
  { id: 'sad', name: 'Sad', icon: 'bi-cloud-rain', color: '#60a5fa', description: 'Feeling down or melancholic' },
  { id: 'lonely', name: 'Lonely', icon: 'bi-person-dash', color: '#a78bfa', description: 'Feeling isolated or disconnected' },
  { id: 'frustrated', name: 'Frustrated', icon: 'bi-emoji-frown', color: '#f97316', description: 'Feeling irritated or blocked' },
  { id: 'angry', name: 'Angry', icon: 'bi-fire', color: '#ef4444', description: 'Feeling intense displeasure' },
  { id: 'hopeless', name: 'Hopeless', icon: 'bi-heart-break', color: '#dc2626', description: 'Feeling without hope' }
];
