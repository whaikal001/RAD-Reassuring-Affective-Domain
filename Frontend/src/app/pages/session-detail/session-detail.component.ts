import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { ChatService } from '../../services/chat.service';
import { EMOTIONS, SessionDetails } from '../../models';
import { TranslatePipe } from '../../pipes/t.pipe';

@Component({
  selector: 'app-session-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './session-detail.component.html',
  styleUrl: './session-detail.component.scss'
})
export class SessionDetailComponent implements OnInit {
  loading = signal(true);
  error = signal<string | null>(null);
  details = signal<SessionDetails | null>(null);

  constructor(
    private route: ActivatedRoute,
    private chatService: ChatService
  ) {}

  ngOnInit(): void {
    const sessionId = this.route.snapshot.paramMap.get('id');

    if (!sessionId) {
      this.error.set('Session id is missing.');
      this.loading.set(false);
      return;
    }

    this.chatService.getSessionDetails(sessionId).subscribe({
      next: details => {
        this.details.set(details);
        this.loading.set(false);
      },
      error: error => {
        console.error('Failed to load session detail', error);
        this.error.set('Unable to load the selected session.');
        this.loading.set(false);
      }
    });
  }

  isUserMessage(sender: string): boolean {
    return sender.toLowerCase() === 'user';
  }

  getEmotionColor(emotion: string): string {
    const found = EMOTIONS.find(item => item.id.toLowerCase() === emotion.toLowerCase());
    return found?.color || '#6366f1';
  }
}
