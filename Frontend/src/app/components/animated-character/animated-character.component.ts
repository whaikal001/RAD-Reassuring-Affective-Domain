import { Component, Input, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { trigger, state, style, animate, transition } from '@angular/animations';

@Component({
  selector: 'app-animated-character',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './animated-character.component.html',
  styleUrl: './animated-character.component.scss',
  animations: [
    trigger('emotionChange', [
      state('neutral', style({ transform: 'scale(1) rotate(0deg)', opacity: 1 })),
      state('happy', style({ transform: 'scale(1.05) rotate(2deg)', opacity: 1 })),
      state('sad-low', style({ transform: 'scale(0.95) rotate(-2deg)', opacity: 0.9 })),
      state('sad-high', style({ transform: 'scale(0.9) rotate(-3deg)', opacity: 0.85 })),
      state('encouraging', style({ transform: 'scale(1.1) rotate(0deg)', opacity: 1 })),
      state('goodjob', style({ transform: 'scale(1.15) rotate(2deg)', opacity: 1 })),
      state('listening', style({ transform: 'scale(1) rotate(-3deg)', opacity: 1 })),
      state('welcoming', style({ transform: 'scale(1.08) rotate(1deg)', opacity: 1 })),
      transition('* => *', animate('300ms ease-in-out'))
    ]),
    trigger('pulse', [
      state('pulse', style({ transform: 'scale(1)' })),
      transition('void => pulse', [
        style({ transform: 'scale(1)' }),
        animate('300ms ease-out', style({ transform: 'scale(1.08)' })),
        animate('300ms ease-in', style({ transform: 'scale(1)' }))
      ])
    ])
  ]
})
export class AnimatedCharacterComponent {
  @Input() emotion: string | null = 'neutral';
  @Input() intensity: number = 5;
  @Input() messageCount: number = 0;

  currentState = signal('neutral');
  currentImagePath = signal('/assets/characters/RadNeutral.png');
  isPulsing = signal(false);
  lastEmotion = signal<string | null>(null);
  lastIntensity = signal<number>(5);
  lastMessageCount = signal<number>(0);

  // Emotion to character expression mapping
  emotionMap: { [key: string]: { low: string; high: string; image: { low: string; high: string } } } = {
    'HAPPY': { low: 'happy', high: 'goodjob', image: { low: '/assets/characters/RadSmiling.png', high: '/assets/characters/RadGoodJob.png' } },
    'JOYFUL': { low: 'happy', high: 'goodjob', image: { low: '/assets/characters/RadSmiling.png', high: '/assets/characters/RadGoodJob.png' } },
    'SAD': { low: 'sad-low', high: 'sad-high', image: { low: '/assets/characters/RadSadLow.png', high: '/assets/characters/RadSadHigh.png' } },
    'DEPRESSED': { low: 'sad-low', high: 'sad-high', image: { low: '/assets/characters/RadSadLow.png', high: '/assets/characters/RadSadHigh.png' } },
    'ANXIOUS': { low: 'listening', high: 'sad-high', image: { low: '/assets/characters/RadThinking.png', high: '/assets/characters/RadSadHigh.png' } },
    'STRESSED': { low: 'listening', high: 'sad-high', image: { low: '/assets/characters/RadThinking.png', high: '/assets/characters/RadSadHigh.png' } },
    'ENCOURAGING': { low: 'welcoming', high: 'goodjob', image: { low: '/assets/characters/RadWelcoming.png', high: '/assets/characters/RadGoodJob.png' } },
    'PROUD': { low: 'encouraging', high: 'goodjob', image: { low: '/assets/characters/RadEncouraging.png', high: '/assets/characters/RadGoodJob.png' } },
    'SUPPORTIVE': { low: 'welcoming', high: 'encouraging', image: { low: '/assets/characters/RadWelcoming.png', high: '/assets/characters/RadEncouraging.png' } },
    'EMPATHETIC': { low: 'listening', high: 'welcoming', image: { low: '/assets/characters/RadThinking.png', high: '/assets/characters/RadWelcoming.png' } },
    'SYMPATHETIC': { low: 'listening', high: 'welcoming', image: { low: '/assets/characters/RadThinking.png', high: '/assets/characters/RadWelcoming.png' } },
    'NEUTRAL': { low: 'listening', high: 'listening', image: { low: '/assets/characters/RadNeutral.png', high: '/assets/characters/RadThinking.png' } },
    'UNDERSTANDING': { low: 'listening', high: 'welcoming', image: { low: '/assets/characters/RadThinking.png', high: '/assets/characters/RadWelcoming.png' } },
    'CONGRATULATORY': { low: 'goodjob', high: 'goodjob', image: { low: '/assets/characters/RadGoodJob.png', high: '/assets/characters/RadGoodJob.png' } }
  };

  constructor() {
    // Effect to watch emotion and intensity changes
    effect(() => {
      const newEmotion = this.emotion;
      const newIntensity = this.intensity;
      const lastEmotion = this.lastEmotion();
      const lastIntensity = this.lastIntensity();
      
      if (newEmotion !== lastEmotion || newIntensity !== lastIntensity) {
        this.triggerEmotionChange(newEmotion, newIntensity);
        this.lastEmotion.set(newEmotion);
        this.lastIntensity.set(newIntensity);
      }
    });

    // Effect to trigger pulse on message count change
    effect(() => {
      const count = this.messageCount;
      const lastCount = this.lastMessageCount();
      if (count > lastCount && count % 3 === 0) {
        this.triggerPulse();
      }
      this.lastMessageCount.set(count);
    });
  }

  triggerEmotionChange(emotion: string | null, intensity: number) {
    if (!emotion) {
      this.currentState.set('neutral');
      this.currentImagePath.set('/assets/characters/RadNeutral.png');
      return;
    }

    const emotionData = this.emotionMap[emotion];
    if (!emotionData) {
      this.currentState.set('neutral');
      this.currentImagePath.set('/assets/characters/RadNeutral.png');
      return;
    }

    // Use intensity to determine low/high expression
    // Intensity 1-4: Use low expression
    // Intensity 5-10: Use high expression
    const isHighIntensity = intensity >= 5;
    
    const state = isHighIntensity ? emotionData.high : emotionData.low;
    const imagePath = isHighIntensity ? emotionData.image.high : emotionData.image.low;

    this.currentState.set(state);
    this.currentImagePath.set(imagePath);

    // Trigger pulse on emotional changes
    if (emotion !== 'NEUTRAL') {
      this.triggerPulse();
    }
  }

  triggerPulse() {
    this.isPulsing.set(true);
    setTimeout(() => this.isPulsing.set(false), 700);
  }

  getEmotionLabel(): string {
    const emotion = this.emotion;
    const intensity = this.intensity;
    
    if (!emotion) return 'Neutral';
    
    const intensityMarker = intensity >= 5 ? '⚠️' : '✨';
    
    const labelMap: { [key: string]: string } = {
      'HAPPY': '😊 Happy',
      'JOYFUL': '😄 Joyful',
      'SAD': '😢 Sad',
      'DEPRESSED': '😔 Depressed',
      'ANXIOUS': '😰 Anxious',
      'STRESSED': '😤 Stressed',
      'ENCOURAGING': '👍 Encouraging',
      'PROUD': '🎉 Proud',
      'SUPPORTIVE': '💪 Supporting',
      'EMPATHETIC': '👂 Empathetic',
      'SYMPATHETIC': '🤝 Sympathetic',
      'NEUTRAL': '😐 Neutral',
      'UNDERSTANDING': '👂 Understanding',
      'CONGRATULATORY': '🎊 Celebrating'
    };
    
    const label = labelMap[emotion] || emotion;
    return `${label} (Intensity: ${intensity}) ${intensityMarker}`;
  }
}
