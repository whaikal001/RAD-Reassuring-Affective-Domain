# HuggingFace & XTTS v2 Model Configuration Guide

This document explains the AI models used in the RAD (Reassuring Affective Domain) project for chat generation and text-to-speech capabilities.

## Overview

The character backend now uses:
- **Chat Generation**: HuggingFace Inference API with LLaMA 2 70B
- **Text-to-Speech (TTS)**: XTTS v2 (Coqui) with multilingual support for English and Malay

## Chat Generation Model

### Selected Model: `meta-llama/Llama-3.1-8B-Instruct`

**Why This Model?**
- **Quality**: LLaMA 2 70B is one of the best open-source chat models available
- **Conversational**: Specifically fine-tuned for chat interactions
- **Safety**: Includes safety guardrails and alignment training
- **Performance**: Excellent for generating natural, contextual responses

**Alternative Models** (if quota/latency issues):
- `mistralai/Mistral-7B-Instruct-v0.1` - Smaller, faster, good quality
- `HuggingFaceH4/zephyr-7b-beta` - Smaller, optimized for instruction-following

**Configuration:**
```env
HF_CHAT_MODEL=meta-llama/Llama-3.1-8B-Instruct
```

### How Chat Works:
1. User sends a message to `/api/character/enrich` endpoint
2. System generates emotion-aware response using LLaMA 2
3. Response includes:
   - `text`: The response message
   - `facialExpression`: emotion mapping (smile, sad, angry, surprised, funnyFace, default)
   - `animation`: character action (Talking_0-2, Crying, Laughing, Rumba, etc.)

## Text-to-Speech: XTTS v2 (Coqui)

### Why XTTS v2?
- **Multilingual**: Supports 28+ languages including English and Malay
- **High Quality**: Natural, expressive speech synthesis
- **Fast**: Efficient inference suitable for real-time applications
- **No External APIs**: Runs locally, no API keys needed after model download
- **Cross-lingual**: Speaker embedding-based approach

### Supported Languages:
- **English**: `en`
- **Malay**: `ms`
- Plus 26+ other languages

### Technical Details:
- **Architecture**: Non-autoregressive flow-based model
- **Model Size**: ~2B parameters
- **Inference Speed**: ~1-3 seconds per sentence
- **Quality**: Professional-grade natural speech

### Configuration:
```env
USE_XTTS=true
```

## API Structure (Matches RAD AI Backend)

The character backend now implements the same API as the Spring Boot backend:

### POST `/api/character/enrich?language=en`

**Request Body**:
```json
{
  "conversationId": "conv-123",
  "mainContent": "That sounds wonderful! I'm so happy for you.",
  "fullResponse": "Full response text...",
  "emotion": "happy"
}
```

**Response**:
```json
{
  "conversationId": "conv-123",
  "mainContent": "That sounds wonderful! I'm so happy for you.",
  "fullResponse": "Full response text...",
  "emotion": "happy",
  "character": {
    "facialExpression": "smile",
    "animation": "Laughing",
    "audioLanguage": "en"
  },
  "audio": {
    "base64": "base64_encoded_wav",
    "lipsync": {
      "mouthCues": [...]
    },
    "format": "wav",
    "language": "en"
  }
}
```

### Emotion to Expression Mapping:
| Emotion | Expression |
|---------|-----------|
| happy | smile |
| sad | sad |
| angry | angry |
| surprised | surprised |
| anxious | funnyFace |
| calm | smile |
| neutral | default |
| afraid | terrified |

### Sentiment to Animation Mapping:
| Sentiment | Animation |
|-----------|----------|
| positive | Laughing |
| negative | Crying |
| neutral | Talking_1 |
| questioning | Talking_0 |
| encouraging | Talking_2 |
| concerned | Idle |

## API Endpoints

### `/api/character/enrich` - Enrich Response with Character Metadata
**POST** with query parameter `language`:
```bash
curl -X POST http://localhost:3001/api/character/enrich?language=en \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "conv-123",
    "mainContent": "Hello!",
    "emotion": "happy"
  }'
```

### `/chat` - Legacy Chat Endpoint (still supported)
**POST** with JSON body:
```bash
curl -X POST http://localhost:3001/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"Hello!","language":"en"}'
```

### `/models` - Get Current Model Configuration
**GET** response:
```json
{
  "chatModel": "meta-llama/Llama-3.1-8B-Instruct",
  "ttsProvider": "XTTS v2 (Coqui)",
  "supportedLanguages": ["en", "ms"]
}
```

## Setup & Deployment

### 1. Get HuggingFace API Key:
- Sign up at [huggingface.co](https://huggingface.co)
- Settings → Access Tokens → Create new **User Access Token**
- Copy token to `HUGGINGFACE_API_KEY` in `.env`

### 2. Create `.env` file:
```bash
cp .env.example .env
# Edit .env with your API key
```

### 3. Deploy with Docker:
```bash
docker compose up --build -d
```

The Docker image automatically downloads the XTTS v2 model on first startup (takes ~2-5 minutes).

### 4. Test the Service:
```bash
# Test chat endpoint
curl -X POST http://localhost:3001/api/character/enrich?language=en \
  -H "Content-Type: application/json" \
  -d '{
    "mainContent": "How are you today?",
    "emotion": "happy"
  }'

# Test models endpoint
curl http://localhost:3001/models
```

## Performance Characteristics

| Model | Type | Speed | Quality | Parameters | Language |
|-------|------|-------|---------|-----------|----------|
| LLaMA 2 70B | Chat | Slower | Excellent | 70B | English |
| XTTS v2 | TTS | Fast | Excellent | 2B | 28+ langs |

## Cost Considerations

- **HuggingFace Inference**: Free tier available (~30,000 requests/month)
- **XTTS v2**: No API costs - runs locally in Docker
- **Total**: Mostly free (only HF token usage counted)

## Future Enhancements

1. **Model Fine-tuning**: Fine-tune LLaMA 2 with custom personality
2. **Voice Cloning**: Use XTTS v2 speaker embedding for custom voices
3. **Emotion Detection**: Real-time emotion detection from user input
4. **Multi-speaker**: Support multiple character voices

## References

- [HuggingFace Inference Docs](https://huggingface.co/docs/api-inference)
- [Llama 3.1 8B Instruct Model Card](https://huggingface.co/meta-llama/Llama-3.1-8B-Instruct)
- [XTTS v2 GitHub](https://github.com/coqui-ai/TTS)
- [Coqui TTS Docs](https://docs.coqui.ai/en/)
