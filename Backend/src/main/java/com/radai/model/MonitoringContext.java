package com.radai.model;

import com.radai.enums.IntensityLevel;
import com.radai.enums.ApproachType;
import com.radai.enums.PathwayType;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Monitoring context for a single conversation session.
 * Tracks emotional state, intensity, approach effectiveness, and conversation history.
 */
public class MonitoringContext {
    private String conversationId;
    private UUID userId;
    private LocalDateTime sessionStartTime;
    private LocalDateTime lastAssessmentTime;
    
    // Current emotional state
    private String currentEmotion;
    private int currentIntensityScore;
    private IntensityLevel currentIntensityLevel;
    private String dominantStressor;
    
    // Pathway and approach tracking
    private PathwayType currentPathway;
    private ApproachType currentApproach;
    private int cycleCount = 0;
    private int maxCycles = 15; // Maximum loop iterations
    
    // Response tracking
    private List<String> recentUserMessages = new ArrayList<>();
    private List<String> recentBotResponses = new ArrayList<>();
    private boolean userResponding = true;
    private boolean userImproving = false;
    private int messagesWithoutImprovement = 0;
    
    // Switching history
    private Map<String, Integer> approachSwitchHistory = new HashMap<>();
    private List<String> switchingReasons = new ArrayList<>();
    
    // Detection flags
    private boolean stressDetected = false;
    private boolean anxietyDetected = false;
    private boolean crisisDetected = false;
    private boolean suicidalIdeationDetected = false;
    
    // Previous state for change detection
    private int previousIntensityScore;
    private String previousEmotion;
    private PathwayType previousPathway;

    public MonitoringContext(UUID userId, String conversationId) {
        this.userId = userId;
        this.conversationId = conversationId;
        this.sessionStartTime = LocalDateTime.now();
        this.lastAssessmentTime = LocalDateTime.now();
        this.currentIntensityScore = 5;
        this.currentIntensityLevel = IntensityLevel.MODERATE;
        this.currentApproach = ApproachType.EMPATHY;
        this.currentPathway = PathwayType.PREVENTION;
        this.previousIntensityScore = 5;
    }

    // Getters and Setters
    public String getConversationId() { return conversationId; }
    public UUID getUserId() { return userId; }
    public LocalDateTime getSessionStartTime() { return sessionStartTime; }
    public LocalDateTime getLastAssessmentTime() { return lastAssessmentTime; }
    public void setLastAssessmentTime(LocalDateTime time) { this.lastAssessmentTime = time; }

    public String getCurrentEmotion() { return currentEmotion; }
    public void setCurrentEmotion(String emotion) { this.currentEmotion = emotion; }

    public int getCurrentIntensityScore() { return currentIntensityScore; }
    public void setCurrentIntensityScore(int score) {
        this.previousIntensityScore = this.currentIntensityScore;
        this.currentIntensityScore = score;
        this.currentIntensityLevel = IntensityLevel.fromScore(score);
    }

    public IntensityLevel getCurrentIntensityLevel() { return currentIntensityLevel; }
    public void setCurrentIntensityLevel(IntensityLevel level) { this.currentIntensityLevel = level; }

    public String getDominantStressor() { return dominantStressor; }
    public void setDominantStressor(String stressor) { this.dominantStressor = stressor; }

    public PathwayType getCurrentPathway() { return currentPathway; }
    public void setCurrentPathway(PathwayType pathway) {
        this.previousPathway = this.currentPathway;
        this.currentPathway = pathway;
    }

    public ApproachType getCurrentApproach() { return currentApproach; }
    public void setCurrentApproach(ApproachType approach) {
        String switchKey = approach.name();
        approachSwitchHistory.put(switchKey, approachSwitchHistory.getOrDefault(switchKey, 0) + 1);
        this.currentApproach = approach;
    }

    public int getCycleCount() { return cycleCount; }
    public void incrementCycle() { this.cycleCount++; }
    public boolean isMaxCyclesReached() { return cycleCount >= maxCycles; }

    public List<String> getRecentUserMessages() { return recentUserMessages; }
    public void addUserMessage(String message) {
        recentUserMessages.add(message);
        if (recentUserMessages.size() > 5) recentUserMessages.remove(0);
    }

    /**
     * Get the last user message in the conversation
     */
    public String getLastUserMessage() {
        if (recentUserMessages.isEmpty()) {
            return null;
        }
        return recentUserMessages.get(recentUserMessages.size() - 1);
    }

    public List<String> getRecentBotResponses() { return recentBotResponses; }
    public void addBotResponse(String response) {
        recentBotResponses.add(response);
        if (recentBotResponses.size() > 5) recentBotResponses.remove(0);
    }

    public boolean isUserResponding() { return userResponding; }
    public void setUserResponding(boolean responding) { this.userResponding = responding; }

    public boolean isUserImproving() { return userImproving; }
    public void setUserImproving(boolean improving) { this.userImproving = improving; }

    public int getMessagesWithoutImprovement() { return messagesWithoutImprovement; }
    public void incrementMessagesWithoutImprovement() { this.messagesWithoutImprovement++; }
    public void resetMessagesWithoutImprovement() { this.messagesWithoutImprovement = 0; }

    public boolean isStressDetected() { return stressDetected; }
    public void setStressDetected(boolean detected) { this.stressDetected = detected; }

    public boolean isAnxietyDetected() { return anxietyDetected; }
    public void setAnxietyDetected(boolean detected) { this.anxietyDetected = detected; }

    public boolean isCrisisDetected() { return crisisDetected; }
    public void setCrisisDetected(boolean detected) { this.crisisDetected = detected; }

    public boolean isSuicidalIdeationDetected() { return suicidalIdeationDetected; }
    public void setSuicidalIdeationDetected(boolean detected) { this.suicidalIdeationDetected = detected; }

    public int getPreviousIntensityScore() { return previousIntensityScore; }
    public String getPreviousEmotion() { return previousEmotion; }
    public PathwayType getPreviousPathway() { return previousPathway; }

    public Map<String, Integer> getApproachSwitchHistory() { return approachSwitchHistory; }
    public List<String> getSwitchingReasons() { return switchingReasons; }
    public void addSwitchingReason(String reason) { this.switchingReasons.add(reason); }

    public boolean hasEscalated() {
        return previousIntensityScore < currentIntensityScore;
    }

    public boolean hasDeEscalated() {
        return previousIntensityScore > currentIntensityScore;
    }

    public long getSessionDurationMinutes() {
        return java.time.temporal.ChronoUnit.MINUTES.between(sessionStartTime, LocalDateTime.now());
    }
}

