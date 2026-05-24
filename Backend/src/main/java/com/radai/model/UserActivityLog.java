package com.radai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_activity_logs", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_activity_type", columnList = "activity_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class UserActivityLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "activity_type", nullable = false)
    private String activityType; // LOGIN, MESSAGE, SESSION_START, SESSION_END, REPORT_VIEW, SCREEN_TEST, etc.
    
    @Column(name = "activity_description")
    private String description;
    
    @Column(name = "intensity_level")
    private String intensityLevel; // LOW, MEDIUM, HIGH based on emotion scores
    
    @Column(name = "emotion_primary")
    private String primaryEmotion; // Joy, Sadness, Anger, Fear, Disgust, Neutral
    
    @Column(name = "emotion_score")
    private Double emotionScore; // 0.0 to 1.0
    
    @Column(name = "session_id")
    private String sessionId;
    
    @Column(name = "duration_seconds")
    private Long durationSeconds;
    
    @Column(name = "message_count")
    private Integer messageCount = 0;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON additional data
    
    // Constructors
    public UserActivityLog() {
        this.timestamp = LocalDateTime.now();
    }
    
    public UserActivityLog(UUID userId, String activityType) {
        this();
        this.userId = userId;
        this.activityType = activityType;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIntensityLevel() { return intensityLevel; }
    public void setIntensityLevel(String intensityLevel) { this.intensityLevel = intensityLevel; }
    
    public String getPrimaryEmotion() { return primaryEmotion; }
    public void setPrimaryEmotion(String primaryEmotion) { this.primaryEmotion = primaryEmotion; }
    
    public Double getEmotionScore() { return emotionScore; }
    public void setEmotionScore(Double emotionScore) { this.emotionScore = emotionScore; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    
    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}

