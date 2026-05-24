package com.radai.service;

import com.radai.model.Conversation;
import com.radai.model.Message;
import com.radai.model.EmotionalHistory;
import com.radai.repository.ConversationRepository;
import com.radai.repository.MessageRepository;
import com.radai.repository.EmotionalHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for session history dashboard functionality.
 * Provides comprehensive view of user's chat sessions, messages, and emotional journey.
 */
@Service
public class SessionHistoryService {
    private static final Logger logger = LoggerFactory.getLogger(SessionHistoryService.class);
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final EmotionalHistoryRepository emotionalHistoryRepository;

    public SessionHistoryService(ConversationRepository conversationRepository,
                                  MessageRepository messageRepository,
                                  EmotionalHistoryRepository emotionalHistoryRepository) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.emotionalHistoryRepository = emotionalHistoryRepository;
    }

    /**
     * Get dashboard summary for a user
     */
    public Map<String, Object> getDashboardSummary(UUID userId) {
        Map<String, Object> summary = new HashMap<>();
        
        // Get all conversations
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByStartedAtDesc(userId);
        
        // Get emotional history
        List<EmotionalHistory> emotionHistory = emotionalHistoryRepository.findByUserIdOrderByLoggedAtAsc(userId);
        
        // Basic stats
        summary.put("totalSessions", conversations.size());
        summary.put("totalEmotionRecords", emotionHistory.size());
        
        // Active session
        Optional<Conversation> activeSession = conversations.stream()
            .filter(c -> c.getIsActive() != null && c.getIsActive())
            .findFirst();
        summary.put("hasActiveSession", activeSession.isPresent());
        if (activeSession.isPresent()) {
            summary.put("activeSessionId", activeSession.get().getId().toString());
        }
        
        // Recent sessions (last 5)
        List<Map<String, Object>> recentSessions = conversations.stream()
            .limit(5)
            .map(this::convertSessionToMap)
            .collect(Collectors.toList());
        summary.put("recentSessions", recentSessions);
        
        // Emotion distribution
        if (!emotionHistory.isEmpty()) {
            Map<String, Long> emotionCounts = emotionHistory.stream()
                .collect(Collectors.groupingBy(EmotionalHistory::getEmotionalState, Collectors.counting()));
            summary.put("emotionDistribution", emotionCounts);
            
            // Dominant emotion
            String dominantEmotion = emotionCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("neutral");
            summary.put("dominantEmotion", dominantEmotion);
            
            // Average intensity
            double avgIntensity = emotionHistory.stream()
                .mapToInt(EmotionalHistory::getIntensity)
                .average()
                .orElse(0.0);
            summary.put("averageIntensity", Math.round(avgIntensity * 10.0) / 10.0);
            
            // Recent emotion trend (improving/declining/stable)
            String trend = calculateEmotionTrend(emotionHistory);
            summary.put("emotionTrend", trend);
        }
        
        return summary;
    }

    /**
     * Get detailed session history with messages
     */
    public Map<String, Object> getSessionWithMessages(UUID conversationId) {
        Map<String, Object> result = new HashMap<>();
        
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found: " + conversationId));
        
        List<Message> messages = messageRepository.findByConversationIdOrderByTimestampAsc(conversationId);
        
        result.put("session", convertSessionToMap(conversation));
        result.put("messages", messages.stream().map(this::convertMessageToMap).collect(Collectors.toList()));
        result.put("messageCount", messages.size());
        
        // Session stats
        if (!messages.isEmpty()) {
            long userMessages = messages.stream().filter(m -> "user".equals(m.getSender()) || "user".equals(m.getSenderType())).count();
            long botMessages = messages.stream().filter(m -> "bot".equals(m.getSender()) || "ai".equals(m.getSenderType())).count();
            result.put("userMessageCount", userMessages);
            result.put("botMessageCount", botMessages);
            
            // Emotions in this session
            List<String> sessionEmotions = messages.stream()
                .map(Message::getDetectedEmotion)
                .filter(e -> e != null && !e.isEmpty())
                .distinct()
                .collect(Collectors.toList());
            result.put("sessionEmotions", sessionEmotions);
        }
        
        return result;
    }

    /**
     * Get all sessions for a user with pagination
     */
    public Map<String, Object> getAllSessions(UUID userId, int page, int size) {
        List<Conversation> allConversations = conversationRepository.findByUserIdOrderByStartedAtDesc(userId);
        
        int totalItems = allConversations.size();
        int totalPages = (int) Math.ceil((double) totalItems / size);
        int start = page * size;
        int end = Math.min(start + size, totalItems);

        List<Map<String, Object>> sessions;
        if (start >= totalItems || start < 0) {
            sessions = new ArrayList<>();
        } else {
            sessions = allConversations.subList(start, end).stream()
                .map(this::convertSessionToMap)
                .collect(Collectors.toList());
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("sessions", sessions);
        result.put("currentPage", page);
        result.put("totalPages", totalPages);
        result.put("totalItems", totalItems);
        result.put("pageSize", size);
        
        return result;
    }

    /**
     * Get existing flow conversation by sessionId or create one.
     */
    public Conversation getOrCreateConversation(UUID userId, String sessionId) {
        return conversationRepository
            .findTopByUserIdAndSessionIdOrderByStartedAtDesc(userId, sessionId)
            .orElseGet(() -> {
                Conversation conversation = new Conversation();
                conversation.setUserId(userId);
                conversation.setSessionId(sessionId);
                conversation.setIsActive(true);
                conversation.setMessageCount(0);
                return conversationRepository.save(conversation);
            });
    }

    /**
     * Persist the user message and bot response for flow chat history.
     */
    public void saveFlowExchange(UUID userId, String sessionId, String userMessage, String botMessage,
                                 String emotion, Integer intensity) {
        Conversation conversation = getOrCreateConversation(userId, sessionId);
        saveMessage(conversation.getId(), "user", userMessage, null, intensity);
        saveMessage(conversation.getId(), "bot", botMessage, emotion, intensity);
    }

    /**
     * Mark conversation ended by flow session id.
     */
    public void endConversation(UUID userId, String sessionId) {
        conversationRepository
            .findTopByUserIdAndSessionIdAndIsActiveOrderByStartedAtDesc(userId, sessionId, true)
            .ifPresent(conversation -> {
                LocalDateTime now = LocalDateTime.now();
                conversation.setEndedAt(now);
                conversation.setIsActive(false);

                LocalDateTime start = conversation.getStartedAt();
                if (start != null) {
                    conversation.setSessionDuration((int) ChronoUnit.MINUTES.between(start, now));
                }

                conversationRepository.save(conversation);
            });
    }

    /**
     * End all active conversations for a user.
     */
    public void endAllActiveConversations(UUID userId) {
        List<Conversation> activeConversations = conversationRepository.findByUserIdAndIsActive(userId, true);

        if (activeConversations.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Conversation conversation : activeConversations) {
            conversation.setEndedAt(now);
            conversation.setIsActive(false);

            LocalDateTime start = conversation.getStartedAt();
            if (start != null) {
                conversation.setSessionDuration((int) ChronoUnit.MINUTES.between(start, now));
            }
        }

        conversationRepository.saveAll(activeConversations);
    }

    /**
     * Get emotional journey timeline
     */
    public List<Map<String, Object>> getEmotionalJourney(UUID userId) {
        List<EmotionalHistory> history = emotionalHistoryRepository.findByUserIdOrderByLoggedAtAsc(userId);
        
        return history.stream().map(e -> {
            Map<String, Object> point = new HashMap<>();
            point.put("timestamp", e.getLoggedAt().toString());
            point.put("emotion", e.getEmotionalState());
            point.put("intensity", e.getIntensity());
            point.put("sentiment", e.getSentimentScore());
            return point;
        }).collect(Collectors.toList());
    }

    /**
     * Save a message to a conversation (for tracking)
     */
    public Message saveMessage(UUID conversationId, String sender, String content, 
                               String emotion, Integer intensity) {
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSender(sender);
        message.setContent(content);
        message.setDetectedEmotion(emotion);
        message.setIntensityScore(intensity);
        message.setTimestamp(LocalDateTime.now());
        
        // Update conversation message count
        try {
            Conversation conv = conversationRepository.findById(conversationId).orElse(null);
            if (conv != null) {
                conv.setMessageCount(conv.getMessageCount() + 1);
                conversationRepository.save(conv);
            }
        } catch (Exception e) {
            logger.warn("Could not update conversation message count: {}", e.getMessage());
        }
        
        return messageRepository.save(message);
    }

    private Map<String, Object> convertSessionToMap(Conversation c) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.getId().toString());
        map.put("userId", c.getUserId().toString());
        map.put("sessionId", c.getSessionId());
        map.put("startedAt", c.getStartedAt() != null ? c.getStartedAt().toString() : null);
        map.put("endedAt", c.getEndedAt() != null ? c.getEndedAt().toString() : null);
        map.put("isActive", c.getIsActive());
        map.put("messageCount", c.getMessageCount());
        map.put("sessionDuration", c.getSessionDuration());
        return map;
    }

    private Map<String, Object> convertMessageToMap(Message m) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId().toString());
        map.put("sender", m.getSenderType() != null ? m.getSenderType() : m.getSender());
        map.put("content", m.getContent() != null ? m.getContent() : m.getMessageText());
        map.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : 
                            (m.getSentAt() != null ? m.getSentAt().toString() : null));
        map.put("emotion", m.getDetectedEmotion() != null ? m.getDetectedEmotion() : m.getEmotion());
        map.put("intensity", m.getIntensityScore());
        return map;
    }

    private String calculateEmotionTrend(List<EmotionalHistory> history) {
        if (history.size() < 3) {
            return "stable";
        }
        
        // Compare last 3 entries with previous 3
        int recentCount = Math.min(3, history.size());
        int olderStart = Math.max(0, history.size() - 6);
        int olderEnd = history.size() - 3;
        
        if (olderEnd <= olderStart) {
            return "stable";
        }
        
        // Calculate average sentiment for recent and older periods
        double recentAvg = history.subList(history.size() - recentCount, history.size()).stream()
            .mapToDouble(EmotionalHistory::getSentimentScore)
            .average()
            .orElse(0.0);
        
        double olderAvg = history.subList(olderStart, olderEnd).stream()
            .mapToDouble(EmotionalHistory::getSentimentScore)
            .average()
            .orElse(0.0);
        
        double diff = recentAvg - olderAvg;
        
        if (diff > 0.1) {
            return "improving";
        } else if (diff < -0.1) {
            return "declining";
        }
        return "stable";
    }
}

