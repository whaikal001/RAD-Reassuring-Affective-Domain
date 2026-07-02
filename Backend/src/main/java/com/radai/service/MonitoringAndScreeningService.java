package com.radai.service;

import com.radai.model.MonitoringContext;
import com.radai.enums.IntensityLevel;
import com.radai.enums.PathwayType;
import com.radai.service.emotion.EmotionClassifier;
import com.radai.service.emotion.EmotionScoringEngine;
import com.radai.service.crisis.CrisisDetectionEngine;
import com.radai.service.config.AppConfig;

/**
 * Handles monitoring and screening of user's emotional state.
 * Detects intensity level, stressors, and determines appropriate pathway.
 */
public class MonitoringAndScreeningService {

    private final EmotionClassifier emotionClassifier;
    private final EmotionScoringEngine emotionScoringEngine;
    private final CrisisDetectionEngine crisisDetectionEngine;
    private final AppConfig appConfig;

    public MonitoringAndScreeningService() {
        this.appConfig = AppConfig.load();
        this.emotionClassifier = new EmotionClassifier(appConfig);
        this.emotionScoringEngine = new EmotionScoringEngine();
        this.crisisDetectionEngine = new CrisisDetectionEngine();
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
     * Detect crisis or emergency indicators.
     *
     * <p>Delegates to the recall-focused {@link CrisisDetectionEngine}, which recognises not only
     * explicit suicide language but also indirect / passive ideation ("no reason to live", "better
     * off without me") in English and Malay — the dangerous false-negatives the old regex missed.
     */
    private void detectCrisisIndicators(MonitoringContext context, String input) {
        CrisisDetectionEngine.CrisisResult result = crisisDetectionEngine.detect(input);
        if (result.crisis()) {
            context.setCrisisDetected(true);
        }
        if (result.suicidalIdeation()) {
            context.setSuicidalIdeationDetected(true);
        }
    }

    /**
     * Detect primary emotion from user input (heuristic fallback).
     *
     * <p>Delegates to the weighted-lexicon {@link EmotionScoringEngine}, which scores every emotion
     * and returns the strongest rather than the first keyword to match. Safety words are weighted
     * high enough to win, preserving the original crisis-first behaviour; the caller additionally
     * forces {@code hopeless} whenever the crisis/suicidal flags are set.
     */
    private String detectEmotion(String text) {
        return emotionScoringEngine.classifyLabel(text);
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

