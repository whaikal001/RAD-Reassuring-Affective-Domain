package com.SocializerAI.controller;

import com.SocializerAI.service.ChatbotFlowEngine;
import com.SocializerAI.model.FlowResponse;
import com.SocializerAI.dto.ChatbotFlowRequest;
import com.SocializerAI.dto.ChatbotFlowResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

public class ChatbotFlowController {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotFlowController.class);

    private final ChatbotFlowEngine flowEngine;

    public ChatbotFlowController() {
        // Initialize flow engine with default language (English)
        this.flowEngine = new ChatbotFlowEngine("en");
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
            
            logger.info("Processing message for user {} in conversation {}", userId, convId);
            
            // Create flow engine for this language
            ChatbotFlowEngine engine = new ChatbotFlowEngine(language);
            
            // Process through flow engine
            FlowResponse flowResponse = engine.processUserMessage(
                userUUID,
                convId,
                request.userMessage(),
                request.intensityScore()
            );
            
            // Convert to DTO
            ChatbotFlowResponseDTO dto = convertToDTO(flowResponse);
            
            logger.info("Response generated successfully for user {}", userId);
            
            return ResponseEntity.ok(dto);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid user ID format: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    
    @PostMapping("/end-session")
    public ResponseEntity<Void> endSession(
            @RequestHeader("X-User-ID") String userId,
            @RequestHeader("X-Conversation-ID") String conversationId) {
        
        try {
            UUID userUUID = UUID.fromString(userId);
            flowEngine.endConversation(userUUID, conversationId);
            
            logger.info("Session ended for user {} in conversation {}", userId, conversationId);
            
            return ResponseEntity.ok().build();
            
        } catch (Exception e) {
            logger.error("Error ending session: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get current conversation context for debugging
     * 
     * GET /api/chat/flow/context
     */
    @GetMapping("/context")
    public ResponseEntity<?> getContext(
            @RequestHeader("X-User-ID") String userId) {
        
        try {
            UUID userUUID = UUID.fromString(userId);
            var context = flowEngine.getContext(userUUID);
            
            if (context == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Return simplified context info
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

    /**
     * Health check for flow engine
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Chatbot Flow Engine is running");
    }

    /**
     * Convert FlowResponse to DTO
     */
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
        
        // Add metadata
        dto.addMetadata("timestamp", System.currentTimeMillis());
        
        return dto;
    }

    /**
     * Simple context info DTO for debugging
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
}
