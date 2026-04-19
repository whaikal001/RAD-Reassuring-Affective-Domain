import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ChatService } from './chat.service';
import { AuthService } from './auth.service';

class AuthServiceStub {
  getUserId(): string {
    return '123e4567-e89b-12d3-a456-426614174000';
  }
}

describe('ChatService', () => {
  let service: ChatService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ChatService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useClass: AuthServiceStub }
      ]
    });

    service = TestBed.inject(ChatService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('adds user and bot messages on successful send', () => {
    service.sendMessage('hello there', 5).subscribe();

    const request = httpMock.expectOne(req => req.url.includes('/chat/flow/process-with-ai'));
    expect(request.request.method).toBe('POST');
    expect(service.messages().length).toBe(1);
    expect(service.messages()[0].isUser).toBeTrue();

    request.flush({
      conversationId: 'conv-1',
      greeting: 'Hi',
      assessment: '',
      mainContent: 'I am here with you.',
      strategies: 'Take a short breath',
      followUp: 'Want to continue?',
      fullResponse: 'Hi. I am here with you. Want to continue?',
      pathway: 'PREVENT',
      approach: 'EMPATHIC',
      intensity: 4,
      emotion: 'calm',
      cycleNumber: 1,
      shouldContinueLoop: true,
      sessionEnding: false,
      endingReason: '',
      sessionDurationMinutes: 2,
      metadata: {}
    });

    expect(service.messages().length).toBe(2);
    expect(service.messages()[1].isUser).toBeFalse();
  });

  it('does not append user message when appendUserMessage is false', () => {
    service.sendMessage('original', 5).subscribe();
    const firstReq = httpMock.expectOne(req => req.url.includes('/chat/flow/process-with-ai'));
    firstReq.flush({
      conversationId: 'conv-1',
      greeting: 'Hi',
      assessment: '',
      mainContent: 'Reply 1',
      strategies: '',
      followUp: '',
      fullResponse: 'Reply 1',
      pathway: 'PREVENT',
      approach: 'EMPATHIC',
      intensity: 5,
      emotion: 'calm',
      cycleNumber: 1,
      shouldContinueLoop: true,
      sessionEnding: false,
      endingReason: '',
      sessionDurationMinutes: 2,
      metadata: {}
    });

    const before = service.messages().length;
    service.sendMessage('original', 5, true, false).subscribe();

    const secondReq = httpMock.expectOne(req => req.url.includes('/chat/flow/process-with-ai'));
    secondReq.flush({
      conversationId: 'conv-1',
      greeting: 'Hi again',
      assessment: '',
      mainContent: 'Reply 2',
      strategies: '',
      followUp: '',
      fullResponse: 'Reply 2',
      pathway: 'PREVENT',
      approach: 'EMPATHIC',
      intensity: 5,
      emotion: 'calm',
      cycleNumber: 2,
      shouldContinueLoop: true,
      sessionEnding: false,
      endingReason: '',
      sessionDurationMinutes: 3,
      metadata: {}
    });

    expect(service.messages().length).toBe(before + 1);
    expect(service.messages()[service.messages().length - 1].isUser).toBeFalse();
  });
});
