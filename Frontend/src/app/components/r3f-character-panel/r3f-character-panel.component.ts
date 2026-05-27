import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { Subscription } from 'rxjs';
import { CharacterService } from '../../services/character.service';
import { R3fCharacterBridgeService } from '../../services/r3f-character-bridge.service';

/**
 * R3FCharacterPanelComponent
 * Renders the 3D character using r3f React app via iframe
 * Communicates with the r3f app using postMessage API
 */
@Component({
  selector: 'app-r3f-character-panel',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="r3f-character-container">
      <iframe
        #r3fIframe
        [src]="r3fUrl"
        class="r3f-iframe"
        (load)="onIframeLoad()"
        sandbox="allow-same-origin allow-scripts allow-popups allow-presentation"
        allow="accelerometer; ambient-light-sensor; autoplay; camera; encrypted-media; geolocation; gyroscope; magnetometer; microphone; payment; usb; vr; xr-spatial-tracking"
        loading="lazy"
      ></iframe>
      <div class="character-status" *ngIf="!iframeReady">
        <div class="spinner"></div>
        <p>Loading 3D Character...</p>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      width: 100%;
      height: 100%;
    }

    .r3f-character-container {
      position: relative;
      width: 100%;
      height: 100%;
      border-radius: 16px;
      overflow: hidden;
      background: linear-gradient(135deg, rgba(147, 100, 200, 0.1), rgba(100, 150, 255, 0.05));
      border: 1px solid rgba(147, 100, 200, 0.15);
    }

    .r3f-iframe {
      width: 100%;
      height: 100%;
      border: none;
      display: block;
    }

    .character-status {
      position: absolute;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: rgba(30, 15, 50, 0.9);
      backdrop-filter: blur(10px);
      gap: 1rem;
      z-index: 10;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 4px solid rgba(147, 100, 200, 0.2);
      border-top-color: rgba(147, 100, 200, 0.8);
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .character-status p {
      color: rgba(147, 100, 200, 0.8);
      font-size: 0.9rem;
      margin: 0;
    }
  `]
})
export class R3fCharacterPanelComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('r3fIframe') iframeRef!: ElementRef<HTMLIFrameElement>;

  r3fUrl!: SafeResourceUrl;
  iframeReady = false;
  private subscriptions = new Subscription();
  private pendingResponse: any | null = null;
  private handshakeTimer: number | null = null;

  constructor(
    private characterService: CharacterService,
    private r3fBridge: R3fCharacterBridgeService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.r3fUrl = this.sanitizer.bypassSecurityTrustResourceUrl('http://localhost:5173');

    // Subscribe to character responses
    const characterResponseSub = this.characterService.characterResponse$.subscribe((response) => {
      if (response && this.iframeReady) {
        this.r3fBridge.playFullResponse(response);
      } else if (response) {
        this.pendingResponse = response;
      }
    });

    this.subscriptions.add(characterResponseSub);
  }

  ngAfterViewInit(): void {
    if (this.iframeRef) {
      // Initialize the iframe bridge
      this.r3fBridge.initializeIframe(this.iframeRef.nativeElement);

      this.iframeRef.nativeElement.addEventListener('load', () => this.startHandshake());
      this.startHandshake();

      // Wait for iframe to be ready
      const readySub = this.r3fBridge.ready$.subscribe((ready) => {
        this.iframeReady = ready;

        if (ready && this.pendingResponse) {
          this.r3fBridge.playFullResponse(this.pendingResponse);
          this.pendingResponse = null;
        }
      });

      this.subscriptions.add(readySub);
    }
  }

  private startHandshake(): void {
    if (this.handshakeTimer !== null) {
      window.clearInterval(this.handshakeTimer);
      this.handshakeTimer = null;
    }

    const ping = () => {
      const iframe = this.iframeRef?.nativeElement;
      if (!iframe?.contentWindow || this.iframeReady) {
        return;
      }

      iframe.contentWindow.postMessage({ type: 'PING' }, 'http://localhost:5173');
    };

    ping();
    this.handshakeTimer = window.setInterval(ping, 1000);
  }

  onIframeLoad(): void {
    this.startHandshake();
  }

  ngOnDestroy(): void {
    if (this.handshakeTimer !== null) {
      window.clearInterval(this.handshakeTimer);
      this.handshakeTimer = null;
    }

    this.subscriptions.unsubscribe();
    this.r3fBridge.destroy();
  }
}
