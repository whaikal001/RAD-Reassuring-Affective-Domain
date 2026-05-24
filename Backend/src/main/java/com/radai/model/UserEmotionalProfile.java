package com.radai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_emotional_profiles")
public class UserEmotionalProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "dominant_emotion")
    private String dominantEmotion;
    
    @Column(name = "average_intensity")
    private Double averageIntensity;
    
    @Column(name = "stress_level")
    private Integer stressLevel;
    
    @Column(name = "anxiety_level")
    private Integer anxietyLevel;
    
    @Column(name = "mood_stability")
    private Double moodStability;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "primary_emotion")
    private String primaryEmotion;

    @Column(name = "secondary_emotion")
    private String secondaryEmotion;

    @Column(name = "emotional_intensity")
    private Integer emotionalIntensity;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public UserEmotionalProfile() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.lastUpdated = this.createdAt;
    }
    
    public UserEmotionalProfile(UUID userId) {
        this();
        this.userId = userId;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getDominantEmotion() { return dominantEmotion; }
    public void setDominantEmotion(String dominantEmotion) { this.dominantEmotion = dominantEmotion; }
    
    public Double getAverageIntensity() { return averageIntensity; }
    public void setAverageIntensity(Double averageIntensity) { this.averageIntensity = averageIntensity; }
    
    public Integer getStressLevel() { return stressLevel; }
    public void setStressLevel(Integer stressLevel) { this.stressLevel = stressLevel; }
    
    public Integer getAnxietyLevel() { return anxietyLevel; }
    public void setAnxietyLevel(Integer anxietyLevel) { this.anxietyLevel = anxietyLevel; }
    
    public Double getMoodStability() { return moodStability; }
    public void setMoodStability(Double moodStability) { this.moodStability = moodStability; }
    
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getPrimaryEmotion() { return primaryEmotion; }
    public void setPrimaryEmotion(String primaryEmotion) { this.primaryEmotion = primaryEmotion; }

    public String getSecondaryEmotion() { return secondaryEmotion; }
    public void setSecondaryEmotion(String secondaryEmotion) { this.secondaryEmotion = secondaryEmotion; }

    public Integer getEmotionalIntensity() { return emotionalIntensity; }
    public void setEmotionalIntensity(Integer emotionalIntensity) { this.emotionalIntensity = emotionalIntensity; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

