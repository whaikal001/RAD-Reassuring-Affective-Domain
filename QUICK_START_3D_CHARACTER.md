# RAD 3D Character Integration - Quick Start Guide

## Prerequisites
- Node.js 16+ installed
- Angular CLI installed (`npm install -g @angular/cli`)
- Docker running (for Java backend)

## Running Everything

### Option 1: Manual Terminal Windows (Easiest)

**Terminal 1 - Java Backend (already running)**
```bash
# If not running, start docker-compose
cd RAD-Reassuring-Affective-Domain
docker compose up --build -d

# Or if running locally:
cd Backend
mvn spring-boot:run
```
✅ Running on `http://localhost:8080`

**Terminal 2 - R3F 3D Character App**
```bash
cd Characters/r3f-virtual-girlfriend-frontend-main
npm install  # First time only
npm run dev
```
✅ Running on `http://localhost:5173`

**Terminal 3 - Angular Chat App**
```bash
cd Frontend
npm install  # First time only
ng serve
```
✅ Running on `http://localhost:4200`

### Open Browser
```
http://localhost:4200
```

## Expected Behavior

1. **Angular Chat loads** - You see the chat interface with 75% messages + 25% character
2. **Character panel loads** - Loading spinner appears (takes ~5 sec for Three.js)
3. **Send a message** - Your chatbot responds
4. **Character animates** - The 3D avatar:
   - Plays animation (Talking_1, Laughing, etc.)
   - Shows facial expression (smile, sad, etc.)
   - Speaks the response via TTS
   - Lip-syncs to audio

## Verify Everything is Connected

### Check Ports
```bash
# Windows
netstat -ano | findstr :4200
netstat -ano | findstr :5173
netstat -ano | findstr :8080

# Mac/Linux
lsof -i :4200
lsof -i :5173
lsof -i :8080
```

### Check Browser Console
Open DevTools (F12) → Console

Should see:
- ✅ `r3f-virtual-girlfriend-frontend-main` app loading
- ✅ `useAngularBridge` initialized
- ✅ postMessage API working

### Check Network Tab
- Look for requests to `/api/character/enrich` (enriching with animation data)
- Look for requests to `/api/tts/generate` (getting audio)

## Troubleshooting

### r3f Iframe Not Loading
**Problem**: Blank white box in character panel
```bash
# Check if r3f app is running
curl http://localhost:5173
# Should return HTML

# Check for CORS errors
# In browser console, look for security warnings
```

**Solution**:
```bash
# Kill and restart r3f
cd Characters/r3f-virtual-girlfriend-frontend-main
npm run dev
```

### Character Animations Not Playing
**Problem**: Character stands still (Idle only)
```bash
# Check if animations.glb is loaded
# In r3f browser console, look for Three.js warnings
```

**Solution**:
- Ensure `animations.glb` exists in `public/models/`
- Check file size > 100KB
- Try refreshing browser (Ctrl+Shift+R)

### Audio Not Playing
**Problem**: Character moves but no sound
```bash
# Check if TTS is running
curl http://localhost:8080/api/tts/health
# Should return: "TTS service is running"
```

**Solution**:
- Verify Java backend is running
- Check TTS API key in `application.properties`
- Try sending a message again

### "postMessage is not working"
**Problem**: Angular sends command but character doesn't respond
```bash
# In r3f browser console (F12), should see postMessage events
# In Angular browser console, should see PONG responses
```

**Solution**:
- Check iframe is fully loaded (wait 5 sec)
- Check browser console for errors
- Try clicking something in r3f iframe first (activates it)
- Refresh both windows

## Optional: Environment Configuration

### Change R3F Port
Edit `Frontend/src/app/services/r3f-character-bridge.service.ts`:
```typescript
private r3fOrigin = 'http://localhost:XXXX'; // Change 5173 to your port
```

### Change Character Model
In `r3f-virtual-girlfriend-frontend-main/src/components/Avatar.jsx`:
```javascript
const { nodes, materials, scene } = useGLTF("/models/YOUR_MODEL.glb");
```

### Disable Character Panel (Fallback)
If r3f won't load, comment out in `Frontend/src/app/pages/chat/chat.component.ts`:
```typescript
// imports: [..., R3fCharacterPanelComponent],
// Uncomment AnimatedCharacterComponent to use static character
```

## Performance Tips

- Close unused browser tabs (Three.js uses GPU)
- Disable browser extensions (may interfere with postMessage)
- Use Chrome/Edge for best WebGL performance
- Check GPU usage: DevTools → Performance tab

## Next Steps

✅ **All setup complete!**

Now you can:
1. Customize character expressions in `Avatar.jsx`
2. Add more animations to `animations.glb`
3. Adjust emotion→animation mapping in `CharacterIntegrationController.java`
4. Add voice control or gesture recognition
5. Deploy to production (see deployment guide)

Enjoy your immersive chatbot! 🎭✨
