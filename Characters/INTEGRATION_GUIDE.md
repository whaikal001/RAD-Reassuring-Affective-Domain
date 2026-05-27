# Character Backend Integration Guide

This document explains how the r3f Character Backend integrates with the RAD AI Spring Boot Backend.

## Architecture Overview

```
RAD AI Spring Boot Backend (Port 8080)
        ↓
    Chat Service
        ↓
    Character Integration Controller
        ↓ (POST /api/character/enrich)
Character Backend (Port 3001)
        ↓
    LLaMA 2 (HuggingFace)
    XTTS v2 (Coqui TTS)
        ↓
    Audio + Character Metadata
        ↓ (Response)
Frontend (Port 3000)
        ↓
    3D Character Animation
```

## API Integration

### 1. Spring Boot calls Character Backend

**POST** `http://localhost:3001/api/character/enrich?language=en`

**Request** (from Spring Boot):
```json
{
  "conversationId": "conv-uuid",
  "mainContent": "That sounds amazing! I'm so excited!",
  "fullResponse": "Full response text...",
  "emotion": "happy"
}
```

**Response** (from Character Backend):
```json
{
  "conversationId": "conv-uuid",
  "mainContent": "That sounds amazing! I'm so excited!",
  "fullResponse": "Full response text...",
  "emotion": "happy",
  "character": {
    "facialExpression": "smile",
    "animation": "Laughing",
    "audioLanguage": "en"
  },
  "audio": {
    "base64": "base64_encoded_wav...",
    "lipsync": {
      "mouthCues": [...]
    },
    "format": "wav",
    "language": "en"
  }
}
```

### 2. Spring Boot returns enriched response to Frontend

The Frontend receives the enriched response with:
- Text content
- Character animation metadata
- Audio base64 + lipsync data
- All needed for 3D character rendering

### 3. Frontend renders character with audio

- Displays text in chat
- Plays audio while animating character
- Uses lipsync data for lip-sync animation

## Configuration

### Environment Variables

**Character Backend** (`.env`):
```env
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx
HF_CHAT_MODEL=meta-llama/Llama-3.1-8B-Instruct
USE_XTTS=true
```

### Docker Compose

All services work together:
```bash
docker compose up --build -d
```

This starts:
1. PostgreSQL (database)
2. Spring Boot Backend (port 8080)
3. Angular Frontend (port 3000)
4. Character Backend (port 3001)
5. Character Frontend React app (port 5173)

## Supported Features

### Languages
- **English** (`en`)
- **Malay** (`ms`)

### Emotions (mapped to facial expressions)
```
happy → smile
sad → sad
angry → angry
surprised → surprised
anxious → funnyFace
calm → smile
neutral → default
afraid → terrified
```

### Sentiments (mapped to animations)
```
positive → Laughing
negative → Crying
neutral → Talking_1
questioning → Talking_0
encouraging → Talking_2
concerned → Idle
```

## Testing

### 1. Test Character Backend Directly

```bash
curl -X POST http://localhost:3001/api/character/enrich?language=en \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "test-1",
    "mainContent": "I am so happy to see you!",
    "emotion": "happy"
  }'
```

### 2. Test via Frontend

1. Open http://localhost:3000
2. Send a message in the chat
3. Character should:
   - Display animation
   - Play audio
   - Show facial expression

### 3. Check Models

```bash
curl http://localhost:3001/models
```

Response:
```json
{
  "chatModel": "meta-llama/Llama-3.1-8B-Instruct",
  "ttsProvider": "XTTS v2 (Coqui)",
  "supportedLanguages": ["en", "ms"]
}
```

## Troubleshooting

### Issue: "HUGGINGFACE_API_KEY not set"

**Solution**: Make sure `.env` file has:
```env
HUGGINGFACE_API_KEY=your_actual_key
```

### Issue: Character Backend takes time to start

**Reason**: First startup downloads XTTS v2 model (~2-5 minutes)

**Check logs**:
```bash
docker logs radai-character-backend
```

### Issue: Audio not generating

**Check**:
1. API key is valid
2. Port 3001 is accessible
3. audios/ directory has write permissions

### Issue: Malay TTS not working properly

**Note**: XTTS v2 supports Malay (`ms`), but quality may vary. To improve:
1. Use Malay-specific speaker voice
2. Fine-tune on Malay dataset (future enhancement)

## Local Development (without Docker)

### Prerequisites
- Node.js 20+
- Python 3.10+
- FFmpeg

### Setup

1. **Install Node dependencies**:
```bash
cd Characters/r3f-virtual-girlfriend-backend-main
npm install
```

2. **Install Python dependencies**:
```bash
pip install -r requirements.txt
```

3. **Create `.env` file**:
```env
HUGGINGFACE_API_KEY=hf_xxxxxxxxxxxxx
HF_CHAT_MODEL=meta-llama/Llama-3.1-8B-Instruct
USE_XTTS=true
```

4. **Download XTTS model** (first run):
```bash
python3 -c "from TTS.api import TTS; TTS(model_name='tts_models/multilingual/multi-dataset/xtts_v2', gpu=False)"
```

5. **Start server**:
```bash
npm run start
```

Server runs on `http://localhost:3000`

## Performance Notes

- **Chat Generation**: ~2-5 seconds (depends on HuggingFace queue)
- **TTS Generation**: ~2-3 seconds per sentence
- **Total**: ~4-8 seconds per character response

For faster inference, consider:
- Local GPU deployment
- Quantized models (smaller, faster)
- Model caching/pre-loading

## References

- [RAD AI Backend Docs](../Backend/README.md)
- [HuggingFace Models](HUGGINGFACE_MODELS_GUIDE.md)
- [XTTS v2 Documentation](https://docs.coqui.ai/)
- [Docker Setup](../docker-compose.yml)
