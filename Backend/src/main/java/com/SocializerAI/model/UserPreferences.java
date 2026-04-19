package com.SocializerAI.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "user_preferences")
public class UserPreferences {
    
    // Supported Languages: English (en) and Malay (ms)
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_MALAY = "ms";
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", unique = true, nullable = false)
    private UUID userId;
    
    @Column(name = "language", columnDefinition = "VARCHAR(5) CHECK (language IN ('en', 'ms'))")
    private String language = LANGUAGE_ENGLISH;
    
    @Column(name = "theme")
    private String theme = "light";

    @Column(name = "timezone")
    private String timezone = "UTC";
    
    @Column(name = "notification_enabled")
    private Boolean notificationEnabled = true;

    @Column(name = "email_notifications")
    private Boolean emailNotifications = true;
    
    @Column(name = "data_collection_consent")
    private Boolean dataCollectionConsent = false;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    public UserPreferences() {}
    
    public UserPreferences(UUID userId) {
        this.userId = userId;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { 
        if (language != null && (language.equals(LANGUAGE_ENGLISH) || language.equals(LANGUAGE_MALAY))) {
            this.language = language;
        } else {
            throw new IllegalArgumentException("Language must be 'en' (English) or 'ms' (Malay)");
        }
    }
    
    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    
    public Boolean getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(Boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    // Aliases for service naming
    public Boolean getNotificationsEnabled() { return notificationEnabled; }
    public void setNotificationsEnabled(Boolean notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public Boolean getEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(Boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    
    public Boolean getDataCollectionConsent() { return dataCollectionConsent; }
    public void setDataCollectionConsent(Boolean dataCollectionConsent) { this.dataCollectionConsent = dataCollectionConsent; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
