package com.SocializerAI.service;

import com.SocializerAI.enums.ApproachType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Emotion-Aware Response Personalization Service
 * 
 * Personalizes HuggingFace responses based on:
 * - User's emotional state
 * - Conversation history patterns
 * - Intensity level
 * - User's preferred approach
 * - Previous successful response patterns
 */
public class EmotionAwarePersonalization {
    private static final Logger logger = LoggerFactory.getLogger(EmotionAwarePersonalization.class);

    private final Map<String, List<String>> emotionResponsePatterns;
    private final Map<String, List<String>> introductionPhrases;

    public EmotionAwarePersonalization() {
        this.emotionResponsePatterns = initializeEmotionPatterns();
        this.introductionPhrases = initializeIntroductions();
    }

    /**
     * Personalize AI response based on user context
     */
    public String personalizeResponse(String aiResponse, String emotion, int intensityScore, 
                                     ApproachType approach, String userMessage) {
        logger.info("Personalizing response for emotion: {}, intensity: {}, approach: {}", 
            emotion, intensityScore, approach);

        // Add personalization layer
        String personalized = aiResponse;

        // Adjust formality based on intensity
        personalized = adjustFormalityByIntensity(personalized, intensityScore);

        // Add emotion-specific personalization
        personalized = addEmotionSpecificPersonalization(personalized, emotion);

        // Ensure approach alignment
        personalized = ensureApproachAlignment(personalized, approach);

        // Add conversational warmth
        personalized = addConversationalWarmth(personalized, emotion);

        logger.debug("Personalized response length: {}", personalized.length());
        return personalized;
    }

    /**
     * Adjust response formality based on intensity level
     */
    private String adjustFormalityByIntensity(String response, int intensityScore) {
        if (intensityScore <= 4) {
            // Low intensity: More casual, friendly
            response = response.replaceAll("(?i)it (may|might) be", "it could be");
            response = response.replaceAll("(?i)you should consider", "you might want to try");
        } else if (intensityScore >= 8) {
            // High intensity: More direct, caring
            response = response.replaceAll("(?i)try to", "let's work on");
            response = response.replaceAll("(?i)might help", "can really help");
            response = response.replaceAll("(?i)consider", "focus on");
        }

        return response;
    }

    /**
     * Add emotion-specific personalization
     */
    private String addEmotionSpecificPersonalization(String response, String emotion) {
        if (emotion == null || emotion.isEmpty()) {
            return response;
        }

        switch (emotion.toLowerCase()) {
            case "anxiety":
                // Add grounding language
                if (!response.contains("breathe") && !response.contains("ground")) {
                    response = "Let's take this one step at a time. " + response;
                }
                break;

            case "depression":
                // Add hope and self-compassion
                if (!response.contains("compassion") && !response.contains("kind")) {
                    response = "Be gentle with yourself. " + response;
                }
                break;

            case "stress":
                // Add permission to rest
                if (!response.contains("rest") && !response.contains("pause")) {
                    response = "It's okay to pause and take care of yourself. " + response;
                }
                break;

            case "loneliness":
                // Add connection emphasis
                if (!response.contains("connect") && !response.contains("alone")) {
                    response = "You're not alone in feeling this way. " + response;
                }
                break;

            case "anger":
                // Add validation of feelings
                if (!response.contains("valid") && !response.contains("understandable")) {
                    response = "Your feelings are completely valid. " + response;
                }
                break;

            case "joy":
                // Celebrate and amplify
                if (!response.contains("celebrate") && !response.contains("wonderful")) {
                    response = "That's wonderful! " + response;
                }
                break;

            case "grief":
                // Honor and validate loss
                if (!response.contains("loss") && !response.contains("honor")) {
                    response = "It's important to honor what you're feeling. " + response;
                }
                break;
        }

        return response;
    }

    /**
     * Ensure response aligns with selected approach
     */
    private String ensureApproachAlignment(String response, ApproachType approach) {
        if (approach == ApproachType.EMPATHY) {
            // Ensure empathetic language
            if (!response.contains("understand") && !response.contains("feel")) {
                response = "I understand how you feel. " + response;
            }
        } else if (approach == ApproachType.SYMPATHY) {
            // Ensure supportive language
            if (!response.contains("here for") && !response.contains("support")) {
                response = "I'm here to support you. " + response;
            }
        }

        return response;
    }

    /**
     * Add conversational warmth and humanity
     */
    private String addConversationalWarmth(String response, String emotion) {
        // Add natural pauses and acknowledgments
        if (!response.contains("...") && !response.contains("—")) {
            // Add subtle pausing for emotional weight
            if (emotion != null && (emotion.equalsIgnoreCase("grief") || emotion.equalsIgnoreCase("depression"))) {
                response = response.replaceFirst("\\.", "... .");
            }
        }

        // Ensure personal pronouns for warmth
        if (!response.matches("(?i).*(i|you|we|me|us).*")) {
            response = "I want you to know: " + response;
        }

        return response;
    }

    /**
     * Initialize emotion-specific response patterns
     */
    private Map<String, List<String>> initializeEmotionPatterns() {
        Map<String, List<String>> patterns = new HashMap<>();

        patterns.put("anxiety", Arrays.asList(
            "Your worry is understandable",
            "Let's break this down together",
            "You can handle this",
            "One step at a time",
            "Your feelings are valid"
        ));

        patterns.put("depression", Arrays.asList(
            "You deserve compassion",
            "This feeling can change",
            "Small steps matter",
            "You're doing better than you think",
            "Be kind to yourself"
        ));

        patterns.put("stress", Arrays.asList(
            "You're carrying a lot",
            "Let's ease this burden",
            "You have the strength",
            "It's okay to rest",
            "One thing at a time"
        ));

        patterns.put("loneliness", Arrays.asList(
            "You deserve connection",
            "Your feelings matter",
            "You're not truly alone",
            "Reach out when ready",
            "You're valuable"
        ));

        patterns.put("anger", Arrays.asList(
            "Your anger makes sense",
            "Let's channel this energy",
            "You have the right to feel angry",
            "This can be constructive",
            "Your voice matters"
        ));

        patterns.put("joy", Arrays.asList(
            "This is wonderful",
            "Hold onto this feeling",
            "You deserve this happiness",
            "Keep this momentum going",
            "Celebrate yourself"
        ));

        patterns.put("grief", Arrays.asList(
            "Your loss matters",
            "Take the time you need",
            "Grief is love",
            "Honor your feelings",
            "You're not alone in this"
        ));

        patterns.put("confusion", Arrays.asList(
            "It's normal to feel confused",
            "Let's find clarity together",
            "Your questions are valid",
            "We'll figure this out",
            "Take your time"
        ));

        return patterns;
    }

    /**
     * Initialize personalized introduction phrases
     */
    private Map<String, List<String>> initializeIntroductions() {
        Map<String, List<String>> intros = new HashMap<>();

        intros.put("EMPATHY", Arrays.asList(
            "I really hear you on this",
            "I can sense what you're going through",
            "I understand how that feels",
            "I can feel the weight of what you're carrying",
            "That resonates deeply with me"
        ));

        intros.put("SYMPATHY", Arrays.asList(
            "I'm truly sorry you're going through this",
            "I'm here for you in this",
            "I care about what you're experiencing",
            "You're not alone in this struggle",
            "I'm committed to supporting you"
        ));

        return intros;
    }

    /**
     * Get emotion-specific suggestion
     */
    public String getEmotionSpecificSuggestion(String emotion) {
        List<String> patterns = emotionResponsePatterns.get(emotion != null ? emotion.toLowerCase() : "");
        if (patterns != null && !patterns.isEmpty()) {
            Random random = new Random();
            return patterns.get(random.nextInt(patterns.size()));
        }
        return "You're doing the right thing by reaching out.";
    }

    /**
     * Get approach-specific introduction
     */
    public String getApproachIntroduction(ApproachType approach) {
        List<String> intros = introductionPhrases.get(approach.toString());
        if (intros != null && !intros.isEmpty()) {
            Random random = new Random();
            return intros.get(random.nextInt(intros.size()));
        }
        return "I'm here for you.";
    }

    /**
     * Create personalized opening based on context
     */
    public String createPersonalizedOpening(String emotion, ApproachType approach, int intensityScore) {
        StringBuilder opening = new StringBuilder();

        // Get approach-specific intro
        opening.append(getApproachIntroduction(approach)).append(" ");

        // Add emotion acknowledgment
        opening.append(getEmotionSpecificSuggestion(emotion)).append(" ");

        // Add intensity-aware follow-up
        if (intensityScore >= 8) {
            opening.append("Let's focus on what you need right now.");
        } else if (intensityScore >= 5) {
            opening.append("Let's work through this together.");
        } else {
            opening.append("I'm here to help you navigate this.");
        }

        return opening.toString();
    }
}
