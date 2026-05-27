# Deployment Guide - RAD AI with HuggingFace + XTTS v2

Complete deployment instructions for RAD AI using HuggingFace for chat and XTTS v2 for multilingual TTS.

## Prerequisites

- Docker & Docker Compose (latest)
- HuggingFace API Key (free)
- ~5GB disk space (for XTTS v2 model)
- 8GB+ RAM recommended

## Step 1: Get HuggingFace API Key

1. Visit [huggingface.co](https://huggingface.co)
2. Sign up or log in
3. Go to **Settings** → **Access Tokens**
4. Click **New token**
5. Copy your **User Access Token**

## Step 2: Setup Environment

1. **Clone/navigate to project**:
```bash
cd RAD-Reassuring-Affective-Domain
```

2. **Create `.env` file**:
```bash
cp .env.example .env
```

3. **Edit `.env` with your keys**:
```env
HUGGINGFACE_API_KEY=hf_your_actual_token_here
HF_CHAT_MODEL=meta-llama/Llama-3.1-8B-Instruct
USE_XTTS=true

# Database
POSTGRES_USER=radai_user
POSTGRES_PASSWORD=your_secure_password_here
POSTGRES_DB=radai
```

## Step 3: Build & Deploy

### Full Stack Deployment (Recommended)

```bash
# Build all images and start services
docker compose up --build -d

# Monitor startup
docker compose logs -f
```

**Services started**:
- PostgreSQL (database) - internal
- Spring Boot Backend - http://localhost:8080
- Angular Frontend - http://localhost:3000
- Character Backend - http://localhost:3001
- Character Frontend (R3F) - http://localhost:5173

### First Startup Notes

⏳ **Character Backend**: Takes 2-5 minutes on first run to download XTTS v2 model (~2GB)

Monitor progress:
```bash
docker logs -f radai-character-backend
```

You'll see:
```
Downloading https://models.coqui.ai/xtts_v2/... (100%)
Model loaded successfully
Server listening on port 3000
```

## Step 4: Verify Deployment

### Check all services running:
```bash
docker compose ps
```

Expected output:
```
NAME                      STATUS
radai-postgres            Up
radai-backend             Up
radai-frontend            Up
radai-character-backend   Up
radai-character-frontend  Up
```

### Test Character Backend:
```bash
curl -X POST http://localhost:3001/api/character/enrich?language=en \
  -H "Content-Type: application/json" \
  -d '{
    "mainContent": "Hello! I am excited to meet you!",
    "emotion": "happy"
  }'
```

Expected: Response with audio base64 and character metadata

### Test Full Stack:
1. Open http://localhost:3000 (Angular Frontend)
2. Send a chat message
3. Character should respond with:
   - ✅ Text display
   - ✅ 3D animation
   - ✅ Facial expression
   - ✅ Audio playback
   - ✅ Lip sync

## Configuration Options

### Change Chat Model

Update `.env`:
```env
HF_CHAT_MODEL=mistralai/Mistral-7B-Instruct-v0.1  # Faster, lighter
```

Rebuild:
```bash
docker compose up --build -d character-backend
```

### Change Language

Pass language parameter:
```bash
curl -X POST http://localhost:3001/api/character/enrich?language=ms ...
```

Supported: `en`, `ms` (Malay)

### Disable XTTS (use HF Inference TTS instead):

Update docker-compose.yml:
```yaml
environment:
  USE_XTTS: "false"
```

## Performance Tuning

### For Better Speed

1. **Use smaller chat model**:
```env
HF_CHAT_MODEL=mistralai/Mistral-7B-Instruct-v0.1
```

2. **Add GPU support** (if available):
   - Update Dockerfile to use nvidia base image
   - Uncomment GPU lines in docker-compose

3. **Enable caching**:
```bash
docker volume create xtts-cache
# Mount in docker-compose
```

### For Better Quality

XTTS v2 settings already optimized for quality. No changes needed.

## Troubleshooting

### Character Backend crashes on startup

**Error**: `Out of memory` or `Model download failed`

**Solution**:
```bash
# Check logs
docker logs radai-character-backend

# Increase Docker memory in Docker Desktop settings
# Or use swap space on Linux
```

### Slow response time

**Common causes**:
1. HuggingFace inference queue (free tier)
2. Network latency
3. Model not cached

**Solution**:
- Use smaller model (Mistral 7B)
- Wait for HF queue to clear
- Check bandwidth

### Audio not generating

**Check**:
```bash
# Verify backend is running
curl http://localhost:3001/models

# Check logs
docker logs radai-character-backend

# Verify write permissions
docker exec radai-character-backend ls -la /app/audios/
```

### "Connection refused" errors

**Solution**:
```bash
# Check if services are up
docker compose ps

# Restart failed service
docker compose restart character-backend

# View full logs
docker compose logs
```

## Management Commands

### View logs
```bash
# All services
docker compose logs

# Specific service
docker compose logs radai-character-backend

# Follow logs (real-time)
docker compose logs -f character-backend
```

### Restart services
```bash
# One service
docker compose restart character-backend

# All services
docker compose restart

# Rebuild and restart
docker compose up --build -d
```

### Stop all services
```bash
docker compose down
```

### Remove all data (clean slate)
```bash
docker compose down -v
```

## Production Deployment

For production (AWS, Azure, GCP, etc.):

1. **Update docker-compose** for production settings:
   - Remove volume mounts to ephemeral storage
   - Add health checks
   - Increase memory limits

2. **Security**:
   - Store API keys in secrets manager
   - Use environment-specific `.env` files
   - Enable HTTPS

3. **Scaling**:
   - Deploy on container orchestration (Kubernetes)
   - Use load balancer for multiple instances
   - Cache models in persistent volumes

4. **Monitoring**:
   - Add logging (ELK, CloudWatch, etc.)
   - Monitor performance metrics
   - Alert on failures

## Cost Considerations

### HuggingFace
- **Free Tier**: ~30,000 requests/month
- **Cost**: $0 - $9/month for higher tiers

### XTTS v2
- **Cost**: $0 (runs locally in Docker)
- **Storage**: ~2GB for model

### Total Monthly Cost
- **Free Tier**: ~$0-5/month
- **Production**: ~$20-50/month

## Support & Documentation

- [HuggingFace Models Guide](./Characters/HUGGINGFACE_MODELS_GUIDE.md)
- [Character Integration Guide](./Characters/INTEGRATION_GUIDE.md)
- [Main README](./README.md)

---

**Status**: ✅ Ready for deployment
**Last Updated**: May 2026
**Tested On**: Docker Desktop 4.x, Docker Compose 2.x
