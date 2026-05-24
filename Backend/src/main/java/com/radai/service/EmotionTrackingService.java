package com.radai.service;

import com.radai.model.EmotionalHistory;
import com.radai.repository.EmotionalHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for tracking and analyzing user emotions over time.
 * Auto-saves detected emotions after each chat interaction.
 */
@Service
public class EmotionTrackingService {
    private static final Logger logger = LoggerFactory.getLogger(EmotionTrackingService.class);
    
    private final EmotionalHistoryRepository repository;

    public EmotionTrackingService(EmotionalHistoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Record a detected emotion from chat interaction
     */
    public EmotionalHistory recordEmotion(UUID userId, String emotion, int intensity, 
                                          String messageText, String source) {
        EmotionalHistory history = new EmotionalHistory();
        history.setUserId(userId);
        history.setEmotionalState(emotion);
        history.setIntensity(intensity);
        history.setMessageText(messageText);
        history.setSource(source != null ? source : "chat");
        history.setLoggedAt(LocalDateTime.now());
        
        // Calculate sentiment score from intensity (-1 to 1 range)
        double sentimentScore = calculateSentimentScore(emotion, intensity);
        history.setSentimentScore(sentimentScore);
        
        EmotionalHistory saved = repository.save(history);
        logger.info("Recorded emotion for user {}: {} (intensity: {})", userId, emotion, intensity);
        return saved;
    }
    
    /**
     * Get user's emotion history
     */
    public List<EmotionalHistory> getHistory(UUID userId) {
        return repository.findByUserIdOrderByLoggedAtAsc(userId);
    }
    
    /**
     * Get emotion trends for a user (last N days)
     */
    public Map<String, Object> getEmotionTrends(UUID userId) {
        List<EmotionalHistory> history = repository.findByUserIdOrderByLoggedAtAsc(userId);
        
        Map<String, Object> trends = new HashMap<>();
        
        if (history.isEmpty()) {
            trends.put("totalRecords", 0);
            trends.put("dominantEmotion", "neutral");
            trends.put("averageIntensity", 0.0);
            return trends;
        }
        
        // Count emotions
        Map<String, Long> emotionCounts = history.stream()
            .collect(Collectors.groupingBy(EmotionalHistory::getEmotionalState, Collectors.counting()));
        
        // Find dominant emotion
        String dominantEmotion = emotionCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("neutral");
        
        // Calculate average intensity
        double avgIntensity = history.stream()
            .mapToInt(EmotionalHistory::getIntensity)
            .average()
            .orElse(0.0);
        
        // Calculate average sentiment
        double avgSentiment = history.stream()
            .mapToDouble(EmotionalHistory::getSentimentScore)
            .average()
            .orElse(0.0);
        
        // Get recent emotions (last 5)
        List<Map<String, Object>> recentEmotions = history.stream()
            .sorted((a, b) -> b.getLoggedAt().compareTo(a.getLoggedAt()))
            .limit(5)
            .map(h -> {
                Map<String, Object> m = new HashMap<>();
                m.put("emotion", h.getEmotionalState());
                m.put("intensity", h.getIntensity());
                m.put("loggedAt", h.getLoggedAt().toString());
                return m;
            })
            .collect(Collectors.toList());
        
        trends.put("totalRecords", history.size());
        trends.put("dominantEmotion", dominantEmotion);
        trends.put("emotionCounts", emotionCounts);
        trends.put("averageIntensity", Math.round(avgIntensity * 10.0) / 10.0);
        trends.put("averageSentiment", Math.round(avgSentiment * 100.0) / 100.0);
        trends.put("recentEmotions", recentEmotions);
        
        return trends;
    }
    
    /**
     * Calculate sentiment score from emotion and intensity
     * Returns value between -1 (very negative) and 1 (very positive)
     */
    private double calculateSentimentScore(String emotion, int intensity) {
        // Map emotions to base sentiment
        double baseSentiment;
        switch (emotion.toLowerCase()) {
            case "joy":
            case "happy":
            case "excited":
                baseSentiment = 0.8;
                break;
            case "neutral":
            case "calm":
                baseSentiment = 0.1;
                break;
            case "stress":
            case "anxiety":
                baseSentiment = -0.4;
                break;
            case "sadness":
            case "sad":
            case "loneliness":
                baseSentiment = -0.5;
                break;
            case "anger":
            case "frustration":
                baseSentiment = -0.6;
                break;
            case "exhaustion":
                baseSentiment = -0.3;
                break;
            default:
                baseSentiment = 0.0;
        }
        
        // Adjust by intensity (1-10 scale)
        // High intensity amplifies the sentiment
        double intensityFactor = (intensity - 5) / 10.0; // -0.4 to 0.5
        double score = baseSentiment + (baseSentiment * intensityFactor);
        
        // Clamp to -1 to 1
        return Math.max(-1.0, Math.min(1.0, score));
    }
}

