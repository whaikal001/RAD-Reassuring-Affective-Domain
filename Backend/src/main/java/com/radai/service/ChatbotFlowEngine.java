package com.radai.service;

import com.radai.model.MonitoringContext;
import com.radai.model.FlowResponse;
import com.radai.enums.PathwayType;
import com.radai.enums.ApproachType;
import com.radai.enums.IntensityLevel;
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
    private final com.radai.service.empathy.ApproachSwitchPolicy approachPolicy =
        new com.radai.service.empathy.ApproachSwitchPolicy();
    private final com.radai.service.insight.SessionInsightEngine sessionInsightEngine =
        new com.radai.service.insight.SessionInsightEngine();

    // Session management
    private final Map<UUID, MonitoringContext> sessionContexts = new ConcurrentHashMap<>();
    private final Map<String, UUID> conversationIdToUserId = new ConcurrentHashMap<>();

    public ChatbotFlowEngine(String language) {
        this.language = language == null ? "en" : language;
        this.screeningService = new MonitoringAndScreeningService();
        this.templateService = new ResponseTemplateService();
        this.loopManager = new LoopManager(screeningService, templateService);
    }

    /**
     * Process a user message through the complete flow
     * This is the main entry point for the chatbot interaction
     */
    public FlowResponse processUserMessage(UUID userId, String conversationId, String userMessage, int intensityScore) {
        return processUserMessage(userId, conversationId, userMessage, intensityScore, null);
    }

    /**
     * Process a user message, supplying the DASS screening band as the stable baseline for the
     * empathy&lt;-&gt;sympathy switch. {@code dassBand} may be null when screening is unknown.
     */
    public FlowResponse processUserMessage(UUID userId, String conversationId, String userMessage,
                                           int intensityScore, String dassBand) {
        try {
            logger.info("Processing message for user {} in conversation {}", userId, conversationId);

            // Get or create monitoring context
            MonitoringContext context = getOrCreateContext(userId, conversationId);

            // Capture the screening band once as the session baseline for approach switching.
            if (dassBand != null && !dassBand.isBlank() && context.getStressBand() == null) {
                context.setStressBand(dassBand);
            }

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

            // Step 6: Generate main content (AI-first)
            String mainContent = null;
            try {
                HuggingFaceResponseEnhancer aiEnhancer = new HuggingFaceResponseEnhancer(new com.radai.chat.hf.HuggingFaceClient());
                mainContent = aiEnhancer.generateHumanLikeResponse(userMessage, context, approach, language);
            } catch (Exception ex) {
                logger.warn("AI enhancer not available or failed: {} - falling back to templates", ex.getMessage());
                mainContent = templateService.generateMainContent(context, userMessage, approach);
            }
            response.withMainContent(mainContent);

            // Step 7: Generate strategies (AI-first)
            String strategies = null;
            try {
                HuggingFaceResponseEnhancer aiEnhancer2 = new HuggingFaceResponseEnhancer(new com.radai.chat.hf.HuggingFaceClient());
                // Use a short instruction to generate strategies based on context
                String strategyPrompt = "Provide 2-3 concise, practical strategies for the user's situation based on recent context: " + (context.getLastUserMessage() == null ? userMessage : context.getLastUserMessage());
                strategies = aiEnhancer2.generateHumanLikeResponse(strategyPrompt, context, ApproachType.SYMPATHY, language);
            } catch (Exception ex) {
                logger.warn("AI strategies generation failed: {} - using template", ex.getMessage());
                strategies = templateService.generateStrategies(context);
            }
            response.withStrategies(strategies);

            // Step 8: Execute loop logic
            LoopManager.LoopDecision decision = loopManager.makeLoopDecision(context);
            loopManager.executeLoopDecision(context, decision);

            logger.info("Loop decision: {} - {}", decision.getAction(), decision.getReason());

            // Step 9: Build final response with follow-up or closure
            // Build follow-up or closure using AI if possible
            String followUpText = null;
            try {
                HuggingFaceResponseEnhancer ai = new HuggingFaceResponseEnhancer(new com.radai.chat.hf.HuggingFaceClient());
                switch (decision.getAction()) {
                    case EXIT_LOOP:
                        followUpText = ai.generateHumanLikeResponse("Provide a concise session closure: reason=" + decision.getReason(), context, ApproachType.SYMPATHY, language);
                        response.withSessionEnd(true, decision.getReason());
                        break;
                    case SWITCH_APPROACH:
                        followUpText = ai.generateHumanLikeResponse("Switch approach message for approach=" + context.getCurrentApproach() + ". Then ask one short follow-up question.", context, ApproachType.SYMPATHY, language);
                        response.withLoopContinuation(true);
                        break;
                    case ESCALATE_PATHWAY:
                        followUpText = ai.generateHumanLikeResponse("Pathway escalation message and one action step to escalate care.", context, ApproachType.SYMPATHY, language);
                        response.withLoopContinuation(true);
                        break;
                    default:
                        followUpText = ai.generateHumanLikeResponse("Write one brief follow-up question to continue the conversation.", context, ApproachType.EMPATHY, language);
                        response.withLoopContinuation(decision.getAction() == LoopManager.LoopAction.CONTINUE_LOOP);
                        break;
                }
            } catch (Exception ex) {
                logger.warn("AI follow-up generation failed: {} - using templates", ex.getMessage());
                // fallback to template behavior
                if (decision.getAction() == LoopManager.LoopAction.EXIT_LOOP) {
                    followUpText = templateService.generateSessionClosure(context, decision.getReason());
                    response.withSessionEnd(true, decision.getReason());
                } else if (decision.getAction() == LoopManager.LoopAction.SWITCH_APPROACH) {
                    String switchMessage = templateService.generateApproachSwitchMessage(context.getCurrentApproach());
                    followUpText = switchMessage + "\n\n" + templateService.generateFollowUp(context);
                    response.withLoopContinuation(true);
                } else if (decision.getAction() == LoopManager.LoopAction.ESCALATE_PATHWAY) {
                    String pathwaySwitch = templateService.generatePathwaySwitchMessage();
                    followUpText = pathwaySwitch + "\n\n" + templateService.generateFollowUp(context);
                    response.withLoopContinuation(true);
                } else {
                    followUpText = templateService.generateFollowUp(context);
                    response.withLoopContinuation(decision.getAction() == LoopManager.LoopAction.CONTINUE_LOOP);
                }
            }

            response.withFollowUp(followUpText);

            // Step 10: Add response metadata
            response.withPathway(context.getCurrentPathway());
            response.withApproach(context.getCurrentApproach());
            response.withIntensity(context.getCurrentIntensityScore());
            response.withEmotion(context.getCurrentEmotion());
            response.withCycleNumber(context.getCycleCount());
            response.withSessionDuration(context.getSessionDurationMinutes());

            // Add localized crisis resources + safety framing.
            response.withMetadata("helpBanner", com.radai.service.support.CrisisResources.helpBanner(language));
            response.withMetadata("disclaimer", com.radai.service.support.CrisisResources.disclaimer(language));
            if (context.isCrisisDetected() || context.isSuicidalIdeationDetected()) {
                response.withMetadata("helpline", com.radai.service.support.CrisisResources.primaryHelpline());
                response.withMetadata("crisisResources", com.radai.service.support.CrisisResources.forLanguage(language));
                response.withMetadata("safetyLevel", "critical");
            } else {
                response.withMetadata("safetyLevel", "low");
            }

            // Within-session progress insight (SessionInsightEngine).
            com.radai.service.insight.SessionInsightEngine.SessionInsight insight = sessionInsightEngine.summarize(
                context.getFirstIntensityScore(), context.getCurrentIntensityScore(), context.getCycleCount(),
                context.getSwitchingReasons().size(),
                context.isCrisisDetected() || context.isSuicidalIdeationDetected(),
                context.getCurrentEmotion(), language);
            response.withMetadata("sessionInsight", Map.of(
                "status", insight.status().name(),
                "improvementPct", insight.improvementPct(),
                "summary", insight.summary(),
                "recommendation", insight.recommendation()));

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
     * Determine the approach (EMPATHY vs SYMPATHY) for this turn from the stress/intensity signal.
     *
     * <p>Delegates to {@link com.radai.service.empathy.ApproachSwitchPolicy}: HIGH stress/intensity
     * &rarr; SYMPATHY (practical, step-in guidance), LOW &rarr; EMPATHY (gentle presence), with
     * hysteresis so the mode does not flip-flop. The session always opens in EMPATHY, and any
     * detected crisis forces SYMPATHY (highest support).
     */
    private ApproachType determineApproach(MonitoringContext context) {
        // cycleCount is still 0 on the first user turn (LoopManager increments it later this cycle).
        boolean firstTurn = context.getCycleCount() == 0;
        boolean crisis = context.isCrisisDetected() || context.isSuicidalIdeationDetected();

        com.radai.service.empathy.ApproachSwitchPolicy.Decision decision = approachPolicy.decide(
            context.getCurrentApproach(), firstTurn, crisis,
            context.getStressBand(), context.getCurrentIntensityScore());

        if (decision.switched()) {
            logger.info("Approach switch {} -> {} | {}",
                context.getCurrentApproach(), decision.approach(), decision.reason());
        } else {
            logger.debug("Approach kept {} | {}", decision.approach(), decision.reason());
        }
        return decision.approach();
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

