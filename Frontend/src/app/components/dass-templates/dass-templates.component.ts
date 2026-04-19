import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TemplatesService, TemplateItem } from '../../services/templates.service';
import { ChatService } from '../../services/chat.service';
import { ScreeningService } from '../../services/screening.service';
import { UiFeedbackService } from '../../services/ui-feedback.service';

@Component({
  selector: 'app-dass-templates',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dass-templates.component.html',
  styleUrls: ['./dass-templates.component.scss']
})
export class DassTemplatesComponent implements OnInit {
  templates: TemplateItem[] = [];
  current: TemplateItem | null = null;
  loading = false;
  index = 0;
  answers: number[] = [];
  userInput = '';
  submitting = false;
  completed = false;

  constructor(private templatesService: TemplatesService, private chatService: ChatService,
              private screeningService: ScreeningService, private uiFeedback: UiFeedbackService) {}

  ngOnInit(): void {
    this.loading = true;
    this.templatesService.fetchTemplates().subscribe({
      next: t => {
        this.templates = t || [];
        this.current = this.templates.length ? this.templates[0] : null;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  submitUserInput(): void {
    if (!this.userInput.trim()) return;
    const input = this.userInput.toLowerCase();
    const value = this.matchInputToValue(input);
    this.userInput = '';
    this.processAnswer(value);
  }

  submitOption(value: number): void {
    this.processAnswer(value);
  }

  private matchInputToValue(text: string): number {
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

  private processAnswer(value: number) {
    if (!this.current) return;
    this.answers.push(value);
    
    // Find the selected option label
    const selectedOption = this.current.options.find(opt => opt.value === value);
    const selectedLabel = selectedOption?.label || `Option ${value}`;
    
    // Add user's selection as a message bubble
    this.chatService.addLocalUserMessage(selectedLabel);
    
    // Add bot's empathic follow-up
    const follow = (this.current.followUp || {})[value];
    if (follow) {
      this.chatService.addLocalBotMessage(follow, 'neutral', 3);
    }
    this.templatesService.trackResponse(this.current.id, value, this.current.prompt);

    // Move to next question or submit screening
    if (this.index + 1 < this.templates.length) {
      this.index++;
      this.current = this.templates[this.index];
      // Show next question intro and prompt
      this.chatService.addLocalBotMessage(this.current.intro, 'neutral', 2);
      this.chatService.addLocalBotMessage(this.current.prompt, 'neutral', 3);
    } else {
      // All questions answered - submit screening
      this.submitting = true;
      const sum = this.answers.reduce((a,b) => a + b, 0);
      const count = this.answers.length || 1;
      const avg = Math.round(sum / count);
      const filled = this.answers.slice();
      while (filled.length < 7) filled.push(avg);

      const consent = localStorage.getItem('socializer_consent') === 'true';
      const language = localStorage.getItem('socializer_language') || 'en';

      this.screeningService.submitScreening(consent, language, filled).subscribe({
        next: res => {
          this.submitting = false;
          this.completed = true;
          try { localStorage.setItem('screening_completed', 'true'); } catch(e){}
          localStorage.setItem('screening_action', res.action || 'allow');
          if (res.message) localStorage.setItem('screening_message', res.message);
          if (res.action === 'intervention') {
            this.uiFeedback.error('Safety', res.message || 'High stress detected.');
          } else if (res.action === 'prevention') {
            this.uiFeedback.info('Prevention', res.message || 'Mild stress detected.');
          } else {
            this.uiFeedback.success('Welcome', 'You may proceed to chat.');
          }
          this.chatService.addLocalBotMessage('Thank you for sharing. I understand how you\'re feeling. Let\'s talk more if you\'d like.', 'neutral', 3);
        },
        error: err => {
          this.submitting = false;
          console.error('Screening submit failed', err);
          this.uiFeedback.error('Screening failed', 'Unable to submit screening.');
        }
      });
    }
  }
}
