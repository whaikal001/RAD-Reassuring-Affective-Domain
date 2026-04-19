package com.SocializerAI.service;

import com.SocializerAI.model.MonitoringContext;
import com.SocializerAI.model.FlowResponse;
import com.SocializerAI.enums.PathwayType;
import com.SocializerAI.enums.ApproachType;
import com.SocializerAI.enums.IntensityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatbotFlowEngine - Main orchestrator for the Mental Health Chatbot Flow System
 * 
 * Manages:
 * 1. Time-based greetings
 * 2. Monitoring & screening assessment
 * 3. Pathway determination (Prevention/Intervention)
 * 4. Approach selection (Empathy/Sympathy)
 * 5. Main conversation loop with state management
 * 6. Dynamic switching based on user response
 * 7. Crisis escalation
 * 
 * Supports all 10 scenarios defined in the specification.
 */
public class ChatbotFlowEngine {
    private static final Logger logger = LoggerFactory.getLogger(ChatbotFlowEngine.class);

    // Services
    private final MonitoringAndScreeningService screeningService;
    private final ResponseTemplateService templateService;
    private final LoopManager loopManager;
    private final String language; // en, ms, etc.
    private final com.SocializerAI.service.config.AppConfig appConfig;

    // Session management
    private final Map<UUID, MonitoringContext> sessionContexts = new ConcurrentHashMap<>();
    private final Map<String, UUID> conversationIdToUserId = new ConcurrentHashMap<>();

    public ChatbotFlowEngine(String language) {
        this.language = language == null ? "en" : language;
        this.screeningService = new MonitoringAndScreeningService();
        this.templateService = new ResponseTemplateService();
        this.loopManager = new LoopManager(screeningService, templateService);
        this.appConfig = com.SocializerAI.service.config.AppConfig.load();
    }

    /**
     * Process a user message through the complete flow
     * This is the main entry point for the chatbot interaction
     */
    public FlowResponse processUserMessage(UUID userId, String conversationId, String userMessage, int intensityScore) {
        try {
            logger.info("Processing message for user {} in conversation {}", userId, conversationId);

            // Get or create monitoring context
            MonitoringContext context = getOrCreateContext(userId, conversationId);

            // Step 1: If first message in conversation, send greeting
            FlowResponse response = new FlowResponse(conversationId);
            
            if (context.getCycleCount() == 0) {
                response.withGreeting(GreetingService.generateTimeBasedGreeting(language));
            }

            // Step 2: Assess emotional state
            screeningService.assessEmotionalState(context, userMessage, intensityScore);
            context.addUserMessage(userMessage);

            // Step 3: Determine pathway
            PathwayType pathway = screeningService.determinePathway(context);
            context.setCurrentPathway(pathway);

            // Step 4: Determine approach (Empathy vs Sympathy)
            ApproachType approach = determineApproach(context);
            context.setCurrentApproach(approach);

            // Step 5: Generate assessment message
            String assessment = generateAssessment(context);
            response.withAssessment(assessment);

            // Step 6: Generate main content
            String mainContent = templateService.generateMainContent(context, userMessage, approach);
            response.withMainContent(mainContent);

            // Step 7: Generate strategies
            String strategies = templateService.generateStrategies(context);
            response.withStrategies(strategies);

            // Step 8: Execute loop logic
            LoopManager.LoopDecision decision = loopManager.makeLoopDecision(context);
            loopManager.executeLoopDecision(context, decision);

            logger.info("Loop decision: {} - {}", decision.getAction(), decision.getReason());

            // Step 9: Build final response with follow-up or closure
            if (decision.getAction() == LoopManager.LoopAction.EXIT_LOOP) {
                String closure = templateService.generateSessionClosure(context, decision.getReason());
                response.withFollowUp(closure);
                response.withSessionEnd(true, decision.getReason());
            } else if (decision.getAction() == LoopManager.LoopAction.SWITCH_APPROACH) {
                String switchMessage = templateService.generateApproachSwitchMessage(context.getCurrentApproach());
                response.withFollowUp(switchMessage + "\n\n" + templateService.generateFollowUp(context));
                response.withLoopContinuation(true);
            } else if (decision.getAction() == LoopManager.LoopAction.ESCALATE_PATHWAY) {
                String pathwaySwitch = templateService.generatePathwaySwitchMessage();
                response.withFollowUp(pathwaySwitch + "\n\n" + templateService.generateFollowUp(context));
                response.withLoopContinuation(true);
            } else {
                response.withFollowUp(templateService.generateFollowUp(context));
                response.withLoopContinuation(decision.getAction() == LoopManager.LoopAction.CONTINUE_LOOP);
            }

            // Step 10: Add response metadata
            response.withPathway(context.getCurrentPathway());
            response.withApproach(context.getCurrentApproach());
            response.withIntensity(context.getCurrentIntensityScore());
            response.withEmotion(context.getCurrentEmotion());
            response.withCycleNumber(context.getCycleCount());
            response.withSessionDuration(context.getSessionDurationMinutes());

            // Add helpline metadata if crisis detected
            if (context.isCrisisDetected() || context.isSuicidalIdeationDetected()) {
                response.withMetadata("helpline", appConfig.getDefaultHelpline());
                response.withMetadata("safetyLevel", "critical");
            } else {
                response.withMetadata("safetyLevel", "low");
            }

            // Store response for tracking
            context.addBotResponse(response.getFullResponse());

            // Detect user engagement for next cycle
            boolean engagement = loopManager.detectPositiveEngagement(userMessage);
            context.setUserResponding(engagement);
            
            boolean improving = loopManager.calculateImprovementScore(context) > 0;
            context.setUserImproving(improving);

            return response;

        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            return createErrorResponse(conversationId, userMessage);
        }
    }

    /**
     * Determine appropriate approach based on context
     * 
     * EMPATHY → SYMPATHY only when:
     * - User receives empathy-based advice but CAN'T follow it
     * - User shows struggle/inability to implement suggestions
     * - User needs step-by-step guidance instead
     * 
     * Stays EMPATHY when:
     * - First cycle (always empathy first)
     * - User is following advice well
     * - User is improving with current approach
     */
    private ApproachType determineApproach(MonitoringContext context) {
        // Log context snapshot to aid debugging approach selection
        logger.debug("DetermineApproach - cycleCount={}, currentApproach={}, intensity={}, intensityLevel={}, approachSwitchHistory={}, messagesWithoutImprovement={}, userStruggling={}",
            context.getCycleCount(), context.getCurrentApproach(), context.getCurrentIntensityScore(), context.getCurrentIntensityLevel(), context.getApproachSwitchHistory(), context.getMessagesWithoutImprovement());

        // Start with EMPATHY by default (first cycle)
        if (context.getCycleCount() == 1) {
            logger.debug("determineApproach -> choosing EMPATHY (first cycle - always start with empathy)");
            return ApproachType.EMPATHY;
        }

        // Stay with EMPATHY until we detect user can't follow the advice
        ApproachType currentApproach = context.getCurrentApproach();
        
        // Check if user is struggling to follow empathy-based advice
        if (currentApproach == ApproachType.EMPATHY && userIsStruggling(context)) {
            logger.info("User struggling with empathy-based advice -> SWITCH to SYMPATHY mode for practical guidance");
            return ApproachType.SYMPATHY;
        }

        // If already in SYMPATHY and user is improving, stay in SYMPATHY
        if (currentApproach == ApproachType.SYMPATHY && context.isUserImproving()) {
            logger.debug("User improving with sympathy approach - continue SYMPATHY");
            return ApproachType.SYMPATHY;
        }

        // If user is improving well with EMPATHY, keep it
        if (currentApproach == ApproachType.EMPATHY && context.isUserImproving()) {
            logger.debug("User improving with empathy approach - continue EMPATHY");
            return ApproachType.EMPATHY;
        }

        // Default: keep current approach
        logger.debug("determineApproach -> keeping current approach: {}", currentApproach);
        return currentApproach;
    }

    /**
     * Detect if user is struggling to follow empathy-based guidance
     * Signs: "can't do it", "too hard", "don't know how", "still overwhelmed", repeated failures
     */
    private boolean userIsStruggling(MonitoringContext context) {
        // Check messages without improvement - if 2+ empathy responses haven't helped
        if (context.getMessagesWithoutImprovement() >= 2) {
            String userLastMessage = context.getLastUserMessage();
            if (userLastMessage != null) {
                String lower = userLastMessage.toLowerCase();
                
                // Signs of struggling with advice
                if (lower.matches(".*(can't|cannot|can't do|don't know how|too hard|difficult|impossible|still feel|not working|doesn't help|struggling|confused|don't understand).*")) {
                    logger.info("User showing struggle signals: can't follow empathy advice");
                    return true;
                }
            }
            return true; // If 2+ messages without improvement, likely struggling
        }
        return false;
    }

    /**
     * Generate assessment message based on current state
     */
    private String generateAssessment(MonitoringContext context) {
        IntensityLevel level = context.getCurrentIntensityLevel();
        String emotion = context.getCurrentEmotion();
        String stressor = context.getDominantStressor();

        StringBuilder assessment = new StringBuilder();
        assessment.append("**Current Assessment:**\n");
        assessment.append("Emotion: ").append(emotion).append(" | ");
        assessment.append("Intensity: ").append(context.getCurrentIntensityScore()).append("/10").append("\n");

        if (stressor != null && !stressor.equals("general")) {
            assessment.append("Main Stressor: ").append(stressor).append("\n");
        }

        assessment.append("Pathway: ").append(context.getCurrentPathway().getName()).append("\n");
        assessment.append("Cycle: ").append(context.getCycleCount());

        return assessment.toString();
    }

    /**
     * Create error response
     */
    private FlowResponse createErrorResponse(String conversationId, String userMessage) {
        FlowResponse response = new FlowResponse(conversationId);
        response.withGreeting("I appreciate you reaching out. Let me help you better.");
        response.withMainContent("I encountered a small issue processing your message, but I'm still here to support you.");
        response.withFollowUp("Could you tell me a bit more about how you're feeling?");
        return response;
    }

    /**
     * Get or create monitoring context for a user
     */
    private MonitoringContext getOrCreateContext(UUID userId, String conversationId) {
        MonitoringContext context = sessionContexts.get(userId);
        
        if (context == null) {
            context = new MonitoringContext(userId, conversationId);
            sessionContexts.put(userId, context);
            conversationIdToUserId.put(conversationId, userId);
            logger.info("Created new monitoring context for user {}", userId);
        } else {
            // Update conversation ID if different (new conversation)
            if (!context.getConversationId().equals(conversationId)) {
                context = new MonitoringContext(userId, conversationId);
                sessionContexts.put(userId, context);
                logger.info("Created new conversation context for user {}", userId);
            }
        }
        
        return context;
    }

    /**
     * End a conversation session
     */
    public void endConversation(UUID userId, String conversationId) {
        MonitoringContext context = sessionContexts.get(userId);
        if (context != null) {
            logger.info("Ending conversation {} for user {} after {} minutes", 
                conversationId, userId, context.getSessionDurationMinutes());
            sessionContexts.remove(userId);
            conversationIdToUserId.remove(conversationId);
        }
    }

    /**
     * Get current context for a user (for debugging/inspection)
     */
    public MonitoringContext getContext(UUID userId) {
        return sessionContexts.get(userId);
    }

    /**
     * Force clear user context regardless of conversation id.
     */
    public void clearUserContext(UUID userId) {
        MonitoringContext removed = sessionContexts.remove(userId);
        if (removed != null && removed.getConversationId() != null) {
            conversationIdToUserId.remove(removed.getConversationId());
        }
    }

    /**
     * Get all active sessions (for admin/monitoring)
     */
    public Map<UUID, MonitoringContext> getAllActiveSessions() {
        return new HashMap<>(sessionContexts);
    }

    /**
     * Clear all sessions
     */
    public void clearAllSessions() {
        sessionContexts.clear();
        conversationIdToUserId.clear();
        logger.info("All sessions cleared");
    }
}
