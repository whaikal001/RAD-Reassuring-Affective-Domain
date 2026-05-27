# ===== Node.js with FFmpeg =====
FROM node:20-slim

WORKDIR /app

# Install FFmpeg for audio/video processing, espeak-ng for local TTS, and curl for rhubarb
RUN apt-get update && apt-get install -y \
    ffmpeg \
    espeak-ng \
    curl \
    --no-install-recommends \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/bin

# Download rhubarb for lipsync generation
RUN curl -L https://github.com/DanielSWolf/rhubarb-lip-sync/releases/download/v1.13.0/rhubarb-lip-sync-1.13.0-Linux-x86_64.tar.gz \
    -o /tmp/rhubarb.tar.gz && \
    tar -xzf /tmp/rhubarb.tar.gz -C /app/bin && \
    chmod +x /app/bin/rhubarb && \
    rm /tmp/rhubarb.tar.gz || true

# Copy package files
COPY package.json yarn.lock ./

# Install Node dependencies using npm
RUN npm install --omit=dev

# Copy application files
COPY . .

# Create audios directory
RUN mkdir -p audios

# Expose port
EXPOSE 3000

# Start application
CMD ["node", "index.js"]
