# Character Expression Mapping - RadAI

## Your Character Assets

You have 8 character expressions already downloaded and ready to use:

```
assets/characters/
├── RadNeutral.png         ← Default listening state
├── RadSmiling.png         ← Happy emotions
├── RadThinking.png        ← Processing/understanding
├── RadWelcoming.png       ← Welcoming/supportive (lower intensity)
├── RadGoodJob.png         ← Celebrating/proud moments
├── RadSadLow.png          ← Sad (low-medium intensity: 1-4)
├── RadSadHigh.png         ← Sad (high intensity: 5-10)
└── RadEncouraging.png     ← Encouraging/supportive (higher intensity)
```

---

## Emotion to Expression Mapping

The system automatically maps backend emotions and stress levels to the correct character expression:

### 😊 **HAPPY Emotions**
| Backend Emotion | Intensity 1-4 | Intensity 5-10 |
|---|---|---|
| `HAPPY` | RadSmiling.png | RadGoodJob.png |
| `JOYFUL` | RadSmiling.png | RadGoodJob.png |

**When to use:**
- User expresses positive feelings
- Successful achievements
- Happy memories or accomplishments

---

### 😢 **SAD Emotions**
| Backend Emotion | Intensity 1-4 | Intensity 5-10 |
|---|---|---|
| `SAD` | RadSadLow.png | RadSadHigh.png |
| `DEPRESSED` | RadSadLow.png | RadSadHigh.png |

**When to use:**
- User expresses sadness or depression
- Low intensity (1-4): Mild sadness → RadSadLow.png (gentler look)
- High intensity (5-10): Deep sadness/depression → RadSadHigh.png (more concerned)

---

### 😰 **ANXIETY/STRESS Emotions**
| Backend Emotion | Intensity 1-4 | Intensity 5-10 |
|---|---|---|
| `ANXIOUS` | RadThinking.png | RadSadHigh.png |
| `STRESSED` | RadThinking.png | RadSadHigh.png |

**When to use:**
- User expresses anxiety or stress
- Low intensity: Listening/understanding → RadThinking.png
- High intensity: Deep concern → RadSadHigh.png

---

### 👍 **ENCOURAGING/PROUD Emotions**
| Backend Emotion | Intensity 1-4 | Intensity 5-10 |
|---|---|---|
| `ENCOURAGING` | RadWelcoming.png | RadGoodJob.png |
| `PROUD` | RadEncouraging.png | RadGoodJob.png |
| `SUPPORTIVE` | RadWelcoming.png | RadEncouraging.png |
| `CONGRATULATORY` | RadGoodJob.png | RadGoodJob.png |

**When to use:**
- Bot gives encouragement or praise
- User makes progress
- Celebration of achievements

---

### 👂 **LISTENING/EMPATHETIC Emotions**
| Backend Emotion | Intensity 1-4 | Intensity 5-10 |
|---|---|---|
| `EMPATHETIC` | RadThinking.png | RadWelcoming.png |
| `SYMPATHETIC` | RadThinking.png | RadWelcoming.png |
| `UNDERSTANDING` | RadThinking.png | RadWelcoming.png |

**When to use:**
- Bot is actively listening
- Showing empathy for user's situation
- Processing user's emotional state

---

### 😐 **NEUTRAL/DEFAULT**
| Backend Emotion | Intensity 1-4 | Intensity 5-10 |
|---|---|---|
| `NEUTRAL` | RadNeutral.png | RadThinking.png |

**When to use:**
- Initial greeting
- No strong emotional context
- General conversation

---

## Intensity Levels Explained

The **intensity** field (1-10) determines how strong the emotion is:

### Low Intensity (1-4) 🟢
- Mild feelings
- Casual conversation
- General supportiveness
- **Character shows:** Gentle, calm expression
- **Example:** User says "I'm a bit tired" → Intensity 2 → RadSadLow.png

### High Intensity (5-10) 🔴
- Strong feelings
- Serious concerns
- Deep emotions
- **Character shows:** More expressive, concerned look
- **Example:** User says "I'm deeply depressed" → Intensity 8 → RadSadHigh.png

---

## Visual Effects Applied

Based on the emotion state, additional CSS effects are applied:

| Expression | CSS Filter | Animation | Purpose |
|---|---|---|---|
| **RadSmiling.png** | Green glow, +10% brightness | Bounce up | Happiness/positivity |
| **RadGoodJob.png** | Orange glow, +15% brightness | Celebration | Pride/celebration |
| **RadSadLow.png** | Red glow (light), -2% brightness | Gentle droop | Mild sadness |
| **RadSadHigh.png** | Red glow (strong), -8% brightness | Deep sadness | Deep depression |
| **RadThinking.png** | Purple glow, normal brightness | Tilt side-to-side | Listening/thinking |
| **RadWelcoming.png** | Purple glow, +8% brightness | Welcoming wave | Support/empathy |
| **RadEncouraging.png** | Purple glow, +10% brightness | Gentle lift | Encouragement |
| **RadNeutral.png** | Subtle glow, normal brightness | Static | Neutral state |

**Bonus:** Sparkles (✨) appear when celebrating (RadGoodJob.png or RadEncouraging.png)

---

## How It Works

### Step 1: Backend Sends Emotion + Intensity
```json
{
  "emotion": "SAD",
  "intensity": 7
}
```

### Step 2: Frontend Maps to Expression
- Emotion: `SAD` 
- Intensity: `7` (high, ≥ 5)
- **Result:** RadSadHigh.png with red glow + deep sadness animation

### Step 3: Character Reacts
- Image swaps to RadSadHigh.png
- CSS filter applies red glow
- Animation plays (gentle droop with scale down)
- Label shows: "😢 Sad (Intensity: 7) ⚠️"
- Sparkles don't appear (not a celebration emotion)

---

## Example Conversation Flow

```
User: "I'm so happy about my achievement!"
├─ Backend emotion: HAPPY
├─ Intensity: 8 (high)
├─ Character shows: RadGoodJob.png ✨✨✨
└─ Animation: Celebration bounce with sparkles

User: "I'm a bit sad but trying to cope"
├─ Backend emotion: SAD
├─ Intensity: 3 (low)
├─ Character shows: RadSadLow.png
└─ Animation: Gentle sadness

User: "I feel very overwhelmed and stressed"
├─ Backend emotion: STRESSED
├─ Intensity: 9 (high)
├─ Character shows: RadSadHigh.png
└─ Animation: Deep concern with scale down

User: "That's really encouraging, thank you"
├─ Backend emotion: ENCOURAGING
├─ Intensity: 6 (medium-high)
├─ Character shows: RadEncouraging.png ✨
└─ Animation: Gentle lift with sparkles
```

---

## Backend Integration

The system automatically uses the `intensity` field from your backend response:

**ChatbotFlowResponseDTO** fields used:
- `emotion` - String (HAPPY, SAD, ENCOURAGING, etc.)
- `intensity` - Integer (1-10) - your stress/severity level

**No changes needed!** Your backend already provides both fields.

---

## Emotion Label Display

The character shows an emotion label below it:

```
😢 Sad (Intensity: 7) ⚠️
   ↑ emotion icon
          ↑ backend emotion      ↑ current intensity level
                                       ↑ indicator (⚠️ high, ✨ low)
```

The indicator changes:
- **✨** for low-medium intensity (1-4)
- **⚠️** for high intensity (5-10)

---

## Testing the Character Reactions

After running `npm start`:

1. **Start a conversation** in the chat
2. **Send a happy message** → Character shows RadSmiling.png or RadGoodJob.png
3. **Express sadness** → RadSadLow.png or RadSadHigh.png depending on how sad
4. **Get encouragement** → RadWelcoming.png or RadGoodJob.png
5. **Notice intensity**: Higher stress = more expressive character

---

## Customization

### Adjust Intensity Threshold
Currently: 5 is the cutoff (1-4 = low, 5-10 = high)

To change in `animated-character.component.ts`:
```typescript
const isHighIntensity = intensity >= 5;  // Change 5 to your preferred cutoff
```

### Modify Emotion Mappings
In `animated-character.component.ts`, update the `emotionMap`:
```typescript
'YOUR_EMOTION': { 
  low: 'listening', 
  high: 'sad-high', 
  image: { 
    low: '/assets/characters/RadThinking.png', 
    high: '/assets/characters/RadSadHigh.png' 
  } 
}
```

### Change Visual Effects
Edit `animated-character.component.scss` to modify filters:
```scss
&.emotion-happy {
  filter: drop-shadow(0 8px 12px rgba(76, 175, 80, 0.3)) brightness(1.1);
  // Adjust brightness, saturation, or hue
}
```

---

## Summary

✅ **Your 8 character expressions are automatically mapped**
✅ **Intensity levels drive expression intensity**
✅ **No backend changes needed - already sending intensity**
✅ **Character reacts in real-time to emotions**
✅ **Visual effects enhance the emotional display**

Your character is ready! Just run the app and start chatting. 🎉
