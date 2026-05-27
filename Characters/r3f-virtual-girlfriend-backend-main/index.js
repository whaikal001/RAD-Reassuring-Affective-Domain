import { exec } from "child_process";
import cors from "cors";
import dotenv from "dotenv";
import express from "express";
import { promises as fs } from "fs";
import { HfInference } from "@huggingface/inference";
dotenv.config();

const hf = new HfInference(process.env.HUGGINGFACE_API_KEY || "-");

// HuggingFace Models Configuration
const HF_CHAT_MODEL = process.env.HF_CHAT_MODEL || "meta-llama/Llama-3.1-8B-Instruct";

// TTS Models (HuggingFace Inference only - lightweight)
const HF_TTS_MODEL_EN = process.env.HF_TTS_MODEL_EN || "facebook/fastspeech2-en-ljspeech";
const HF_TTS_MODEL_MS = process.env.HF_TTS_MODEL_MS || "microsoft/speecht5_tts"; // Better Malay support

// Emotion Detection, Screening & Safety Models
const HF_EMOTION_MODEL = process.env.HF_EMOTION_MODEL || "j-hartmann/emotion-english-distilroberta-base";
const HF_SCREENING_MODEL = process.env.HF_SCREENING_MODEL || "facebook/bart-large-mnli";
const HF_SAFETY_MODEL = process.env.HF_SAFETY_MODEL || "unitary/toxic-bert";

// Emotion to Facial Expression Mapping (matches RAD AI backend)
const EMOTION_TO_EXPRESSION = {
  happy: "smile",
  sad: "sad",
  angry: "angry",
  surprised: "surprised",
  anxious: "funnyFace",
  calm: "smile",
  neutral: "default",
  afraid: "terrified",
};

// Sentiment to Animation Mapping (matches RAD AI backend)
const SENTIMENT_TO_ANIMATION = {
  positive: "Laughing",
  negative: "Crying",
  neutral: "Talking_1",
  questioning: "Talking_0",
  encouraging: "Talking_2",
  concerned: "Idle",
};

const app = express();
app.use(express.json());
app.use(cors());
const port = 3000;

app.get("/", (req, res) => {
  res.send("Hello World! Using HuggingFace Inference API + XTTS v2 TTS");
});

app.get("/models", async (req, res) => {
  res.send({
    chatModel: HF_CHAT_MODEL,
    ttsProvider: USE_XTTS ? "XTTS v2 (Coqui)" : "HuggingFace Inference",
    supportedLanguages: ["en", "ms"],
  });
});

// Helper: Detect sentiment from text
const detectSentimentAndMapAnimation = (text) => {
  const lower = text.toLowerCase();
  
  // Simple keyword-based sentiment detection
  if (lower.match(/(happy|great|amazing|wonderful|love|excellent|fantastic|perfect)/i)) {
    return SENTIMENT_TO_ANIMATION["positive"];
  }
  if (lower.match(/(sad|sorry|bad|terrible|awful|hate|poor|worst)/i)) {
    return SENTIMENT_TO_ANIMATION["negative"];
  }
  if (lower.match(/(\?|what|how|when|where|why|which)/i)) {
    return SENTIMENT_TO_ANIMATION["questioning"];
  }
  if (lower.match(/(please|would you|could you|can you|should|must|encourage)/i)) {
    return SENTIMENT_TO_ANIMATION["encouraging"];
  }
  if (lower.match(/(worry|concern|afraid|scared|nervous|anxious|problem|issue)/i)) {
    return SENTIMENT_TO_ANIMATION["concerned"];
  }
  
  return SENTIMENT_TO_ANIMATION["neutral"];
};

// Emotion Detection using HuggingFace
const detectEmotion = async (text) => {
  try {
    console.log(`Detecting emotion from: "${text.substring(0, 50)}..."`);
    
    const result = await hf.textClassification({
      model: HF_EMOTION_MODEL,
      inputs: text,
    });

    // Result is array of {label, score}, sorted by score descending
    const emotion = result[0];
    console.log(`Detected emotion: ${emotion.label} (confidence: ${(emotion.score * 100).toFixed(2)}%)`);
    
    return {
      emotion: emotion.label.toLowerCase(),
      confidence: emotion.score,
      allScores: result.map(r => ({ emotion: r.label, score: r.score }))
    };
  } catch (error) {
    console.error(`Error detecting emotion: ${error}`);
    return { emotion: "neutral", confidence: 0, error: error.message };
  }
};

// Pre-screening Classification (DASS category detection)
const screenUserInput = async (text) => {
  try {
    console.log(`Screening input for crisis indicators...`);
    
    const result = await hf.zeroShotClassification({
      model: HF_SCREENING_MODEL,
      inputs: text,
      candidate_labels: ["suicide risk", "self-harm", "crisis", "distress", "safe"],
    });

    console.log(`Screening result: ${result.labels[0]} (${(result.scores[0] * 100).toFixed(2)}%)`);
    
    return {
      category: result.labels[0],
      confidence: result.scores[0],
      allScores: result.labels.map((label, idx) => ({ category: label, score: result.scores[idx] }))
    };
  } catch (error) {
    console.error(`Error screening input: ${error}`);
    return { category: "safe", confidence: 0, error: error.message };
  }
};

// Safety Check - Toxicity Detection
const checkSafety = async (text) => {
  try {
    console.log(`Checking text safety for toxicity...`);
    
    const result = await hf.textClassification({
      model: HF_SAFETY_MODEL,
      inputs: text,
    });

    // Result indicates if text is toxic
    const isToxic = result[0].label.toLowerCase() === "toxic";
    const toxicityScore = isToxic ? result[0].score : result[1].score;
    
    console.log(`Safety check: ${isToxic ? "TOXIC" : "SAFE"} (score: ${(toxicityScore * 100).toFixed(2)}%)`);
    
    return {
      isSafe: !isToxic,
      isToxic: isToxic,
      toxicityScore: toxicityScore,
      allScores: result.map(r => ({ label: r.label, score: r.score }))
    };
  } catch (error) {
    console.error(`Error checking safety: ${error}`);
    return { isSafe: true, isToxic: false, error: error.message };
  }
};

const execCommand = (command) => {
  return new Promise((resolve, reject) => {
    exec(command, (error, stdout, stderr) => {
      if (error) reject(error);
      resolve(stdout);
    });
  });
};

// Generate TTS using HuggingFace Inference API only
const generateTTS = async (text, messageIndex, language = "en") => {
  try {
    const wavPath = `audios/message_${messageIndex}.wav`;
    console.log(`Generating TTS for message ${messageIndex} using local espeak-ng (${language})`);
    
    // Map our language codes to espeak language codes
    const espeak_lang = language === "ms" || language === "malay" ? "ms" : "en";
    
    // Use espeak-ng to generate speech
    // -w writes to WAV file, -l specifies language, -s speed, -a amplitude
    const cmd = `espeak-ng -w "${wavPath}" -l ${espeak_lang} -s 150 -a 200 "${text.replace(/"/g, '\\"')}"`;
    
    await execCommand(cmd);
    console.log(`TTS generated: ${wavPath}`);
    
    return wavPath;
  } catch (error) {
    console.error(`Error generating TTS: ${error.message}`);
    throw error;
  }
};

const lipSyncMessage = async (messageIndex) => {
  const time = new Date().getTime();
  const wavPath = `audios/message_${messageIndex}.wav`;
  console.log(`Starting lip sync for message ${messageIndex}`);
  
  try {
    await execCommand(
      `./bin/rhubarb -f json -o audios/message_${messageIndex}.json "${wavPath}" -r phonetic`
    );
    console.log(`Lip sync done in ${new Date().getTime() - time}ms`);
  } catch (error) {
    console.error(`Error generating lip sync: ${error}`);
    throw error;
  }
};

app.post("/chat", async (req, res) => {
  const userMessage = req.body.message;
  const language = req.body.language || "en"; // "en" or "ms" for Malay
  
  if (!userMessage) {
    res.send({
      messages: [
        {
          text: "Hey dear... How was your day?",
          audio: await audioFileToBase64("audios/intro_0.wav"),
          lipsync: await readJsonTranscript("audios/intro_0.json"),
          facialExpression: "smile",
          animation: "Talking_1",
        },
        {
          text: "I missed you so much... Please don't go for so long!",
          audio: await audioFileToBase64("audios/intro_1.wav"),
          lipsync: await readJsonTranscript("audios/intro_1.json"),
          facialExpression: "sad",
          animation: "Crying",
        },
      ],
    });
    return;
  }

  if (!process.env.HUGGINGFACE_API_KEY || process.env.HUGGINGFACE_API_KEY === "-") {
    res.send({
      messages: [
        {
          text: "Please add your HuggingFace API key!",
          audio: await audioFileToBase64("audios/api_0.wav"),
          lipsync: await readJsonTranscript("audios/api_0.json"),
          facialExpression: "angry",
          animation: "Angry",
        },
        {
          text: "Set the HUGGINGFACE_API_KEY environment variable to proceed.",
          audio: await audioFileToBase64("audios/api_1.wav"),
          lipsync: await readJsonTranscript("audios/api_1.json"),
          facialExpression: "smile",
          animation: "Laughing",
        },
      ],
    });
    return;
  }

  try {
    // Generate response using HuggingFace Inference API
    console.log(`[CHAT] Generating chat response using model: ${HF_CHAT_MODEL}`);
    console.log(`[CHAT] User message: "${userMessage}"`);
    console.log(`[CHAT] API Key present: ${!!process.env.HUGGINGFACE_API_KEY}`);
    
    const systemPrompt = `You are a helpful and empathetic virtual companion. Keep responses brief and natural.`;
    
    const prompt = `${systemPrompt}\n\nUser: ${userMessage}\n\nAssistant:`;

    console.log(`[CHAT] Sending request to HuggingFace API...`);
    
    let response;
    try {
      // Use Spring Boot backend (internal network) instead of external HF API
      console.log(`[CHAT] Calling Spring Boot backend at backend:8080 for chat response...`);
      
      try {
        // Generate a consistent user ID for the character backend
        const userId = 'f47ac10b-58cc-4372-a567-0e02b2c3d479';
        const conversationId = 'char-conv-' + Date.now();
        
        const springBootResponse = await fetch('http://backend:8080/api/chat/flow/process', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-User-ID': userId,
            'X-Conversation-ID': conversationId,
            'X-Screening-Completed': 'true',
            'X-Screening-Action': 'allow'
          },
          body: JSON.stringify({ 
            userMessage: userMessage,
            language: language,
            intensityScore: 5
          }),
        });

        if (!springBootResponse.ok) {
          throw new Error(`Spring Boot API returned ${springBootResponse.status}`);
        }

        const springBootData = await springBootResponse.json();
        let assistantText = springBootData.mainContent || springBootData.reply || 'I appreciate you sharing that with me.';
        
        console.log(`[CHAT] Spring Boot response: "${assistantText}"`);
        
        // Create response object that matches our format
        response = {
          generated_text: prompt + assistantText
        };
      } catch (backendError) {
        console.error(`[CHAT] Spring Boot call failed:`, backendError.message);
        console.log(`[CHAT] Falling back to local response generation...`);
        // Generate a basic local response
        const localResponses = [
          "That's really important to share. Tell me more.",
          "I understand. What else is on your mind?",
          "That sounds significant. Can you elaborate?",
          "I'm listening. Help me understand better.",
          "Thank you for trusting me with that."
        ];
        const randomResponse = localResponses[Math.floor(Math.random() * localResponses.length)];
        response = {
          generated_text: prompt + randomResponse
        };
      }
    } catch (apiError) {
      console.error(`[CHAT ERROR] HuggingFace API call failed:`, apiError.message);
      console.error(`[CHAT ERROR] Full error:`, JSON.stringify(apiError, null, 2));
      console.error(`[CHAT ERROR] Error status:`, apiError.status);
      console.error(`[CHAT ERROR] Error headers:`, apiError.headers);
      // Return graceful fallback response
      const fallbackMessages = [
        {
          text: `I hear you saying: "${userMessage.substring(0, 50)}". That's important. Tell me more.`,
          facialExpression: "smile",
          animation: "Talking_1",
          audio: null,
          lipsync: null
        }
      ];
      res.send({ messages: fallbackMessages, debug: { error: apiError.message, fallback: true } });
      return;
    }

    // Parse the generated text
    let generatedText = response.generated_text;
    console.log(`[CHAT] Generated text length: ${generatedText.length}`);
    console.log(`[CHAT] Generated preview: ${generatedText.substring(0, 100)}...`);
    
    // Extract the assistant's response (everything after "Assistant:")
    const assistantMatch = generatedText.match(/Assistant:\s*([\s\S]*?)$/);
    const assistantText = assistantMatch ? assistantMatch[1].trim() : generatedText.trim();
    
    console.log(`[CHAT] Assistant response: "${assistantText}"`);

    // Detect emotion from response
    let emotion = "neutral";
    try {
      const emotionResult = await detectEmotion(assistantText);
      emotion = emotionResult.emotion;
      console.log(`[CHAT] Detected emotion: ${emotion}`);
    } catch (emotionError) {
      console.warn(`[CHAT] Emotion detection failed:`, emotionError.message);
    }

    // Detect animation from sentiment
    const animation = detectSentimentAndMapAnimation(assistantText);
    const facialExpression = EMOTION_TO_EXPRESSION[emotion] || "default";
    
    console.log(`[CHAT] Animation: ${animation}, Expression: ${facialExpression}`);

    // Create single message response
    const message = {
      text: assistantText,
      facialExpression: facialExpression,
      animation: animation,
      emotion: emotion,
      audio: null,
      lipsync: null
    };

    // Try to generate TTS and lipsync
    try {
      console.log(`[CHAT] Generating TTS...`);
      await generateTTS(message.text, 0, language);
      
      console.log(`[CHAT] Generating lipsync...`);
      await lipSyncMessage(0);
      
      // Encode audio and read lipsync
      message.audio = await audioFileToBase64(`audios/message_0.wav`);
      message.lipsync = await readJsonTranscript(`audios/message_0.json`);
      console.log(`[CHAT] Audio and lipsync generated successfully`);
    } catch (audioError) {
      console.warn(`[CHAT] Audio/lipsync generation failed (will continue without):`, audioError.message);
      // Continue without audio - still return the text response
    }

    res.send({ messages: [message] });
  } catch (error) {
    console.error(`[CHAT ERROR] Unhandled error in chat endpoint:`, error);
    res.status(500).send({
      error: error.message,
      messages: [
        {
          text: "I'm having trouble processing that right now. Please try again in a moment.",
          facialExpression: "sad",
          animation: "Idle",
          audio: null,
          lipsync: null
        },
      ],
    });
  }
});

/**
 * NEW ENDPOINT: Matches RAD AI Backend API
 * POST /api/character/enrich
 * Enriches chat response with character animation metadata
 * 
 * Request body:
 * {
 *   "conversationId": "...",
 *   "mainContent": "response text",
 *   "fullResponse": "full response",
 *   "emotion": "happy|sad|etc"
 * }
 * 
 * Response includes character metadata (animation, expression, etc)
 */
app.post("/api/character/enrich", async (req, res) => {
  const { conversationId, mainContent, fullResponse, emotion } = req.body;
  const language = req.query.language || "en";

  if (!mainContent) {
    return res.status(400).json({ error: "mainContent is required" });
  }

  try {
    console.log(`Enriching character response (emotion: ${emotion}, language: ${language})`);

    // Map emotion to facial expression
    const mappedEmotion = emotion ? emotion.toLowerCase() : "neutral";
    const facialExpression = EMOTION_TO_EXPRESSION[mappedEmotion] || "default";
    
    // Detect sentiment and map to animation
    const animation = detectSentimentAndMapAnimation(mainContent);

    // Generate TTS audio
    const wavPath = `audios/enriched_0.wav`;
    await generateTTS(mainContent, 0, language);

    // Generate lip sync
    await lipSyncMessage(0);

    // Encode audio
    const audioBase64 = await audioFileToBase64(wavPath);
    const lipsync = await readJsonTranscript("audios/enriched_0.json").catch(() => ({}));

    // Return enriched response matching RAD AI format
    const enriched = {
      conversationId,
      mainContent,
      fullResponse,
      emotion,
      character: {
        facialExpression,
        animation,
        audioLanguage: language,
      },
      audio: {
        base64: audioBase64,
        lipsync: lipsync,
        format: "wav",
        language: language,
      },
    };

    res.json(enriched);
  } catch (error) {
    console.error(`Error in enrich endpoint: ${error}`);
    res.status(500).json({
      error: error.message,
      character: {
        facialExpression: "default",
        animation: "Idle",
      },
    });
  }
});

/**
 * NEW ENDPOINT: Emotion Detection for Daily Tracking
 * POST /api/character/detect-emotion
 * Detects user emotion to track daily progress
 */
app.post("/api/character/detect-emotion", async (req, res) => {
  const { userMessage, conversationId } = req.body;

  if (!userMessage) {
    return res.status(400).json({ error: "userMessage is required" });
  }

  try {
    const emotionResult = await detectEmotion(userMessage);
    const safetyResult = await checkSafety(userMessage);

    res.json({
      conversationId,
      userMessage,
      emotionDetection: emotionResult,
      safetyCheck: safetyResult,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    console.error(`Error detecting emotion: ${error}`);
    res.status(500).json({ error: error.message });
  }
});

/**
 * NEW ENDPOINT: Pre-screening Classification
 * POST /api/character/screen
 * Screens input for crisis indicators (DASS: Suicide, Self-harm, Crisis, Distress)
 */
app.post("/api/character/screen", async (req, res) => {
  const { userMessage, conversationId } = req.body;

  if (!userMessage) {
    return res.status(400).json({ error: "userMessage is required" });
  }

  try {
    const screeningResult = await screenUserInput(userMessage);
    const emotionResult = await detectEmotion(userMessage);
    const safetyResult = await checkSafety(userMessage);

    // Determine alert level
    let alertLevel = "green"; // Safe
    if (screeningResult.category === "suicide risk" || screeningResult.category === "self-harm") {
      alertLevel = "red"; // High risk
    } else if (screeningResult.category === "crisis" || screeningResult.category === "distress") {
      alertLevel = "yellow"; // Moderate risk
    }

    res.json({
      conversationId,
      userMessage,
      screening: screeningResult,
      emotion: emotionResult,
      safety: safetyResult,
      alertLevel: alertLevel,
      requiresIntervention: alertLevel !== "green",
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    console.error(`Error screening input: ${error}`);
    res.status(500).json({ error: error.message });
  }
});

/**
 * NEW ENDPOINT: Daily Progress Tracking
 * POST /api/character/track-progress
 * Comprehensive daily emotion tracking for progress monitoring
 */
app.post("/api/character/track-progress", async (req, res) => {
  const { userMessage, conversationId, userId } = req.body;

  if (!userMessage) {
    return res.status(400).json({ error: "userMessage is required" });
  }

  try {
    const emotionResult = await detectEmotion(userMessage);
    const screeningResult = await screenUserInput(userMessage);
    const safetyResult = await checkSafety(userMessage);

    // Calculate progress indicators
    const dailyProgressReport = {
      conversationId,
      userId,
      userMessage,
      
      // Emotion tracking
      currentEmotion: emotionResult.emotion,
      emotionConfidence: emotionResult.confidence,
      emotionHistory: emotionResult.allScores,
      
      // Risk assessment
      screeningCategory: screeningResult.category,
      screeningConfidence: screeningResult.confidence,
      isAtRisk: screeningResult.category === "suicide risk" || screeningResult.category === "self-harm",
      
      // Safety assessment
      isSafe: safetyResult.isSafe,
      toxicityScore: safetyResult.toxicityScore,
      
      // Character response recommendation
      suggestedAnimation: detectSentimentAndMapAnimation(userMessage),
      suggestedExpression: EMOTION_TO_EXPRESSION[emotionResult.emotion] || "default",
      
      // Timestamps
      timestamp: new Date().toISOString(),
      date: new Date().toISOString().split("T")[0], // YYYY-MM-DD for daily grouping
    };

    res.json(dailyProgressReport);
  } catch (error) {
    console.error(`Error tracking progress: ${error}`);
    res.status(500).json({ error: error.message });
  }
});

/**
 * NEW ENDPOINT: Safety Check Only
 * POST /api/character/check-safety
 * Quick safety/toxicity check
 */
app.post("/api/character/check-safety", async (req, res) => {
  const { userMessage, conversationId } = req.body;

  if (!userMessage) {
    return res.status(400).json({ error: "userMessage is required" });
  }

  try {
    const safetyResult = await checkSafety(userMessage);

    res.json({
      conversationId,
      userMessage,
      safety: safetyResult,
      canProceed: safetyResult.isSafe,
      timestamp: new Date().toISOString(),
    });
  } catch (error) {
    console.error(`Error checking safety: ${error}`);
    res.status(500).json({ error: error.message });
  }
});

const readJsonTranscript = async (file) => {
  try {
    const data = await fs.readFile(file, "utf8");
    return JSON.parse(data);
  } catch (error) {
    console.warn(`[WARNING] Failed to read lipsync file ${file}: ${error.message}`);
    return { mouthCues: [] }; // Return empty lipsync data
  }
};

const audioFileToBase64 = async (file) => {
  try {
    const data = await fs.readFile(file);
    return data.toString("base64");
  } catch (error) {
    console.warn(`[WARNING] Failed to read audio file ${file}: ${error.message}`);
    return null; // Return null if file doesn't exist
  }
};

app.listen(port, () => {
  console.log(`Virtual Girlfriend listening on port ${port}`);
});
