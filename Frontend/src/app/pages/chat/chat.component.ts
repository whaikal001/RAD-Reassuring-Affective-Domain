import { Component, signal, ViewChild, ElementRef, AfterViewChecked, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ChatService } from '../../services/chat.service';
import { TtsService } from '../../services/tts.service';
import { AuthService } from '../../services/auth.service';
import { UiFeedbackService } from '../../services/ui-feedback.service';
import { ScreeningService } from '../../services/screening.service';
import { TemplatesService, TemplateItem } from '../../services/templates.service';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, TranslatePipe],
  templateUrl: './chat.component.html',
  styleUrl: './chat.component.scss'
})
export class ChatComponent implements AfterViewChecked, OnInit {
  @ViewChild('messagesContainer') private messagesContainer!: ElementRef;
  @ViewChild('messageTextarea') private messageTextarea?: ElementRef<HTMLTextAreaElement>;

  messageInput = '';
  language = signal('en');
  initialGreeting = signal('');
  useAI = signal(true);
  anonDataConsent = signal<boolean>(localStorage.getItem('socializer_data_consent') === 'true');
  consentGiven = signal<boolean>(localStorage.getItem('socializer_consent') === 'true');
  sidebarCollapsed = signal(false);
  mobileSidebarOpen = signal(false);
  expandedTipsMessageIds = signal<string[]>([]);
  sendError = signal<string | null>(null);
  lastSentUserMessage = signal<string | null>(null);
  activeGoals = signal<string[]>([]);
  emotionalMemory = signal<any>(null);
  showMemoryCard = signal<boolean>(false);
  screeningCompleted = signal<boolean>(false);
  screeningSubmitting = signal<boolean>(false);
  screeningAnswers = signal<number[]>([]);
  screeningQuestions = signal<string[]>([]);
  screeningIndex = signal<number>(0);
  screeningAction = signal<string | null>(localStorage.getItem('screening_action'));
  screeningMessage = signal<string | null>(localStorage.getItem('screening_message'));
  screeningResources = signal<string[] | null>(null);
  currentIntensity = signal<number | null>(null);

  // DASS Template properties
  dassTemplates: TemplateItem[] = [];
  dassCurrent: TemplateItem | null = null;
  dassLoading = false;
  dassIndex = 0;
  dassAnswers: number[] = [];
  dassUserInput = '';
  dassSubmitting = false;
  dassCompleted = false;
  greetingShown = false;
  dassQuestionsLoaded = false;
  selectedDassQuestions: TemplateItem[] = [];

  private shouldScroll = false;

  constructor(
    public chatService: ChatService,
    public authService: AuthService,
    public ttsService: TtsService,
    private uiFeedback: UiFeedbackService,
    private screeningService: ScreeningService,
    private templatesService: TemplatesService
  ) {}

  ngOnInit(): void {
    this.language.set(this.chatService.language());
    this.chatService.getContext().subscribe({
      next: () => {
        this.updateInitialGreeting();
      },
      error: () => {}
    });
    this.chatService.getGoals().subscribe({
      next: (res) => this.activeGoals.set(res.goals || []),
      error: () => this.activeGoals.set([])
    });

    // Load emotional memory for registered users
    if (!this.authService.isAnonymous()) {
      this.loadEmotionalMemory();
    }

    // Load DASS templates for screening
    this.loadDassTemplatesForScreening();

    // Start conversational screening for anonymous users
    if (this.authService.isAnonymous()) {
      // Reset chat session for fresh anonymous start
      this.chatService.resetSession().subscribe({
        next: () => {
          this.shouldScroll = true;
          // Show greeting and questions after session reset
          if (this.isScreeningVisible() && this.dassQuestionsLoaded) {
            this.showGreetingAndStartScreening();
          }
        },
        error: () => {
          // Even on error, try to show greeting
          if (this.isScreeningVisible() && this.dassQuestionsLoaded) {
            this.showGreetingAndStartScreening();
          }
        }
      });

      // Reset any previous anonymous screening when a fresh anonymous session begins
      try {
        localStorage.removeItem('screening_completed');
        localStorage.removeItem('screening_action');
        localStorage.removeItem('screening_message');
      } catch (e) {}

      this.screeningCompleted.set(false);
      this.screeningAction.set(null);
      this.screeningMessage.set(null);
    } else {
      // For registered users, use past session data
      this.loadPastSessionIntensity();
      if (this.isScreeningVisible() && this.dassQuestionsLoaded) {
        this.showGreetingAndStartScreening();
      }
    }
  }

  startConversationalScreening(): void {
    // This method is deprecated in favor of showGreetingAndStartScreening
    // Kept for backward compatibility
  }
  
    getCurrentQuestion(): string | null {
      const qs = this.screeningQuestions();
      const idx = this.screeningIndex();
      return qs && qs[idx] ? qs[idx] : null;
    }
  
    answerScreening(value: number): void {
        const idx = this.screeningIndex();
        this.setScreeningAnswer(idx, value);
        const next = idx + 1;
        if (next < this.screeningQuestions().length) {
          this.screeningIndex.set(next);
          // Render next question as bot message
          const nextQ = this.screeningQuestions()[next];
          if (nextQ) {
            this.chatService.addLocalBotMessage(nextQ, 'neutral', 4);
          }
        } else {
        // finished: prepare answers scaled to 7-item equivalent
        const answers = this.screeningAnswers().slice();
        // compute average of provided answers
        const sum = answers.reduce((a, b) => a + (b >= 0 ? b : 0), 0);
        const count = answers.filter(v => v >= 0).length || answers.length || 1;
        const avg = sum / count;
        const filled = answers.slice();
        while (filled.length < 7) {
          filled.push(Math.round(avg));
        }
      
        this.submitScreeningWithAnswers(filled);
        // indicate in chat the screening is being submitted
        this.chatService.addLocalBotMessage('Thanks — submitting your quick wellbeing check now.', 'neutral', 3);
      }
    }
  
    private submitScreeningWithAnswers(filledAnswers: number[]): void {
      this.screeningSubmitting.set(true);
      this.screeningService.submitScreening(this.consentGiven(), this.language(), filledAnswers).subscribe({
        next: res => {
          this.screeningSubmitting.set(false);
          this.screeningCompleted.set(true);
          try { localStorage.setItem('screening_completed', 'true'); } catch(e){}
          localStorage.setItem('screening_action', res.action || 'allow');
          if (res.message) localStorage.setItem('screening_message', res.message);
          this.screeningAction.set(res.action);
          this.screeningMessage.set(res.message || null);
          this.screeningResources.set(res.resources || null);
        
          if (res.action === 'intervention') {
            this.uiFeedback.error('Safety', res.message || 'High stress detected.');
          } else if (res.action === 'prevention') {
            this.uiFeedback.info('Prevention', res.message || 'Mild stress detected.');
          } else {
            this.uiFeedback.success('Welcome', 'You may proceed to chat.');
          }
        },
        error: err => {
          this.screeningSubmitting.set(false);
          console.error('Screening error', err);
          this.uiFeedback.error('Screening failed', 'Unable to submit screening. Please try again.');
        }
      });
    }

  isScreeningVisible(): boolean {
    // Show screening for anonymous users OR new registered users who haven't completed it
    const isAnonymous = this.authService.isAnonymous();
    const screeningNotCompleted = !this.screeningCompleted();
    return (isAnonymous || !this.currentIntensity()) && screeningNotCompleted;
  }

  setScreeningAnswer(index: number, value: number): void {
    this.screeningAnswers.update(arr => {
      const copy = [...arr];
      copy[index] = value;
      return copy;
    });
  }

  allAnswersProvided(): boolean {
    return this.screeningAnswers().every(v => v >= 0);
  }

  submitScreening(): void {
    if (!this.allAnswersProvided()) {
      this.uiFeedback.warning('Screening', 'Please answer all questions.');
      return;
    }

    this.screeningSubmitting.set(true);
    const answers = this.screeningAnswers();

    this.screeningService.submitScreening(this.consentGiven(), this.language(), answers).subscribe({
      next: res => {
        this.screeningSubmitting.set(false);
        this.screeningCompleted.set(true);
        localStorage.setItem('screening_completed', 'true');
        localStorage.setItem('screening_action', res.action || 'allow');
        if (res.message) localStorage.setItem('screening_message', res.message);
        this.screeningAction.set(res.action);
        this.screeningMessage.set(res.message || null);
        this.screeningResources.set(res.resources || null);

        if (res.action === 'intervention') {
          this.uiFeedback.error('Safety', res.message || 'High stress detected.');
        } else if (res.action === 'prevention') {
          this.uiFeedback.info('Prevention', res.message || 'Mild stress detected.');
        } else {
          this.uiFeedback.success('Welcome', 'You may proceed to chat.');
        }
      },
      error: err => {
        this.screeningSubmitting.set(false);
        console.error('Screening error', err);
        this.uiFeedback.error('Screening failed', 'Unable to submit screening. Please try again.');
      }
    });
  }

  skipScreening(): void {
    this.screeningCompleted.set(true);
    try {
      localStorage.setItem('screening_completed', 'true');
    } catch (e) {
      console.warn('Unable to persist screening flag', e);
    }
  }

  /**
   * Load past session intensity for registered users
   */
  private loadPastSessionIntensity(): void {
    this.chatService.getEmotionalJourney().subscribe({
      next: (journey) => {
        if (journey && journey.length > 0) {
          const lastEntry = journey[journey.length - 1];
          if (lastEntry && typeof lastEntry.intensity === 'number') {
            this.currentIntensity.set(lastEntry.intensity);
          }
        }
      },
      error: () => {} // Silently fail
    });
  }

  /**
   * Show greeting and start screening with 4 DASS questions
   */
  private showGreetingAndStartScreening(): void {
    // Don't add greeting as a chat message - let it show in welcome panel
    this.greetingShown = true;
    this.shouldScroll = true;

    // Wait before showing first question (gives user time to see welcome panel)
    setTimeout(() => {
      this.startNaturalQuestionFlow();
    }, 1200);
  }

  /**
   * Start the natural question flow with 4 DASS questions
   */
  private startNaturalQuestionFlow(): void {
    if (this.dassQuestionsLoaded && this.selectedDassQuestions.length > 0) {
      this.dassCurrent = this.selectedDassQuestions[0];
      this.dassIndex = 0;
      this.dassAnswers = [];
      this.dassCompleted = false;

      // Show first question as natural bot message (this triggers chat scroll)
      this.chatService.addLocalBotMessage(this.dassCurrent.prompt, 'neutral', 3);
      this.shouldScroll = true;
    }
  }

  /**
   * Load 4 random DASS templates for screening
   */
  loadDassTemplatesForScreening(): void {
    this.dassLoading = true;
    this.templatesService.fetchTemplates().subscribe({
      next: t => {
        this.dassTemplates = t || [];
        // Select 4 random questions
        this.selectedDassQuestions = this.selectRandomQuestions(this.dassTemplates, 4);
        this.dassQuestionsLoaded = true;
        this.dassLoading = false;
      },
      error: () => { this.dassLoading = false; }
    });
  }

  /**
   * Select N random unique questions from array
   */
  private selectRandomQuestions(questions: TemplateItem[], n: number): TemplateItem[] {
    const shuffled = [...questions].sort(() => Math.random() - 0.5);
    return shuffled.slice(0, Math.min(n, questions.length));
  }

  submitDassUserInput(): void {
    if (!this.dassUserInput.trim()) return;
    const input = this.dassUserInput.toLowerCase();
    const value = this.matchDassInputToValue(input);
    this.dassUserInput = '';
    this.processDassAnswer(value);
  }

  submitDassOption(value: number): void {
    this.processDassAnswer(value);
  }

  private matchDassInputToValue(text: string): number {
    // Match keywords to 0-3 intensity
    // 3: very, always, all, most, extremely, stressed, overwhelmed, tense
    // 2: yes, often, quite, pretty, many, a lot
    // 1: some, sometimes, little, bit, occasionally, maybe, ok
    // 0: not, no, nope, never, nothing, good, fine, well
    
    if (/\b(very|always|all|most|extremely|stressed|overwhelmed|tense|constant|continuously)\b/.test(text)) {
      return 3;
    }
    if (/\b(yes|often|quite|pretty|many|a lot|good amount)\b/.test(text)) {
      return 2;
    }
    if (/\b(some|sometimes|little|bit|occasionally|maybe|ok|kinda|sort of)\b/.test(text)) {
      return 1;
    }
    return 0;
  }

  private processDassAnswer(value: number) {
    if (!this.dassCurrent) return;
    this.dassAnswers.push(value);
    
    // Find the selected option label
    const selectedOption = this.dassCurrent.options.find(opt => opt.value === value);
    const selectedLabel = selectedOption?.label || `Option ${value}`;
    
    // Add user's selection as a message bubble
    this.chatService.addLocalUserMessage(selectedLabel);
    
    // Add bot's empathic follow-up
    const follow = (this.dassCurrent.followUp || {})[value];
    if (follow) {
      this.chatService.addLocalBotMessage(follow, 'neutral', 3);
    }
    this.templatesService.trackResponse(this.dassCurrent.id, value, this.dassCurrent.prompt);

    // Move to next question or finish
    if (this.dassIndex + 1 < this.selectedDassQuestions.length) {
      this.dassIndex++;
      this.dassCurrent = this.selectedDassQuestions[this.dassIndex];
      // Show next question as natural bot message
      setTimeout(() => {
        this.chatService.addLocalBotMessage(this.dassCurrent!.prompt, 'neutral', 3);
      }, 500);
    } else {
      // All 4 questions answered - calculate and track current intensity
      this.dassCompleted = true;
      this.trackCurrentIntensity();
    }
  }

  /**
   * Calculate and track current intensity after 4 questions answered
   * Only for anonymous users
   */
  private trackCurrentIntensity(): void {
    if (!this.authService.isAnonymous()) {
      // For registered users, intensity is already loaded from past session
      this.chatService.addLocalBotMessage('Thank you for sharing. I understand how you\'re feeling. Let\'s talk more if you\'d like.', 'neutral', 3);
      this.screeningCompleted.set(true);
      return;
    }

    // Calculate average intensity from 4 answers
    const sum = this.dassAnswers.reduce((a, b) => a + b, 0);
    const avg = Math.round(sum / (this.dassAnswers.length || 1));
    this.currentIntensity.set(avg);

    // Scale to 7-point DASS format
    const filled = this.dassAnswers.slice();
    while (filled.length < 7) filled.push(avg);

    const consent = localStorage.getItem('socializer_consent') === 'true';
    const language = localStorage.getItem('socializer_language') || 'en';

    // Submit screening for anonymous user
    this.screeningService.submitScreening(consent, language, filled).subscribe({
      next: res => {
        try { localStorage.setItem('screening_completed', 'true'); } catch(e){}
        localStorage.setItem('screening_action', res.action || 'allow');
        if (res.message) localStorage.setItem('screening_message', res.message);
        this.screeningCompleted.set(true);
        this.screeningAction.set(res.action || 'allow');
        this.screeningMessage.set(res.message || null);
        
        // Show intensity level and message
        const intensityLevel = this.getIntensityLabel(avg);
        const intensityMessage = this.buildIntensityMessage(avg, intensityLevel, language === 'ms');
        this.chatService.addLocalBotMessage(intensityMessage, intensityLevel, 3);
        
        // Set screening context emotion for follow-up messages to maintain intensity tone
        this.chatService.setScreeningContextEmotion(intensityLevel);
        
        if (res.action === 'intervention') {
          this.uiFeedback.error('Safety', res.message || 'High stress detected.');
        } else if (res.action === 'prevention') {
          this.uiFeedback.info('Prevention', res.message || 'Mild stress detected.');
        } else {
          this.uiFeedback.success('Welcome', 'You may proceed to chat.');
        }
      },
      error: err => {
        console.error('Screening submit failed', err);
        this.uiFeedback.error('Screening failed', 'Unable to submit screening.');
        this.screeningCompleted.set(true);
      }
    });
  }

  /**
   * Get intensity label based on numeric value (0-3)
   */
  private getIntensityLabel(intensity: number): string {
    if (intensity === 0) return 'calm';
    if (intensity === 1) return 'neutral';
    if (intensity === 2) return 'anxious';
    return 'stressed';
  }

  /**
   * Build intensity message with clear labeling
   */
  private buildIntensityMessage(intensity: number, label: string, isMalay: boolean): string {
    const intensityMap: { [key: number]: { en: string; ms: string } } = {
      0: { 
        en: 'Low Intensity - You seem calm and relaxed. Great! Let\'s continue our conversation.',
        ms: 'Keamatan Rendah - Anda nampak tenang dan rileks. Bagus! Mari kita teruskan perbincangan kami.'
      },
      1: { 
        en: 'Mild Intensity - You\'re feeling a bit stressed, but manageable. I\'m here to listen and support you.',
        ms: 'Keamatan Ringan - Anda mengalami sedikit tekanan, tetapi dapat dikendalikan. Saya di sini untuk mendengar dan menyokong anda.'
      },
      2: { 
        en: 'Moderate Intensity - You\'re experiencing noticeable stress. Let\'s explore what\'s on your mind and find some relief.',
        ms: 'Keamatan Sederhana - Anda mengalami tekanan yang ketara. Mari kita terokai apa yang ada di fikiran anda dan cari beberapa kelegaan.'
      },
      3: { 
        en: 'High Intensity - You\'re feeling quite stressed. I\'m here to help. Let\'s talk through what you\'re experiencing.',
        ms: 'Keamatan Tinggi - Anda mengalami banyak tekanan. Saya di sini untuk membantu. Mari kita bincangkan apa yang anda alami.'
      }
    };

    return intensityMap[intensity]?.[isMalay ? 'ms' : 'en'] || 
      (isMalay ? 'Terima kasih atas perkongsian anda. Mari kita bincangkan lebih lanjut.' : 
       'Thank you for sharing. Let\'s talk more if you\'d like.');
  }

  /**
   * Load user's emotional history to show memory and continuity
   */
  private loadEmotionalMemory(): void {
    this.chatService.getEmotionalJourney().subscribe({
      next: (journey) => {
        if (journey && journey.length > 0) {
          this.processEmotionalMemory(journey);
          this.showMemoryCard.set(true);
        }
      },
      error: () => {
        // Fallback to emotion history
        this.chatService.getEmotionHistory().subscribe({
          next: (history) => {
            if (history && history.length > 0) {
              this.processEmotionHistoryMemory(history);
              this.showMemoryCard.set(true);
            }
          },
          error: () => {} // Silently fail
        });
      }
    });
  }

  /**
   * Process emotional journey data to create memory
   */
  private processEmotionalMemory(journey: any[]): void {
    if (!journey || journey.length === 0) return;

    // Get most recent emotions (last 3)
    const recentJourney = journey.slice(-3).reverse();
    const dominantEmotion = journey[journey.length - 1];

    this.emotionalMemory.set({
      recentEmotions: recentJourney,
      dominantEmotion: dominantEmotion,
      frequency: journey.length,
      timestamp: new Date()
    });
  }

  /**
   * Process emotion history when journey API is unavailable
   */
  private processEmotionHistoryMemory(history: any[]): void {
    if (!history || history.length === 0) return;

    // Group by emotion and get frequencies
    const emotionMap: Record<string, any[]> = {};
    history.forEach(record => {
      const emotion = record.emotion || 'unknown';
      if (!emotionMap[emotion]) {
        emotionMap[emotion] = [];
      }
      emotionMap[emotion].push(record);
    });

    // Get most common emotion
    let dominantEmotion = '';
    let maxCount = 0;
    for (const [emotion, records] of Object.entries(emotionMap)) {
      if (records.length > maxCount) {
        maxCount = records.length;
        dominantEmotion = emotion;
      }
    }

    // Get recent emotions
    const recentEmotions = history.slice(-3).reverse().map(h => ({
      emotion: h.emotion,
      intensity: h.intensity,
      timestamp: h.timestamp
    }));

    this.emotionalMemory.set({
      recentEmotions: recentEmotions,
      dominantEmotion: dominantEmotion,
      frequency: history.length,
      timestamp: new Date()
    });
  }

  /**
   * Get memory message for the user
   * Example: "Last time you felt stressed too..."
   */
  getMemoryMessage(): string {
    const memory = this.emotionalMemory();
    if (!memory) return '';

    const current = this.chatService.currentEmotion() || this.chatService.currentIntensity();
    const recent = memory.recentEmotions || [];
    
    if (recent.length === 0) {
      return `Welcome back! I remember our ${memory.frequency} conversations together.`;
    }

    const lastEmotion = recent[0]?.emotion || 'that feeling';
    const isRepeating = recent.some((r: any) => this.emotionMatch(r.emotion, current));

    if (isRepeating) {
      return `I remember last time you felt ${lastEmotion} too. I'm here to support you again today. 💙`;
    }

    const emotions = recent.map((r: any) => r.emotion).join(', ');
    return `Welcome back! I remember you've felt ${emotions} before. How are you feeling now?`;
  }

  /**
   * Check if two emotions are similar
   */
  private emotionMatch(emotion1: string, emotion2: string | number): boolean {
    if (!emotion1 || !emotion2) return false;
    
    const normalized1 = String(emotion1).toLowerCase();
    const normalized2 = String(emotion2).toLowerCase();
    
    // Exact match
    if (normalized1 === normalized2) return true;
    
    // Semantic match for similar emotions
    const stressEmotions = ['stressed', 'anxious', 'worried', 'overwhelmed', 'pressure'];
    const sadEmotions = ['sad', 'down', 'lonely', 'disappointed', 'blue'];
    const happyEmotions = ['happy', 'excited', 'joyful', 'content', 'uplifted'];
    
    const checkGroup = (group: string[]) => 
      group.includes(normalized1) && group.includes(normalized2);
    
    return checkGroup(stressEmotions) || checkGroup(sadEmotions) || checkGroup(happyEmotions);
  }

  onAutoTtsChanged(event: Event): void {
    const checked = (event.target as HTMLInputElement)?.checked ?? false;
    this.ttsService.setAutoSpeakEnabled(checked);
    this.uiFeedback.success('Settings', checked ? 'Auto TTS enabled.' : 'Auto TTS disabled.');
  }

  setAnonDataConsent(event: Event): void {
    const checked = (event.target as HTMLInputElement)?.checked ?? false;
    this.anonDataConsent.set(checked);
    try {
      localStorage.setItem('socializer_data_consent', checked ? 'true' : 'false');
      this.uiFeedback.success('Settings', checked ? 'Data collection consent saved.' : 'Data collection consent removed.');
    } catch (e) {
      console.warn('Failed to persist anonymous data consent', e);
    }
  }

  private updateInitialGreeting(): void {
    this.initialGreeting.set(this.buildInitialGreeting());
  }

  acceptConsent(): void {
    this.consentGiven.set(true);
    localStorage.setItem('socializer_consent', 'true');
    this.uiFeedback.success('Thanks', 'Consent saved.');
  }

  getHelpline(): string | null {
    const meta = this.chatService.lastResponse()?.metadata || {};
    return meta['helpline'] || null;
  }

  newChat(): void {
    this.chatService.resetSession().subscribe({
      next: () => {
        this.expandedTipsMessageIds.set([]);
        this.sendError.set(null);
        this.lastSentUserMessage.set(null);
        this.shouldScroll = true;

        // Reset screening for anonymous users
        if (this.authService.isAnonymous()) {
          try {
            localStorage.removeItem('screening_completed');
            localStorage.removeItem('screening_action');
            localStorage.removeItem('screening_message');
          } catch (e) {}
          this.screeningCompleted.set(false);
          this.screeningAction.set(null);
          this.screeningMessage.set(null);
        }

        this.uiFeedback.success('New Chat', 'Started a fresh conversation.');
      },
      error: () => {
        this.expandedTipsMessageIds.set([]);
        this.sendError.set(null);
        this.lastSentUserMessage.set(null);
        this.shouldScroll = true;
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  setLanguage(lang: string): void {
    this.language.set(lang);
    this.chatService.setLanguage(lang);
    this.closeMobileSidebar();
    this.updateInitialGreeting();
  }

  toggleAI(): void {
    this.useAI.update(v => !v);
  }

  toggleSidebar(): void {
    this.sidebarCollapsed.update(v => !v);
  }

  toggleMobileSidebar(): void {
    this.mobileSidebarOpen.update(v => !v);
  }

  closeMobileSidebar(): void {
    this.mobileSidebarOpen.set(false);
  }

  sendMessage(): void {
    const message = this.messageInput.trim();

    if (!message) {
      return;
    }

    this.messageInput = '';
    this.autoResizeTextarea();
    this.submitMessage(message, true);
  }

  retryLastMessage(): void {
    const message = this.lastSentUserMessage();

    if (!message) {
      return;
    }

    this.submitMessage(message, false);
  }

  regenerateLastReply(): void {
    const messages = this.chatService.messages();
    const lastUserMessage = [...messages].reverse().find(message => message.isUser)?.content;

    if (!lastUserMessage) {
      this.uiFeedback.info('No Message', 'Send a message first before regenerating.');
      return;
    }

    this.submitMessage(lastUserMessage, false);
  }

  copyMessage(content: string): void {
    if (typeof navigator === 'undefined' || !navigator.clipboard) {
      this.uiFeedback.warning('Clipboard Unavailable', 'Your browser does not support copying from here.');
      return;
    }

    navigator.clipboard.writeText(content)
      .then(() => this.uiFeedback.success('Copied', 'Message copied to clipboard.', 2200))
      .catch(() => this.uiFeedback.error('Copy Failed', 'Could not copy this message.'));
  }

  onMessageKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  onMessageInput(): void {
    this.autoResizeTextarea();
  }

  private submitMessage(message: string, appendUserMessage: boolean): void {
    this.sendError.set(null);
    this.lastSentUserMessage.set(message);
    
    // Use screening intensity if just completed, otherwise infer from message
    let messageIntensity = this.inferIntensityFromMessage(message);
    const screeningIntensity = this.currentIntensity();
    if (this.screeningCompleted() && screeningIntensity !== null) {
      // Use current intensity from screening for bot context awareness
      messageIntensity = screeningIntensity;
    }

    this.shouldScroll = true;

    this.chatService.sendMessage(message, messageIntensity, this.useAI(), appendUserMessage)
      .subscribe({
        next: () => {
          this.autoSpeakLatestBotMessage();
          this.shouldScroll = true;
        },
        error: err => {
          console.error('Chat error:', err);
          this.sendError.set('Message could not be delivered. Please retry.');
        }
      });
  }

  sendQuickMessage(message: string): void {
    this.messageInput = message;
    this.sendMessage();
  }

  triggerQuickAction(action: 'breathe' | 'ground' | 'journal' | 'goal'): void {
    switch (action) {
      case 'breathe':
        this.sendQuickMessage('Guide me through a 2-minute breathing reset.');
        break;
      case 'ground':
        this.sendQuickMessage('Please guide me through the 5-4-3-2-1 grounding exercise.');
        break;
      case 'journal':
        this.sendQuickMessage('Give me one quick journal prompt for how I feel right now.');
        break;
      case 'goal':
        this.messageInput = 'Goal: I want to improve my emotional regulation this week.';
        this.sendMessage();
        this.chatService.getGoals().subscribe({
          next: (res) => this.activeGoals.set(res.goals || []),
          error: () => {}
        });
        break;
    }
  }

  latestSafetyLevel(): string {
    return this.chatService.lastResponse()?.metadata?.['safetyLevel'] || 'low';
  }

  private autoSpeakLatestBotMessage(): void {
    if (!this.ttsService.isAutoSpeakEnabled()) {
      return;
    }

    const latestBotMessage = [...this.chatService.messages()].reverse().find(message => !message.isUser);
    if (!latestBotMessage?.content) {
      return;
    }

    this.ttsService.speak(latestBotMessage.content, latestBotMessage.emotion);
  }

  endSession(): void {
    if (confirm('Are you sure you want to end this session?')) {
      this.chatService.endSession().subscribe();
    }
  }

  speakMessage(content: string, emotion?: string): void {
    this.ttsService.speak(content, emotion);
  }

  isBotFigureSpeaking(): boolean {
    return this.chatService.isLoading() || this.ttsService.isSpeaking();
  }

  getBotTone(emotion?: string | null): string {
    const normalized = (emotion || '').toLowerCase();

    if (!normalized) {
      return 'neutral';
    }

    if (/(happy|excited|joy|joyful|uplifted|content)/.test(normalized)) {
      return 'happy';
    }

    if (/(calm|relaxed|peaceful|neutral|okay|fine)/.test(normalized)) {
      return 'calm';
    }

    if (/(stressed|overloaded|anxious|worried|frustrated|irritable)/.test(normalized)) {
      return 'stressed';
    }

    if (/(sad|down|lonely|isolated|disappointed|blue)/.test(normalized)) {
      return 'sad';
    }

    if (/(angry|heated|hopeless|critical|unsafe|panic|fear|afraid)/.test(normalized)) {
      return 'critical';
    }

    return 'neutral';
  }

  getCurrentBotTone(): string {
    return this.getBotTone(this.chatService.currentEmotion() || this.chatService.lastResponse()?.emotion);
  }

  /**
   * Convert technical emotion labels to human-friendly descriptions with emojis
   * Example: "Neutral Intensity 4" → "😔 Slightly overwhelmed"
   */
  getHumanFriendlyEmotion(emotion?: string | null, intensity?: number): string {
    const normalized = (emotion || 'neutral').toLowerCase();
    const level = intensity || 0;

    const emotionMap: Record<string, { emoji: string; phrases: string[] }> = {
      happy: {
        emoji: '😊',
        phrases: ['Feeling great!', 'In a good mood', 'Uplifted and positive']
      },
      calm: {
        emoji: '😌',
        phrases: ['Peaceful and centered', 'Feeling relaxed', 'At ease']
      },
      stressed: {
        emoji: '😰',
        phrases: ['Feeling pressured', 'A bit overwhelmed', 'Under stress']
      },
      sad: {
        emoji: '😔',
        phrases: ['Feeling down', 'A bit low', 'Struggling emotionally']
      },
      critical: {
        emoji: '🆘',
        phrases: ['In crisis', 'Urgent support needed', 'Critical state']
      },
      neutral: {
        emoji: '😐',
        phrases: ['Steady state', 'Balanced', 'Centered']
      }
    };

    // Determine emotional tone
    let tone = 'neutral';
    if (/(happy|excited|joy|joyful|uplifted|content)/.test(normalized)) {
      tone = 'happy';
    } else if (/(calm|relaxed|peaceful|okay|fine)/.test(normalized)) {
      tone = 'calm';
    } else if (/(stressed|overloaded|anxious|worried|frustrated|irritable)/.test(normalized)) {
      tone = 'stressed';
    } else if (/(sad|down|lonely|isolated|disappointed|blue)/.test(normalized)) {
      tone = 'sad';
    } else if (/(angry|heated|hopeless|critical|unsafe|panic|fear|afraid)/.test(normalized)) {
      tone = 'critical';
    }

    const emotionData = emotionMap[tone] || emotionMap['neutral'];
    const phraseIndex = Math.min(level > 7 ? 2 : level > 4 ? 1 : 0, emotionData.phrases.length - 1);
    
    return `${emotionData.emoji} ${emotionData.phrases[phraseIndex]}`;
  }

  /**
   * Show the response transformation: empathy → sympathy
   * This is the CORE mechanic of SocializerAI
   */
  getResponseApproach(approach?: string | null): string {
    if (!approach) {
      return '💙 Empathy';
    }
    const normalized = approach.toUpperCase();

    // Support both enum-style values from backend (EMPATHY / SYMPATHY)
    // and adjective-style variants (EMPATHIC / SYMPATHETIC)
    if (normalized === 'SYMPATHY' || normalized === 'SYMPATHETIC') {
      return '🤝 Sympathy - Guided Tips & Actions';
    } else if (normalized === 'EMPATHY' || normalized === 'EMPATHIC') {
      return '💙 Empathy - Understanding & Support';
    }

    return '💙 Empathy';
  }

  /**
   * Safe helpers for template type-checking to determine approach icons.
   * Using methods avoids direct enum/string comparisons in the template which
   * can fail the Angular AOT type checker when approach is a typed enum.
   */
  isApproachSympathy(approach?: any): boolean {
    const a = (approach ?? '').toString().toUpperCase();
    return a === 'SYMPATHY' || a === 'SYMPATHETIC';
  }

  isApproachEmpathy(approach?: any): boolean {
    const a = (approach ?? '').toString().toUpperCase();
    return a === 'EMPATHY' || a === 'EMPATHIC';
  }

  /**
   * Show the loop cycle status and user progression
   * Demonstrates: empathy → sympathy → calm/safety → loop back
   */
  getCycleStatus(cycleNumber?: number, shouldContinue?: boolean, intensity?: number, isUser?: boolean): string {
    if (isUser) {
      return '';
    }

    const cycle = cycleNumber || 0;
    const level = intensity || 0;
    
    // Show progression through the loop
    if (level <= 3) {
      return `✅ Cycle ${cycle} - User feeling calm & safe`;
    } else if (level <= 5) {
      return `🔄 Cycle ${cycle} - Guided progress`;
    } else if (level >= 7) {
      return `🎯 Cycle ${cycle} - Intensive support`;
    }
    
    return `🔄 Cycle ${cycle}`;
  }

  getBotColor(tone: string, part: 'head' | 'body' | 'arm' | 'heart' | 'border' | 'line' | 'eye' | 'mouth' | 'cheek'): string {
    const palettes: Record<string, Record<typeof part, string>> = {
      happy: {
        head: '#25d7d7', body: '#18c78f', arm: '#9ff0dd', heart: '#ffd166', border: '#24c78f', line: '#ffffff', eye: '#ffffff', mouth: '#ffffff', cheek: 'rgba(255,255,255,0.30)'
      },
      calm: {
        head: '#77a9ff', body: '#4f7cff', arm: '#a7c4ff', heart: '#7ee7d5', border: '#77a9ff', line: '#ffffff', eye: '#ffffff', mouth: '#ffffff', cheek: 'rgba(255,255,255,0.22)'
      },
      stressed: {
        head: '#ffbe55', body: '#fb7185', arm: '#ffd7a4', heart: '#ff8a9d', border: '#fb7185', line: '#ffffff', eye: '#ffffff', mouth: '#ffffff', cheek: 'rgba(255,255,255,0.18)'
      },
      sad: {
        head: '#7d8bff', body: '#5d6ad8', arm: '#a9b2ff', heart: '#c4d4ff', border: '#7d8bff', line: '#ffffff', eye: '#ffffff', mouth: '#ffffff', cheek: 'rgba(255,255,255,0.16)'
      },
      critical: {
        head: '#ff8fa3', body: '#ef5d6c', arm: '#ffbcc7', heart: '#ffd2d8', border: '#ef5d6c', line: '#ffffff', eye: '#ffffff', mouth: '#ffffff', cheek: 'rgba(255,255,255,0.16)'
      },
      neutral: {
        head: '#8f9bff', body: '#5b69f5', arm: '#9ea8ff', heart: '#ff7f9b', border: '#c4cdff', line: '#ffffff', eye: '#ffffff', mouth: '#ffffff', cheek: 'rgba(255,255,255,0.18)'
      }
    };

    const palette = palettes[tone] || palettes['neutral'];
    return palette[part];
  }

  isLatestBotMessage(messageId: string): boolean {
    const messages = this.chatService.messages();

    for (let i = messages.length - 1; i >= 0; i--) {
      if (!messages[i].isUser) {
        return messages[i].id === messageId;
      }
    }

    return false;
  }

  toggleTips(messageId: string): void {
    this.expandedTipsMessageIds.update(messageIds =>
      messageIds.includes(messageId)
        ? messageIds.filter(id => id !== messageId)
        : [...messageIds, messageId]
    );
  }

  isTipsExpanded(messageId: string): boolean {
    return this.expandedTipsMessageIds().includes(messageId);
  }

  getSuggestedPrompts(): string[] {
    return [
      'Hi, I need someone to talk to',
      'I am feeling stressed today',
      'Can you help me calm down a bit?'
    ];
  }

  getUserInitials(): string {
    const raw = this.authService.userId() || '';

    if (!raw) {
      return 'YU';
    }

    const clean = raw.replace(/[^a-zA-Z0-9]/g, '').toUpperCase();
    return clean.slice(0, 2) || 'YU';
  }

  private scrollToBottom(): void {
    try {
      this.messagesContainer.nativeElement.scrollTop = 
        this.messagesContainer.nativeElement.scrollHeight;
    } catch (err) {}
  }

  private autoResizeTextarea(): void {
    const textarea = this.messageTextarea?.nativeElement;

    if (!textarea) {
      return;
    }

    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 160)}px`;
  }

  private addInitialGreetingIfNeeded(force = false): void {
    if (!force && this.chatService.messages().length > 0) {
      return;
    }

    // Do not inject the greeting as a chat message. The welcome panel
    // (shown when there are no messages) already renders the same
    // greeting via `getInitialGreeting()`. Leaving this method so
    // callers (ngOnInit, tests) can request the initial state, but
    // avoid creating a bot message on every login.
    this.shouldScroll = true;
  }

  private buildInitialGreeting(): string {
    const isMalay = this.language() === 'ms';

    if (this.authService.isAnonymous()) {
      // Simple greeting for anonymous user
      return isMalay 
        ? 'Hi there! 👋\n\nI am here to listen and support you. What\'s on your mind?'
        : 'Hi there! 👋\n\nI am here to listen and support you. What\'s on your mind?';
    }

    const displayName = this.authService.getDisplayName() || 'there';
    return isMalay
      ? `Hi ${displayName}! 👋\n\nI am here to listen and support you. What's on your mind?`
      : `Hi ${displayName}! 👋\n\nI am here to listen and support you. What's on your mind?`;
  }

  public getInitialGreeting(): string {
    const isMalay = this.language() === 'ms';
    const hour = new Date().getHours();
    const timeOfDay = this.getTimeOfDayLabel(hour);

    if (this.authService.isAnonymous()) {
      return isMalay
        ? `${timeOfDay}! Apa perasaan anda hari ini? Apa yang sedang bermain di fikiran anda?`
        : `${timeOfDay}! How's your day going so far? What's on your mind?`;
    }

    const displayName = this.authService.getDisplayName() || 'there';
    return isMalay
      ? `${timeOfDay}, ${displayName}! Selamat datang kembali. Apa yang ingin anda bincangkan hari ini?`
      : `${timeOfDay}, ${displayName}! Welcome back. What's on your mind today?`;
  }

  private getTimeOfDayLabel(hour: number): string {
    const isMalay = this.language() === 'ms';

    // 12 AM - 5 AM: Good Midnight
    if (hour >= 0 && hour < 5) {
      return isMalay ? 'Selamat tengah malam' : 'Good Midnight';
    }
    // 5 AM - 12 PM: Good Morning
    if (hour >= 5 && hour < 12) {
      return isMalay ? 'Selamat pagi' : 'Good Morning';
    }
    // 12 PM - 1 PM: Good Afternoon
    if (hour >= 12 && hour < 13) {
      return isMalay ? 'Selamat petang' : 'Good Afternoon';
    }
    // 1 PM - 6 PM: Good Evening
    if (hour >= 13 && hour < 18) {
      return isMalay ? 'Selamat malam' : 'Good Evening';
    }
    // 6 PM - 12 AM: Night!
    if (hour >= 18 && hour < 24) {
      return isMalay ? 'Selamat malam!' : 'Night!';
    }

    return isMalay ? 'Selamat malam' : 'Night!';
  }

  private inferIntensityFromMessage(message: string): number {
    const text = message.toLowerCase();

    let score = 4;

    // Strong distress indicators.
    if (/(suicide|kill myself|end it|can't go on|hopeless|worthless|panic attack|want to die|die|nak mati|mahu mati|nak bunuh diri|bunuh diri|tak mahu hidup|tak ingin hidup|mahu bunuh diri)/.test(text)) {
      return 10;
    }

    // Negative emotional words and stress terms.
    if (/(depressed|depression|sad|cry|stressed|stress|anxious|anxiety|angry|frustrated|overwhelmed|drained|lonely|afraid|scared|sedih|tertekan|putus asa|tak berdaya)/.test(text)) {
      score += 2;
    }

    // Intensifiers.
    if (/(very|really|so much|extremely|too much|can't|cannot|never)/.test(text)) {
      score += 1;
    }

    // Positive tone tends to lower intensity.
    if (/(okay|fine|better|good|calm|relaxed|happy|grateful)/.test(text)) {
      score -= 1;
    }

    // Lots of punctuation/emphasis often indicates higher emotional load.
    const emphasis = (text.match(/[!?]/g) || []).length;
    if (emphasis >= 2) {
      score += 1;
    }

    return Math.max(1, Math.min(10, score));
  }
}
