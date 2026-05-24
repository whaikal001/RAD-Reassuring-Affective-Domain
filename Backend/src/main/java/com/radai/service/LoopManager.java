package com.radai.service;

import com.radai.model.MonitoringContext;
import com.radai.enums.ApproachType;
import com.radai.enums.PathwayType;
import com.radai.enums.IntensityLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loop mechanism handler for the chatbot flow system.
 * Manages the main conversation loop, detecting when to continue, switch, escalate, or exit.
 */
public class LoopManager {
    private static final Logger logger = LoggerFactory.getLogger(LoopManager.class);

    private final MonitoringAndScreeningService screeningService;
    private final ResponseTemplateService templateService;

    public LoopManager(MonitoringAndScreeningService screeningService, 
                       ResponseTemplateService templateService) {
        this.screeningService = screeningService;
        this.templateService = templateService;
    }

    /**
     * Main loop decision logic
     * Determines whether to continue, switch approaches, escalate, or exit
     */
    public LoopDecision makeLoopDecision(MonitoringContext context) {
        context.incrementCycle();
        logger.info("Loop cycle {} - User intensity: {}, Pathway: {}, Approach: {}", 
            context.getCycleCount(), 
            context.getCurrentIntensityScore(), 
            context.getCurrentPathway(), 
            context.getCurrentApproach());

        // Check exit conditions first
        if (shouldExitLoop(context)) {
            return new LoopDecision(LoopAction.EXIT_LOOP, getExitReason(context));
        }

        // Check if max cycles reached
        if (context.isMaxCyclesReached()) {
            logger.warn("Max cycles reached for user {}. Recommending professional help.", context.getUserId());
            return new LoopDecision(LoopAction.ESCALATE_PROFESSIONAL, 
                "Extended conversation with limited improvement - professional help recommended");
        }

        // Check if user is improving
        if (context.isUserImproving() && context.getCurrentIntensityScore() <= 4) {
            logger.info("User improving - continue current approach");
            context.resetMessagesWithoutImprovement();
            return new LoopDecision(LoopAction.CONTINUE_LOOP, "User is improving, continue current approach");
        }

        // Check if user is not responding to current approach
        if (!context.isUserResponding() || context.getMessagesWithoutImprovement() >= 2) {
            logger.info("User not responding to current approach - attempting switch");
            context.incrementMessagesWithoutImprovement();
            return new LoopDecision(LoopAction.SWITCH_APPROACH, "User not responding, switch approach");
        }

        // Check if escalation from Prevention to Intervention is needed
        if (context.getCurrentPathway() == PathwayType.PREVENTION && context.hasEscalated()) {
            logger.info("Intensity escalated - switching to Intervention");
            return new LoopDecision(LoopAction.ESCALATE_PATHWAY, "Intensity increased, escalate to intervention");
        }

        // Check for crisis indicators
        if (context.isSuicidalIdeationDetected() || context.isCrisisDetected()) {
            logger.warn("Crisis detected for user {} - escalating", context.getUserId());
            return new LoopDecision(LoopAction.ESCALATE_CRISIS, "Crisis indicators detected");
        }

        // Default: continue with current approach
        logger.info("Continuing loop with current approach");
        return new LoopDecision(LoopAction.CONTINUE_LOOP, "Continue with current approach");
    }

    /**
     * Determine if loop should exit
     */
    private boolean shouldExitLoop(MonitoringContext context) {
        // User is stable and feeling okay
        if (context.getCurrentIntensityLevel() == IntensityLevel.LOW && 
            context.isUserImproving() && 
            context.getCycleCount() >= 2) {
            return true;
        }

        // User has a coping plan and is stable
        if (context.getCurrentIntensityScore() <= 3 && context.getCycleCount() >= 3) {
            return true;
        }

        // Session duration exceeded (more than 30 minutes)
        if (context.getSessionDurationMinutes() > 30) {
            return true;
        }

        return false;
    }

    /**
     * Get the exit reason for session closure
     */
    private String getExitReason(MonitoringContext context) {
        if (context.isUserImproving() && context.getCurrentIntensityScore() <= 4) {
            return "improved";
        } else if (context.getCurrentIntensityScore() <= 3) {
            return "stable";
        } else if (context.hasEscalated()) {
            return "escalated";
        }
        return "session_complete";
    }

    /**
     * Execute the loop decision
     */
    public void executeLoopDecision(MonitoringContext context, LoopDecision decision) {
        switch (decision.getAction()) {
            case CONTINUE_LOOP:
                logger.debug("Continuing loop with current configuration");
                break;
                
            case SWITCH_APPROACH:
                ApproachType newApproach = context.getCurrentApproach().opposite();
                context.setCurrentApproach(newApproach);
                context.addSwitchingReason("User not responding to " + context.getCurrentApproach());
                logger.info("Switched approach to {}", newApproach);
                break;
                
            case ESCALATE_PATHWAY:
                context.setCurrentPathway(PathwayType.INTERVENTION);
                context.setCurrentApproach(ApproachType.EMPATHY); // Reset to empathy for new pathway
                context.addSwitchingReason("Intensity escalated, pathway switched to intervention");
                logger.info("Escalated pathway to INTERVENTION");
                break;
                
            case EXIT_LOOP:
                logger.info("Exiting loop - reason: {}", decision.getReason());
                break;
                
            case ESCALATE_CRISIS:
                logger.error("Crisis escalation for user {}", context.getUserId());
                break;
                
            case ESCALATE_PROFESSIONAL:
                logger.warn("Escalating to professional help for user {}", context.getUserId());
                break;
        }
    }

    /**
     * Check if user response indicates lack of engagement
     */
    public boolean detectLackOfEngagement(String userResponse) {
        String lower = userResponse.toLowerCase().trim();
        
        // Short, non-committal responses
        if (lower.matches("^(ok|okay|sure|fine|whatever|idk|dunno|no|nope|yeah|yep)$")) {
            return true;
        }
        
        // Resistance indicators
        if (lower.contains("don't care") || lower.contains("doesn't matter") || 
            lower.contains("i don't know") || lower.contains("not helping")) {
            return true;
        }
        
        return false;
    }

    /**
     * Check if user response indicates positive engagement
     */
    public boolean detectPositiveEngagement(String userResponse) {
        String lower = userResponse.toLowerCase();
        
        // Positive indicators
        if (lower.matches(".*(better|good|thanks|helpful|helped|feel better|helped me).*")) {
            return true;
        }
        
        // Specific action indicators
        if (lower.matches(".*(tried|doing|going to|will|plan to|starting).*")) {
            return true;
        }
        
        return false;
    }

    /**
     * Calculate user improvement score based on intensity change
     */
    public double calculateImprovementScore(MonitoringContext context) {
        int previousScore = context.getPreviousIntensityScore();
        int currentScore = context.getCurrentIntensityScore();
        
        if (previousScore == 0) return 0.0; // No previous score
        
        return ((previousScore - currentScore) / (double) previousScore) * 100.0;
    }

    /**
     * Enum for loop actions
     */
    public enum LoopAction {
        CONTINUE_LOOP,
        SWITCH_APPROACH,
        ESCALATE_PATHWAY,
        EXIT_LOOP,
        ESCALATE_CRISIS,
        ESCALATE_PROFESSIONAL
    }

    /**
     * Loop decision data class
     */
    public static class LoopDecision {
        private final LoopAction action;
        private final String reason;

        public LoopDecision(LoopAction action, String reason) {
            this.action = action;
            this.reason = reason;
        }

        public LoopAction getAction() {
            return action;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            return "LoopDecision{" +
                    "action=" + action +
                    ", reason='" + reason + '\'' +
                    '}';
        }
    }
}

