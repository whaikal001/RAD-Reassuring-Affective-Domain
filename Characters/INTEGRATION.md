# Full 3D Character Integration with R3F

This guide integrates the **full 3D animated character** from `r3f-virtual-girlfriend-frontend` into your Angular RAD chatbot.

## Architecture

```
┌─────────────────────────────────────────┐
│   Angular Chat (Port 4200)              │
│  ┌─────────────────────────────────────┐│
│  │ Chat Messages (75%)                 ││
│  └─────────────────────────────────────┘│
│  ┌─────────────────────────────────────┐│
│  │ R3F Character Panel (25%)           ││
│  │  ┌─────────────────────────────────┐││
│  │  │ Iframe: r3f App (Port 5173)    │││
│  │  │ ✓ 3D Avatar                     │││
│  │  │ ✓ Facial expressions            │││
│  │  │ ✓ Animations (Talking, Crying)  │││
│  │  │ ✓ Lip-sync                      │││
│  │  │ ✓ Audio playback                │││
│  │  └─────────────────────────────────┘││
│  └─────────────────────────────────────┘│
│                                         │
│  Communication: postMessage API         │
└─────────────────────────────────────────┘
```

## Setup Instructions

### 1. Build & Run r3f React App (Port 5173)

```bash
# Navigate to r3f frontend
cd Characters/r3f-virtual-girlfriend-frontend-main

# Install dependencies
npm install

# Start dev server (runs on http://localhost:5173)
npm run dev
```

### 2. Update r3f App to Accept Angular Commands

The r3f app has been updated with:
- **New hook**: `useAngularBridge.js` - Listens for messages from Angular
- **Avatar integration**: Avatar component now accepts animation/expression commands via postMessage
- **No changes to models**: Existing 3D model and animations work as-is

### 3. Run Angular App (Port 4200)

```bash
# In Frontend folder
cd Frontend

# Start Angular dev server
ng serve
```

### 4. Integration Features

**Automatic Integration**:
- Angular chat component now renders `<app-r3f-character-panel>` on the right (25%)
- `R3fCharacterBridgeService` handles all iframe communication
- When chat response comes back, character automatically plays:
  - ✓ Animation (Talking_1, Laughing, Crying, etc.)
  - ✓ Facial expression (smile, sad, angry, etc.)
  - ✓ Audio (from your TTS API)
  - ✓ Lip-sync (if available)

## Message Protocol

**Angular → r3f Iframe**

```javascript
// Play animation
{ type: 'PLAY_ANIMATION', animation: 'Talking_1' }

// Set facial expression
{ type: 'SET_EXPRESSION', expression: 'smile' }

// Play audio with lip-sync
{ 
  type: 'PLAY_AUDIO', 
  audio: 'base64...', 
  lipsync: {...}, 
  animation: 'Talking_1' 
}

// Full response (emotion + animation + audio)
{ 
  type: 'FULL_RESPONSE', 
  data: {
    animation: 'Talking_2',
    expression: 'happy',
    audio: 'base64...',
    lipsync: {...},
    text: 'Hello there!'
  }
}

// Reset character
{ type: 'RESET', animation: 'Idle', expression: 'default' }

// Ping/Pong (handshake)
{ type: 'PING' } / { type: 'PONG' }
```

**r3f → Angular Iframe**

```javascript
// Audio finished playing
{ type: 'AUDIO_ENDED' }

// Animation finished
{ type: 'ANIMATION_FINISHED', animation: 'Talking_1' }

// Error
{ type: 'ERROR', message: 'Animation not found' }
```

## Facial Expressions Available

Mapped from your chat emotion to character expressions:

| Emotion | Expression | Look |
|---------|------------|------|
| happy, joyful | smile | Smiling with closed eyes |
| sad, depressed | sad | Sad/crying expression |
| angry | angry | Angry/scowling |
| surprised | surprised | Wide eyes, open mouth |
| anxious, afraid | funnyFace, terrified | Various comical/scared faces |
| calm | default | Neutral/resting |
| neutral | default | Normal face |

## Animations Available

From `animations.glb`:

| Animation | Usage |
|-----------|-------|
| Idle | Default, waiting for input |
| Talking_0 | General talking |
| Talking_1 | Engaged conversation |
| Talking_2 | Enthusiastic talking |
| Laughing | Laughing response |
| Crying | Sad/crying response |
| Angry | Angry response |
| Terrified | Scared response |
| Rumba | Celebratory/happy |

## Customization

### Emotion to Expression Mapping

Edit in `Avatar.jsx`:
```javascript
const facialExpressions = {
  smile: { /* morphTarget values */ },
  sad: { /* morphTarget values */ },
  // ... add custom expressions here
};
```

### Animation Transitions

Edit in `Avatar.jsx`:
```javascript
// Customize fade-in/fade-out speeds
actions[animation]
  .reset()
  .fadeIn(0.5)  // Change this value
  .play();
```

## Troubleshooting

**Iframe Not Loading**
- Check if `http://localhost:5173` is running
- Check browser console for CORS errors
- Security: Iframe uses `sandbox="allow-same-origin allow-scripts"`

**Animation Not Playing**
- Verify animation name matches `animations.glb` clip names
- Check browser console for errors from r3f app
- Try with simpler animation: `Idle`

**Lip-sync Not Working**
- Ensure lipsync data format matches expected structure
- Check if audio base64 is valid MP3

**Character Expressions Not Changing**
- Verify expression name is in `facialExpressions` object
- Check morphTarget values are valid (0-1 range)

## Performance Optimization

- r3f runs in separate iframe ≠ impacts main chat performance
- Three.js rendering is offloaded to separate process
- postMessage API is optimized for batching commands

## Files Added/Modified

**Angular:**
- `services/r3f-character-bridge.service.ts` - Iframe communication
- `components/r3f-character-panel/` - Character display component
- `services/character.service.ts` - Enhanced with response emission
- `pages/chat/chat.component.ts` - Updated to use R3FCharacterPanel
- `pages/chat/chat.component.html` - Replaced AnimatedCharacter with R3FPanel
- `pages/chat/chat.component.scss` - 75-25 layout (already done)

**R3F React:**
- `hooks/useAngularBridge.js` - NEW: Listen to Angular messages
- `components/Avatar.jsx` - Updated to accept Angular commands
- `hooks/useChat.js` - No changes (kept for standalone usage)

## Next Steps

1. ✅ Set up r3f app on port 5173
2. ✅ Run Angular on port 4200
3. Open chat and send a message
4. Watch the 3D character animate in real-time!
5. Optional: Customize expressions/animations in Avatar.jsx
6. Optional: Add gesture recognition or voice control

Enjoy your immersive mental health chatbot! 🎭✨