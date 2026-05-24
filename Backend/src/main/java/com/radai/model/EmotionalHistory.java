package com.radai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="emotional_history")
public class EmotionalHistory {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID userId;
    private String emotionalState;
    private int intensity;
    private double sentimentScore;
    private LocalDateTime loggedAt = LocalDateTime.now();
    private String source;
    @Column(columnDefinition="text")
    private String messageText;

    public EmotionalHistory() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getEmotionalState() { return emotionalState; }
    public void setEmotionalState(String emotionalState) { this.emotionalState = emotionalState; }
    public int getIntensity() { return intensity; }
    public void setIntensity(int intensity) { this.intensity = intensity; }
    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
    public void setLoggedAt(LocalDateTime loggedAt) { this.loggedAt = loggedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
}

