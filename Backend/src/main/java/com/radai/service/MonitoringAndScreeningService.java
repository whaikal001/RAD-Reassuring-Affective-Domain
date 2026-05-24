package com.radai.service;

import com.radai.model.MonitoringContext;
import com.radai.enums.IntensityLevel;
import com.radai.enums.PathwayType;
import com.radai.service.emotion.EmotionClassifier;
import com.radai.service.config.AppConfig;

/**
 * Handles monitoring and screening of user's emotional state.
 * Detects intensity level, stressors, and determines appropriate pathway.
 */
public class MonitoringAndScreeningService {

    private final EmotionClassifier emotionClassifier;
    private final AppConfig appConfig;

    public MonitoringAndScreeningService() {
        this.appConfig = AppConfig.load();
        this.emotionClassifier = new EmotionClassifier(appConfig);
    }

    /**
     * Assess user's emotional state and update monitoring context
     */
    public void assessEmotionalState(MonitoringContext context, String userInput, int intensityScore) {
        // Classifier attempt
        String detectedEmotion = null;
        try {
            detectedEmotion = emotionClassifier.classifyEmotion(userInput);
        } catch (Exception e) {
            // ignore and fallback to heuristics
        }

        // Fallback
        if (detectedEmotion == null) {
            detectedEmotion = detectEmotion(userInput);
        }

        context.setCurrentEmotion(detectedEmotion);

        // Set intensity level
        context.setCurrentIntensityScore(intensityScore);
        IntensityLevel level = IntensityLevel.fromScore(intensityScore);
        context.setCurrentIntensityLevel(level);

        // Detect stressors and conditions
        detectStressors(context, userInput);
        detectCrisisIndicators(context, userInput);

        if (context.isCrisisDetected() || context.isSuicidalIdeationDetected()) {
            context.setCurrentEmotion("hopeless");
            context.setCurrentIntensityScore(Math.max(intensityScore, 9));
            context.setCurrentIntensityLevel(IntensityLevel.HIGH);
        }

        // Determine dominant stressor
        String stressor = extractMainStressor(userInput);
        context.setDominantStressor(stressor);
    }

    /**
     * Determine which pathway to use based on intensity and stress level
     * LOW + NO STRESS → PREVENTION
     * LOW + STRESS → PREVENTION (with preventive strategies)
     * MODERATE/HIGH → INTERVENTION
     */
    public PathwayType determinePathway(MonitoringContext context) {
        IntensityLevel intensity = context.getCurrentIntensityLevel();
        boolean stressDetected = context.isStressDetected();

        // Decision tree
        if (intensity == IntensityLevel.LOW && !stressDetected) {
            return PathwayType.PREVENTION; // Brief check-in, positive reinforcement
        } else if (intensity == IntensityLevel.LOW && stressDetected) {
            return PathwayType.PREVENTION; // Prevention with coping strategies
        } else if (intensity == IntensityLevel.MODERATE || intensity == IntensityLevel.HIGH) {
            return PathwayType.INTERVENTION; // Active intervention required
        }

        return PathwayType.PREVENTION; // Default
    }

    /**
     * Detect various stressors in user input
     */
    private void detectStressors(MonitoringContext context, String input) {
        String lower = input.toLowerCase();

        // Stress indicators
        if (containsAny(lower, "stress", "stressed", "overwhelm", "overwhelmed", "pressure", "too much", "swamped", "drowning", "burnout", "burned out")) {
            context.setStressDetected(true);
        }

        // Anxiety indicators
        if (containsAny(lower, "anxious", "anxiety", "panic", "panicking", "worried", "worry", "afraid", "scared", "nervous", "tense")) {
            context.setAnxietyDetected(true);
        }
    }

    /**
     * Detect crisis or emergency indicators
     */
    private void detectCrisisIndicators(MonitoringContext context, String input) {
        String lower = input.toLowerCase();

        // Crisis indicators
        if (lower.matches(".*(crisis|emergency|danger|help|emergency).*")) {
            context.setCrisisDetected(true);
        }

        // Suicidal ideation indicators
        if (lower.matches(".*(suicide|suicidal|kill myself|end it|no point|hopeless|helpless|give up|want to die|end my life|hurt myself|nak mati|mahu mati|nak bunuh diri|bunuh diri|tak mahu hidup|tak ingin hidup|tak ada sebab untuk hidup).*")) {
            context.setSuicidalIdeationDetected(true);
            context.setCrisisDetected(true);
        }

        // Safety-related questions → HIGH risk indicators
        // Users asking about safety often indicates they may be in crisis
        if (lower.matches(".*(safe place|am i safe|am i in a safe|safe right now|feeling unsafe|not safe|dangerous place|hurt myself|harm myself).*")) {
            context.setCrisisDetected(true);
            context.setSuicidalIdeationDetected(true); // Treat as potential crisis
        }
    }

    /**
     * Detect primary emotion from user input (heuristic fallback)
     */
    private String detectEmotion(String text) {
        String lower = text.toLowerCase();

        // Critical safety concerns → hopeless/crisis emotion
        if (lower.matches(".*(safe place|am i safe|am i in a safe|safe right now|feeling unsafe|not safe|dangerous|hurt myself|harm myself).*")) {
            return "hopeless";
        }

        if (lower.matches(".*(suicide|suicidal|kill myself|end it|want to die|end my life|nak mati|mahu mati|nak bunuh diri|bunuh diri|tak mahu hidup|tak ingin hidup).*")) {
            return "hopeless";
        }

        if (containsAny(lower, "stress", "stressed", "overwhelm", "overwhelmed", "pressure", "too much", "burnout", "burned out")) return "stress";
        if (containsAny(lower, "anxious", "anxiety", "panic", "panicking", "worried", "worry", "afraid", "scared")) return "anxiety";
        if (containsAny(lower, "sad", "sadness", "down", "unhappy", "depressed", "depression", "blue", "heartbroken")) return "sadness";
        if (containsAny(lower, "angry", "mad", "frustrated", "frustrating", "furious", "hate")) return "anger";
        if (containsAny(lower, "tired", "exhausted", "drained", "fatigue", "burned out")) return "exhaustion";
        if (containsAny(lower, "lonely", "loneliness", "isolated", "alone")) return "loneliness";
        if (containsAny(lower, "happy", "great", "wonderful", "excited", "amazing", "relieved", "grateful")) return "joy";

        return "neutral";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extract the main stressor/topic from user input
     */
    private String extractMainStressor(String text) {
        String lower = text.toLowerCase();

        if (lower.contains("assignment") || lower.contains("homework") || lower.contains("exam")) {
            return "academics";
        }
        if (lower.contains("work") || lower.contains("job") || lower.contains("boss")) {
            return "work";
        }
        if (lower.contains("relationship") || lower.contains("partner") || lower.contains("breakup")) {
            return "relationship";
        }
        if (lower.contains("family") || lower.contains("parents") || lower.contains("sibling")) {
            return "family";
        }
        if (lower.contains("friend") || lower.contains("friendship")) {
            return "friendship";
        }
        if (lower.contains("health") || lower.contains("sick") || lower.contains("illness")) {
            return "health";
        }
        if (lower.contains("money") || lower.contains("financial") || lower.contains("debt")) {
            return "financial";
        }
        if (lower.contains("sleep") || lower.contains("insomnia")) {
            return "sleep";
        }

        return "general";
    }

    /**
     * Check if user's condition has escalated compared to previous assessment
     */
    public boolean hasEscalated(MonitoringContext context) {
        return context.hasEscalated();
    }

    /**
     * Check if user's condition has improved compared to previous assessment
     */
    public boolean hasImproved(MonitoringContext context) {
        return context.hasDeEscalated();
    }

    /**
     * Check if emergency professional help is needed
     */
    public boolean requiresProfessionalHelp(MonitoringContext context) {
        return context.isSuicidalIdeationDetected() || 
               (context.getCurrentIntensityLevel() == IntensityLevel.HIGH && 
                context.getCycleCount() >= 10);
    }
}

