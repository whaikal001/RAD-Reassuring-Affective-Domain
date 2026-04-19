/// <reference types="jasmine" />
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
import { ChatComponent } from './chat.component';

describe('ChatComponent', () => {
  function createComponent(sendReturnsError = false): ChatComponent {
    const chatServiceMock: any = {
      language: signal('en'),
      messages: signal([]),
      isLoading: signal(false),
      currentEmotion: signal(null),
      lastResponse: signal(null),
      getContext: () => of(null),
      getGoals: () => of({ goals: [] }),
      resetSession: () => of(undefined),
      setLanguage: jasmine.createSpy('setLanguage'),
      sendMessage: jasmine.createSpy('sendMessage').and.callFake(() => {
        return sendReturnsError ? throwError(() => new Error('network')) : of({});
      }),
      endSession: jasmine.createSpy('endSession').and.returnValue(of(undefined))
    };

    const authServiceMock: any = {
      userId: signal('ABCD'),
      isAnonymous: signal(true)
    };

    const ttsServiceMock: any = {
      speak: jasmine.createSpy('speak'),
      isAutoSpeakEnabled: jasmine.createSpy('isAutoSpeakEnabled').and.returnValue(false),
      isSpeaking: jasmine.createSpy('isSpeaking').and.returnValue(false)
    };

    const uiFeedbackMock: any = {
      info: jasmine.createSpy('info'),
      success: jasmine.createSpy('success'),
      warning: jasmine.createSpy('warning'),
      error: jasmine.createSpy('error')
    };

    return new ChatComponent(chatServiceMock, authServiceMock, ttsServiceMock, uiFeedbackMock);
  }

  it('sends message on Enter key without Shift', () => {
    const component = createComponent();
    component.messageInput = 'hello';

    const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false });
    spyOn(event, 'preventDefault');

    component.onMessageKeydown(event);

    expect(event.preventDefault).toHaveBeenCalled();
    expect((component as any).chatService.sendMessage).toHaveBeenCalled();
  });

  it('does not send on Shift+Enter', () => {
    const component = createComponent();
    component.messageInput = 'hello';

    const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
    spyOn(event, 'preventDefault');

    component.onMessageKeydown(event);

    expect(event.preventDefault).not.toHaveBeenCalled();
    expect((component as any).chatService.sendMessage).not.toHaveBeenCalled();
  });

  it('shows retry error state when send fails', () => {
    const component = createComponent(true);
    component.messageInput = 'hello';
    spyOn(console, 'error');

    component.sendMessage();

    expect(component.sendError()).toBeTruthy();
  });
});
