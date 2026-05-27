package com.radai.controller;

import com.radai.dto.ChatbotFlowResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * CharacterIntegrationController bridges existing chat/TTS with r3f character animations.
 * Enriches responses with facial expressions, animations, and audio metadata.
 * Uses existing Deepseek + TTS infrastructure without external dependencies.
 */
@RestController
@RequestMapping("/api/character")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CharacterIntegrationController {
    private static final Logger logger = LoggerFactory.getLogger(CharacterIntegrationController.class);

    private final RestTemplate restTemplate;

    public CharacterIntegrationController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Map emotion keywords to r3f facial expressions
     */
    private static final Map<String, String> EMOTION_TO_EXPRESSION = Map.ofEntries(
        Map.entry("happy", "smile"),
        Map.entry("sad", "sad"),
        Map.entry("angry", "angry"),
        Map.entry("surprised", "surprised"),
        Map.entry("anxious", "funnyFace"),
        Map.entry("calm", "smile"),
        Map.entry("neutral", "default"),
        Map.entry("afraid", "terrified")
    );

    /**
     * Map response sentiment to animations
     */
    private static final Map<String, String> SENTIMENT_TO_ANIMATION = Map.ofEntries(
        Map.entry("positive", "Laughing"),
        Map.entry("negative", "Crying"),
        Map.entry("neutral", "Talking_1"),
        Map.entry("questioning", "Talking_0"),
        Map.entry("encouraging", "Talking_2"),
        Map.entry("concerned", "Idle")
    );

    /**
     * Enrich chat response with character metadata
     * POST /api/character/enrich
     * 
     * Request body (from existing flow controller):
     * {
     *   "conversationId": "...",
     *   "mainContent": "...",
     *   "emotion": "happy|sad|etc",
     *   "fullResponse": "..."
     * }
     * 
     * Response includes animation, facial expression, and audio metadata
     */
    @PostMapping("/enrich")
    public ResponseEntity<Map<String, Object>> enrichResponseWithCharacter(
            @RequestBody ChatbotFlowResponseDTO flowResponse,
            @RequestParam(defaultValue = "en") String language) {

        try {
            Map<String, Object> enriched = new LinkedHashMap<>();
            
            // Preserve original response
            enriched.put("conversationId", flowResponse.getConversationId());
            enriched.put("mainContent", flowResponse.getMainContent());
            enriched.put("fullResponse", flowResponse.getFullResponse());
            enriched.put("emotion", flowResponse.getEmotion());
            
            // Add character animation metadata
            String emotion = flowResponse.getEmotion() != null ? 
                flowResponse.getEmotion().toLowerCase() : "neutral";
            
            String facialExpression = EMOTION_TO_EXPRESSION.getOrDefault(emotion, "default");
            String animation = SENTIMENT_TO_ANIMATION.getOrDefault(
                detectSentiment(flowResponse.getMainContent()), 
                "Talking_1"
            );
            
            enriched.put("character", Map.of(
                "facialExpression", facialExpression,
                "animation", animation,
                "audioLanguage", language
            ));
            
            // Add TTS metadata for frontend to request audio separately
            enriched.put("audioMetadata", Map.of(
                "text", flowResponse.getMainContent(),
                "ttsEndpoint", "/api/tts/generate",
                "language", language,
                "audioLanguage", language
            ));
            
            logger.info("Enriched response with character metadata: emotion={}, animation={}, expression={}",
                emotion, animation, facialExpression);
            
            return ResponseEntity.ok(enriched);
            
        } catch (Exception e) {
            logger.error("Error enriching response: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Simple sentiment detection from response text (extensible)
     */
    private String detectSentiment(String text) {
        if (text == null || text.isEmpty()) return "neutral";
        
        String lower = text.toLowerCase();
        
        if (lower.contains("great") || lower.contains("wonderful") || lower.contains("excellent")) {
            return "positive";
        } else if (lower.contains("worried") || lower.contains("difficult") || lower.contains("struggle")) {
            return "negative";
        } else if (lower.contains("what") || lower.contains("how") || lower.contains("why")) {
            return "questioning";
        } else if (lower.contains("you can") || lower.contains("try") || lower.contains("let's")) {
            return "encouraging";
        } else if (lower.contains("understand") || lower.contains("concern") || lower.contains("problem")) {
            return "concerned";
        }
        
        return "neutral";
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "Character integration service is running",
            "availableExpressions", String.join(", ", EMOTION_TO_EXPRESSION.values()),
            "availableAnimations", String.join(", ", SENTIMENT_TO_ANIMATION.values())
        ));
    }
}
