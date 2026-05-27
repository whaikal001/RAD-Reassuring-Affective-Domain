import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * R3FCharacterBridgeService
 * Manages iframe communication between Angular (parent) and r3f React app (iframe)
 * Sends animation, emotion, and audio commands to the 3D character
 */
@Injectable({
  providedIn: 'root'
})
export class R3fCharacterBridgeService {
  private iframeRef: HTMLIFrameElement | null = null;
  private r3fOrigin = 'http://localhost:5173'; // r3f app origin
  private messageQueue: any[] = [];
  private isReady = new BehaviorSubject<boolean>(false);
  private pingTimer: number | null = null;

  readonly ready$ = this.isReady.asObservable();

  constructor() {
    // Listen for messages from r3f iframe
    window.addEventListener('message', (event) => this.handleMessage(event));
  }

  /**
   * Initialize the iframe bridge
   * Call this once when the character panel loads
   */
  initializeIframe(iframeElement: HTMLIFrameElement): void {
    this.iframeRef = iframeElement;

    this.sendPing();

    if (this.pingTimer !== null) {
      window.clearInterval(this.pingTimer);
    }

    this.pingTimer = window.setInterval(() => {
      if (this.isReady.value) {
        this.clearPingTimer();
        return;
      }

      this.sendPing();
    }, 1000);
  }

  /**
   * Send animation command to r3f character
   * @param animationName Animation to play (e.g., 'Talking_1', 'Laughing', 'Idle')
   */
  playAnimation(animationName: string): void {
    this.sendMessage({
      type: 'PLAY_ANIMATION',
      animation: animationName
    });
  }

  /**
   * Send facial expression command
   * @param expressionName Expression name (e.g., 'smile', 'sad', 'angry', 'surprised')
   */
  setFacialExpression(expressionName: string): void {
    this.sendMessage({
      type: 'SET_EXPRESSION',
      expression: expressionName
    });
  }

  /**
   * Send audio + lipsync to character
   * @param audioBase64 Base64 encoded audio (from TTS)
   * @param lipsyncData Lip-sync data (from r3f backend or generated)
   * @param animation Animation to play while audio plays
   */
  playAudioWithLipsync(audioBase64: string, lipsyncData: any, animation: string = 'Talking_1'): void {
    this.sendMessage({
      type: 'PLAY_AUDIO',
      audio: audioBase64,
      lipsync: lipsyncData,
      animation: animation
    });
  }

  /**
   * Send full character response with emotion, animation, expression, and audio
   * @param response Full character response from CharacterIntegrationController
   */
  playFullResponse(response: any): void {
    this.sendMessage({
      type: 'FULL_RESPONSE',
      data: {
        animation: response.character?.animation || 'Talking_1',
        expression: response.character?.facialExpression || 'default',
        audio: response.audio, // base64 if available
        lipsync: response.lipsync, // lip-sync data if available
        text: response.mainContent
      }
    });
  }

  /**
   * Zoom camera in/out
   */
  setCameraZoom(zoomed: boolean): void {
    this.sendMessage({
      type: 'SET_CAMERA_ZOOM',
      zoomed: zoomed
    });
  }

  /**
   * Reset character to idle state
   */
  resetCharacter(): void {
    this.sendMessage({
      type: 'RESET',
      animation: 'Idle',
      expression: 'default'
    });
  }

  /**
   * Internal: Send message to iframe
   */
  sendMessage(message: any): void {
    if (!this.iframeRef || !this.isReady.value) {
      this.messageQueue.push(message);
      return;
    }

    if (this.iframeRef.contentWindow) {
      this.iframeRef.contentWindow.postMessage(message, this.r3fOrigin);
    }
  }

  private sendPing(): void {
    if (this.iframeRef?.contentWindow) {
      this.iframeRef.contentWindow.postMessage({ type: 'PING' }, this.r3fOrigin);
    }
  }

  private clearPingTimer(): void {
    if (this.pingTimer !== null) {
      window.clearInterval(this.pingTimer);
      this.pingTimer = null;
    }
  }

  /**
   * Handle messages from r3f iframe
   */
  private handleMessage(event: MessageEvent): void {
    // Security check
    if (!event.origin.startsWith('http://localhost')) {
      return;
    }

    const { type, data } = event.data;

    switch (type) {
      case 'PONG':
        // iframe is ready, flush queued messages
        this.isReady.next(true);
        this.clearPingTimer();
        this.messageQueue.forEach(msg => this.sendMessage(msg));
        this.messageQueue = [];
        break;

      case 'AUDIO_ENDED':
        // Audio playback finished
        console.log('Character audio playback ended');
        break;

      case 'ANIMATION_FINISHED':
        // Animation finished
        console.log('Animation finished:', data.animation);
        break;

      case 'ERROR':
        console.error('r3f character error:', data.message);
        break;
    }
  }

  /**
   * Destroy the bridge (cleanup)
   */
  destroy(): void {
    this.iframeRef = null;
    this.isReady.next(false);
    this.clearPingTimer();
    this.messageQueue = [];
  }
}
