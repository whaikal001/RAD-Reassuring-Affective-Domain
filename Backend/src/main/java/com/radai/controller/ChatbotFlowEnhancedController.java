package com.radai.controller;

import com.radai.service.ChatbotFlowEngine;
import com.radai.service.FlowWithAIService;
import com.radai.service.EmotionTrackingService;
import com.radai.service.SessionHistoryService;
import com.radai.service.PbotUsbService;
import com.radai.model.FlowResponse;
import com.radai.dto.ChatbotFlowRequest;
import com.radai.dto.ChatbotFlowResponseDTO;
import com.radai.chat.hf.HuggingFaceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PostConstruct;
import java.util.UUID;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat/flow")
public class ChatbotFlowEnhancedController {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotFlowEnhancedController.class);

    @Autowired(required = false)
    private HuggingFaceClient huggingFaceClient;
    
    @Autowired(required = false)
    private EmotionTrackingService emotionTrackingService;
    
    @Autowired(required = false)
    private SessionHistoryService sessionHistoryService;

    @Autowired(required = false)
    private PbotUsbService pbotUsbService;

    private final ChatbotFlowEngine flowEngine;
    private FlowWithAIService flowWithAIService;

    public ChatbotFlowEnhancedController() {
        this.flowEngine = new ChatbotFlowEngine("en");
    }

    @PostConstruct
    public void init() {
        // Initialize Flow+AI service if HuggingFace is available
        if (huggingFaceClient != null) {
            this.flowWithAIService = new FlowWithAIService(flowEngine, huggingFaceClient, true);
            logger.info("Flow+AI service initialized with HuggingFace");
        }
    }

    @PostMapping("/process")
    public ResponseEntity<ChatbotFlowResponseDTO> processMessage(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Conversation-ID", required = false) String conversationId,
            @RequestBody ChatbotFlowRequest request) {
        
        try {
            UUID userUUID = UUID.fromString(userId);
            String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
            String language = request.language() != null ? request.language() : "en";
            
            logger.info("Processing with Flow System only - user: {}, intensity: {}", userId, request.intensityScore());
            
            ChatbotFlowEngine engine = new ChatbotFlowEngine(language);
            FlowResponse flowResponse = engine.processUserMessage(
                userUUID,
                convId,
                request.userMessage(),
                request.intensityScore(),
                request.dassBand()
            );
            
            ChatbotFlowResponseDTO dto = convertToDTO(flowResponse);
            dto.addMetadata("mode", "flow-only");

            if (sessionHistoryService != null) {
                try {
                    sessionHistoryService.saveFlowExchange(
                        userUUID,
                        convId,
                        request.userMessage(),
                        responseText(flowResponse),
                        flowResponse.getEmotion(),
                        flowResponse.getIntensity()
                    );
                } catch (Exception e) {
                    logger.warn("Failed to persist flow-only chat history: {}", e.getMessage());
                }
            }

            sendToPbot(flowResponse);
            
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            logger.error("Error in flow processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/process-with-ai")
    public ResponseEntity<ChatbotFlowResponseDTO> processMessageWithAI(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader(value = "X-Conversation-ID", required = false) String conversationId,
            @RequestBody ChatbotFlowRequest request) {
        
        // Check if HuggingFace is available
        if (huggingFaceClient == null || flowWithAIService == null) {
            logger.warn("HuggingFace not configured. Falling back to flow-only mode.");
            return processMessage(userId, conversationId, request);
        }
        
        try {
            UUID userUUID = UUID.fromString(userId);
            String convId = conversationId != null ? conversationId : UUID.randomUUID().toString();
            String language = request.language() != null ? request.language() : "en";
            
            logger.info("Processing with Flow + AI - user: {}, intensity: {}, language: {}", userId, request.intensityScore(), language);
            
            FlowResponse flowResponse = flowWithAIService.processWithAI(
                userUUID,
                convId,
                request.userMessage(),
                request.intensityScore(),
                language,
                request.dassBand()
            );
            
            // Auto-track emotion in database
            if (emotionTrackingService != null && flowResponse.getEmotion() != null) {
                try {
                    emotionTrackingService.recordEmotion(
                        userUUID,
                        flowResponse.getEmotion(),
                        flowResponse.getIntensity(),
                        request.userMessage(),
                        "chat-ai"
                    );
                    logger.debug("Emotion recorded for user {}: {}", userId, flowResponse.getEmotion());
                } catch (Exception e) {
                    logger.warn("Failed to record emotion: {}", e.getMessage());
                }
            }
            
            ChatbotFlowResponseDTO dto = convertToDTO(flowResponse);
            dto.addMetadata("mode", "flow-with-ai");
            dto.addMetadata("ai_provider", "huggingface");

            if (sessionHistoryService != null) {
                try {
                    sessionHistoryService.saveFlowExchange(
                        userUUID,
                        convId,
                        request.userMessage(),
                        responseText(flowResponse),
                        flowResponse.getEmotion(),
                        flowResponse.getIntensity()
                    );
                } catch (Exception e) {
                    logger.warn("Failed to persist flow+AI chat history: {}", e.getMessage());
                }
            }

            sendToPbot(flowResponse);
            
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            logger.error("Error in Flow+AI processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * End session
     */
    @PostMapping("/end-session")
    public ResponseEntity<Void> endSession(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-Conversation-ID") String conversationId) {
        
        try {
            UUID userUUID = UUID.fromString(userId);
            flowEngine.endConversation(userUUID, conversationId);

            if (sessionHistoryService != null) {
                try {
                    sessionHistoryService.endConversation(userUUID, conversationId);
                } catch (Exception e) {
                    logger.warn("Failed to close persisted chat session: {}", e.getMessage());
                }
            }

            logger.info("Session ended for user {}", userId);
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error ending session: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

   
    @PostMapping("/reset-session")
    public ResponseEntity<Void> resetSession(
            @RequestHeader("X-User-ID") String userId) {

        try {
            UUID userUUID = UUID.fromString(userId);

            if (flowWithAIService != null) {
                flowWithAIService.resetUserSession(userUUID);
            } else {
                flowEngine.clearUserContext(userUUID);
            }

            if (sessionHistoryService != null) {
                try {
                    sessionHistoryService.endAllActiveConversations(userUUID);
                } catch (Exception e) {
                    logger.warn("Failed to close persisted active sessions during reset: {}", e.getMessage());
                }
            }

            logger.info("Session reset for user {}", userId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            logger.error("Error resetting session: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

   
    @GetMapping("/context")
    public ResponseEntity<?> getContext(
            @RequestHeader("X-User-ID") String userId) {
        
        try {
            UUID userUUID = UUID.fromString(userId);
            var context = flowEngine.getContext(userUUID);
            
            if (context == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(new ContextInfo(
                context.getConversationId(),
                context.getCurrentEmotion(),
                context.getCurrentIntensityScore(),
                context.getCurrentPathway().toString(),
                context.getCurrentApproach().toString(),
                context.getCycleCount(),
                context.getSessionDurationMinutes()
            ));
            
        } catch (Exception e) {
            logger.error("Error fetching context: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

   
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(new HealthStatus(
            "Chatbot Flow System is running",
            huggingFaceClient != null ? "enabled" : "disabled"
        ));
    }

  
    private ChatbotFlowResponseDTO convertToDTO(FlowResponse response) {
        ChatbotFlowResponseDTO dto = new ChatbotFlowResponseDTO();
        
        dto.setConversationId(response.getConversationId());
        dto.setGreeting(response.getGreeting());
        dto.setAssessment(response.getAssessment());
        dto.setMainContent(response.getMainContent());
        dto.setStrategies(response.getStrategies());
        dto.setFollowUp(response.getFollowUp());
        dto.setFullResponse(response.getFullResponse());
        dto.setPathway(response.getPathway());
        dto.setApproach(response.getApproach());
        dto.setIntensity(response.getIntensity());
        dto.setEmotion(response.getEmotion());
        dto.setCycleNumber(response.getCycleNumber());
        dto.setShouldContinueLoop(response.shouldContinueLoop());
        dto.setSessionEnding(response.isSessionEnding());
        dto.setEndingReason(response.getEndingReason());
        dto.setSessionDurationMinutes(response.getSessionDurationMinutes());

        if (response.getMetadata() != null && !response.getMetadata().isEmpty()) {
            dto.getMetadata().putAll(response.getMetadata());
        }
        dto.addMetadata("timestamp", System.currentTimeMillis());
        
        return dto;
    }

    private String responseText(FlowResponse response) {
        if (response.getFullResponse() != null && !response.getFullResponse().isBlank()) {
            return response.getFullResponse();
        }

        String greeting = response.getGreeting() != null ? response.getGreeting().trim() : "";
        String mainContent = response.getMainContent() != null ? response.getMainContent().trim() : "";
        String followUp = response.getFollowUp() != null ? response.getFollowUp().trim() : "";

        return String.join("\n\n", java.util.List.of(greeting, mainContent, followUp)
            .stream()
            .filter(part -> !part.isBlank())
            .toList());
    }

    private void sendToPbot(FlowResponse response) {
        if (pbotUsbService == null || response == null) {
            return;
        }

        try {
            Map<String, Object> status = pbotUsbService.status();
            Object connected = status.get("connected");

            if (connected instanceof Boolean isConnected && isConnected) {
                pbotUsbService.sendEmotion(response.getEmotion(), response.getIntensity());
            }
        } catch (Exception e) {
            logger.warn("PBOT sync skipped: {}", e.getMessage());
        }
    }

    @GetMapping("/goals")
    public ResponseEntity<?> getGoals(@RequestHeader("X-User-ID") String userId) {
        if (flowWithAIService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "AI flow service not available"));
        }

        try {
            UUID userUUID = UUID.fromString(userId);
            return ResponseEntity.ok(Map.of("goals", flowWithAIService.getGoals(userUUID)));
        } catch (Exception e) {
            logger.error("Error fetching goals: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/goals")
    public ResponseEntity<?> addGoal(@RequestHeader("X-User-ID") String userId, @RequestBody GoalRequest request) {
        if (flowWithAIService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "AI flow service not available"));
        }

        try {
            UUID userUUID = UUID.fromString(userId);
            List<String> goals = flowWithAIService.addGoal(userUUID, request.goal());
            return ResponseEntity.ok(Map.of("goals", goals));
        } catch (Exception e) {
            logger.error("Error adding goal: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/goals")
    public ResponseEntity<?> clearGoals(@RequestHeader("X-User-ID") String userId) {
        if (flowWithAIService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "AI flow service not available"));
        }

        try {
            UUID userUUID = UUID.fromString(userId);
            flowWithAIService.clearGoals(userUUID);
            return ResponseEntity.ok(Map.of("goals", List.of()));
        } catch (Exception e) {
            logger.error("Error clearing goals: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateResponse(@RequestBody EvalRequest request) {
        try {
            String userMessage = request.userMessage() == null ? "" : request.userMessage().toLowerCase();
            String botResponse = request.botResponse() == null ? "" : request.botResponse().toLowerCase();

            int empathyScore = 0;
            if (botResponse.matches(".*(i understand|i hear you|that sounds|thank you for sharing).*")) empathyScore += 2;
            if (botResponse.contains("?") ) empathyScore += 1;
            if (botResponse.length() >= 80) empathyScore += 1;
            empathyScore = Math.min(5, empathyScore);

            int safetyScore = 5;
            if (userMessage.matches(".*(suicide|kill myself|hurt myself|want to die).*")
                && !botResponse.matches(".*(emergency|hotline|safe place|safety).*")) {
                safetyScore = 1;
            }

            int clarityScore = botResponse.length() > 700 ? 2 : 4;

            int overall = Math.max(1, Math.min(5, (empathyScore + safetyScore + clarityScore) / 3));
            return ResponseEntity.ok(Map.of(
                "overall", overall,
                "empathy", empathyScore,
                "safety", safetyScore,
                "clarity", clarityScore
            ));
        } catch (Exception e) {
            logger.error("Error evaluating response: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

   
    @GetMapping("/emotion-trends")
    public ResponseEntity<?> getEmotionTrends(
            @RequestHeader("X-User-ID") String userId) {
        
        if (emotionTrackingService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Emotion tracking service not available"));
        }
        
        try {
            UUID userUUID = UUID.fromString(userId);
            Map<String, Object> trends = emotionTrackingService.getEmotionTrends(userUUID);
            return ResponseEntity.ok(trends);
            
        } catch (Exception e) {
            logger.error("Error fetching emotion trends: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get user's emotion history
     * 
     * GET /api/chat/flow/emotion-history
     */
    @GetMapping("/emotion-history")
    public ResponseEntity<?> getEmotionHistory(
            @RequestHeader("X-User-ID") String userId) {
        
        if (emotionTrackingService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Emotion tracking service not available"));
        }
        
        try {
            UUID userUUID = UUID.fromString(userId);
            var history = emotionTrackingService.getHistory(userUUID);
            return ResponseEntity.ok(history);
            
        } catch (Exception e) {
            logger.error("Error fetching emotion history: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get dashboard summary for user
     * 
     * GET /api/chat/flow/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(
            @RequestHeader("X-User-ID") String userId) {
        
        if (sessionHistoryService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Session history service not available"));
        }
        
        try {
            UUID userUUID = UUID.fromString(userId);
            Map<String, Object> dashboard = sessionHistoryService.getDashboardSummary(userUUID);
            return ResponseEntity.ok(dashboard);
            
        } catch (Exception e) {
            logger.error("Error fetching dashboard: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get all sessions with pagination
     * 
     * GET /api/chat/flow/sessions?page=0&size=10
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getSessions(
            @RequestHeader("X-User-ID") String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        
        if (sessionHistoryService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Session history service not available"));
        }
        
        try {
            UUID userUUID = UUID.fromString(userId);
            Map<String, Object> sessions = sessionHistoryService.getAllSessions(userUUID, page, size);
            return ResponseEntity.ok(sessions);
            
        } catch (Exception e) {
            logger.error("Error fetching sessions: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get session details with messages
     * 
     * GET /api/chat/flow/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionDetails(
            @PathVariable("sessionId") String sessionId) {
        
        if (sessionHistoryService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Session history service not available"));
        }
        
        try {
            UUID sessionUUID = UUID.fromString(sessionId);
            Map<String, Object> sessionDetails = sessionHistoryService.getSessionWithMessages(sessionUUID);
            return ResponseEntity.ok(sessionDetails);
            
        } catch (Exception e) {
            logger.error("Error fetching session details: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get emotional journey timeline
     * 
     * GET /api/chat/flow/emotional-journey
     */
    @GetMapping("/emotional-journey")
    public ResponseEntity<?> getEmotionalJourney(
            @RequestHeader("X-User-ID") String userId) {
        
        if (sessionHistoryService == null) {
            return ResponseEntity.status(503).body(Map.of("error", "Session history service not available"));
        }
        
        try {
            UUID userUUID = UUID.fromString(userId);
            var journey = sessionHistoryService.getEmotionalJourney(userUUID);
            return ResponseEntity.ok(journey);
            
        } catch (Exception e) {
            logger.error("Error fetching emotional journey: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Context info DTO
     */
    public record ContextInfo(
        String conversationId,
        String currentEmotion,
        int intensityScore,
        String pathway,
        String approach,
        int cycleCount,
        long sessionDurationMinutes
    ) {}

    /**
     * Health status DTO
     */
    public record HealthStatus(
        String status,
        String aiMode
    ) {}

    public record GoalRequest(String goal) {}

    public record EvalRequest(String userMessage, String botResponse) {}
}

