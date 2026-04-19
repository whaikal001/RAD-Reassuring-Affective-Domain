package com.SocializerAI.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name="wellbeing_activities")
public class WellbeingActivity {
    @Id
    @GeneratedValue
    private UUID id;
    private String activityName;
    private String category;
    private int durationMinutes;
    private String recommendedFor;

    public WellbeingActivity() {}

    // Getters & Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getRecommendedFor() { return recommendedFor; }
    public void setRecommendedFor(String recommendedFor) { this.recommendedFor = recommendedFor; }
}
