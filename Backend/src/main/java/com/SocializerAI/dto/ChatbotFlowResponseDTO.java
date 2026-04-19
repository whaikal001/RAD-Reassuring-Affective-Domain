package com.SocializerAI.dto;

import com.SocializerAI.enums.PathwayType;
import com.SocializerAI.enums.ApproachType;
import java.util.HashMap;
import java.util.Map;

/**
 * DTO for full flow response including metadata for client-side tracking
 */
public class ChatbotFlowResponseDTO {
    private String conversationId;
    private String greeting;
    private String assessment;
    private String mainContent;
    private String strategies;
    private String followUp;
    private String fullResponse;
    private PathwayType pathway;
    private ApproachType approach;
    private int intensity;
    private String emotion;
    private int cycleNumber;
    private boolean shouldContinueLoop;
    private boolean isSessionEnding;
    private String endingReason;
    private long sessionDurationMinutes;
    private Map<String, Object> metadata;

    public ChatbotFlowResponseDTO() {
        this.metadata = new HashMap<>();
    }

    // Getters and Setters
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }

    public String getAssessment() { return assessment; }
    public void setAssessment(String assessment) { this.assessment = assessment; }

    public String getMainContent() { return mainContent; }
    public void setMainContent(String mainContent) { this.mainContent = mainContent; }

    public String getStrategies() { return strategies; }
    public void setStrategies(String strategies) { this.strategies = strategies; }

    public String getFollowUp() { return followUp; }
    public void setFollowUp(String followUp) { this.followUp = followUp; }

    public String getFullResponse() { return fullResponse; }
    public void setFullResponse(String fullResponse) { this.fullResponse = fullResponse; }

    public PathwayType getPathway() { return pathway; }
    public void setPathway(PathwayType pathway) { this.pathway = pathway; }

    public ApproachType getApproach() { return approach; }
    public void setApproach(ApproachType approach) { this.approach = approach; }

    public int getIntensity() { return intensity; }
    public void setIntensity(int intensity) { this.intensity = intensity; }

    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }

    public int getCycleNumber() { return cycleNumber; }
    public void setCycleNumber(int cycleNumber) { this.cycleNumber = cycleNumber; }

    public boolean isShouldContinueLoop() { return shouldContinueLoop; }
    public void setShouldContinueLoop(boolean shouldContinueLoop) { this.shouldContinueLoop = shouldContinueLoop; }

    public boolean isSessionEnding() { return isSessionEnding; }
    public void setSessionEnding(boolean sessionEnding) { isSessionEnding = sessionEnding; }

    public String getEndingReason() { return endingReason; }
    public void setEndingReason(String endingReason) { this.endingReason = endingReason; }

    public long getSessionDurationMinutes() { return sessionDurationMinutes; }
    public void setSessionDurationMinutes(long sessionDurationMinutes) { this.sessionDurationMinutes = sessionDurationMinutes; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
}
