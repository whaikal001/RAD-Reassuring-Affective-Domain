package com.SocializerAI.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="emotional_patterns")
public class EmotionalPattern {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID userId;
    private String patternType;
    private String dominantEmotion;
    private Double averageIntensity;
    private Integer frequencyCount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String analysisNotes;
    private LocalDateTime createdAt = LocalDateTime.now();

    public EmotionalPattern() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }
    public String getDominantEmotion() { return dominantEmotion; }
    public void setDominantEmotion(String dominantEmotion) { this.dominantEmotion = dominantEmotion; }
    public Double getAverageIntensity() { return averageIntensity; }
    public void setAverageIntensity(Double averageIntensity) { this.averageIntensity = averageIntensity; }
    public Integer getFrequencyCount() { return frequencyCount; }
    public void setFrequencyCount(Integer frequencyCount) { this.frequencyCount = frequencyCount; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public String getAnalysisNotes() { return analysisNotes; }
    public void setAnalysisNotes(String analysisNotes) { this.analysisNotes = analysisNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
