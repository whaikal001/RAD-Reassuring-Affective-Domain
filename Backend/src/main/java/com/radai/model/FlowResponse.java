package com.radai.model;

import com.radai.enums.ApproachType;
import com.radai.enums.PathwayType;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalTime;

/**
 * Response for the chatbot flow system.
 * Contains the full response including greeting, assessment, pathway, approach, content, and follow-up.
 */
public class FlowResponse {
    private String conversationId;
    private String greeting;
    private String assessment;
    private String mainContent;
    private String strategies;
    private String followUp;
    private PathwayType pathway;
    private ApproachType approach;
    private int intensity;
    private String emotion;
    private int cycleNumber;
    private boolean shouldContinueLoop;
    private boolean isSessionEnding;
    private String endingReason;
    private long sessionDurationMinutes;
    private Map<String, Object> metadata = new HashMap<>();

    public FlowResponse(String conversationId) {
        this.conversationId = conversationId;
        this.pathway = PathwayType.PREVENTION;
        this.approach = ApproachType.EMPATHY;
    }

    // Builder pattern for convenience
    public FlowResponse withGreeting(String greeting) {
        this.greeting = greeting;
        return this;
    }

    public FlowResponse withAssessment(String assessment) {
        this.assessment = assessment;
        return this;
    }

    public FlowResponse withMainContent(String content) {
        this.mainContent = content;
        return this;
    }

    public FlowResponse withStrategies(String strategies) {
        this.strategies = strategies;
        return this;
    }

    public FlowResponse withFollowUp(String followUp) {
        this.followUp = followUp;
        return this;
    }

    public FlowResponse withPathway(PathwayType pathway) {
        this.pathway = pathway;
        return this;
    }

    public FlowResponse withApproach(ApproachType approach) {
        this.approach = approach;
        return this;
    }

    public FlowResponse withIntensity(int intensity) {
        this.intensity = intensity;
        return this;
    }

    public FlowResponse withEmotion(String emotion) {
        this.emotion = emotion;
        return this;
    }

    public FlowResponse withCycleNumber(int cycle) {
        this.cycleNumber = cycle;
        return this;
    }

    public FlowResponse withLoopContinuation(boolean shouldContinue) {
        this.shouldContinueLoop = shouldContinue;
        return this;
    }

    public FlowResponse withSessionEnd(boolean ending, String reason) {
        this.isSessionEnding = ending;
        this.endingReason = reason;
        return this;
    }

    public FlowResponse withSessionDuration(long minutes) {
        this.sessionDurationMinutes = minutes;
        return this;
    }

    public FlowResponse withMetadata(String key, Object value) {
        this.metadata.put(key, value);
        return this;
    }

    public FlowResponse withMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }

    // Getters
    public String getConversationId() { return conversationId; }
    public String getGreeting() { return greeting; }
    public String getAssessment() { return assessment; }
    public String getMainContent() { return mainContent; }
    public String getStrategies() { return strategies; }
    public String getFollowUp() { return followUp; }
    public PathwayType getPathway() { return pathway; }
    public ApproachType getApproach() { return approach; }
    public int getIntensity() { return intensity; }
    public String getEmotion() { return emotion; }
    public int getCycleNumber() { return cycleNumber; }
    public boolean shouldContinueLoop() { return shouldContinueLoop; }
    public boolean isSessionEnding() { return isSessionEnding; }
    public String getEndingReason() { return endingReason; }
    public long getSessionDurationMinutes() { return sessionDurationMinutes; }
    public Map<String, Object> getMetadata() { return metadata; }

    /**
     * Get the full response text combining all components
     */
    public String getFullResponse() {
        StringBuilder response = new StringBuilder();
        
        if (greeting != null && !greeting.isEmpty()) {
            response.append(greeting).append("\n\n");
        }
        
        if (assessment != null && !assessment.isEmpty()) {
            response.append(assessment).append("\n\n");
        }
        
        if (mainContent != null && !mainContent.isEmpty()) {
            response.append(mainContent).append("\n\n");
        }
        
        if (strategies != null && !strategies.isEmpty()) {
            response.append(strategies).append("\n\n");
        }
        
        if (followUp != null && !followUp.isEmpty()) {
            response.append(followUp);
        }
        
        return response.toString().trim();
    }

    @Override
    public String toString() {
        return "FlowResponse{" +
                "conversationId='" + conversationId + '\'' +
                ", pathway=" + pathway +
                ", approach=" + approach +
                ", intensity=" + intensity +
                ", emotion='" + emotion + '\'' +
                ", cycleNumber=" + cycleNumber +
                ", shouldContinueLoop=" + shouldContinueLoop +
                ", isSessionEnding=" + isSessionEnding +
                '}';
    }
}

