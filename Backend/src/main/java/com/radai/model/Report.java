package com.radai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "report_type")
    private String reportType; // "weekly", "monthly", "custom"
    
    @Column(name = "start_date")
    private LocalDateTime startDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    @Column(columnDefinition = "TEXT")
    private String summary;
    
    @Column(name = "total_sessions")
    private Integer totalSessions;
    
    @Column(name = "average_mood_score")
    private Double averageMoodScore;
    
    public Report() {
        this.generatedAt = LocalDateTime.now();
    }
    
    public Report(UUID userId, String reportType) {
        this();
        this.userId = userId;
        this.reportType = reportType;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public Integer getTotalSessions() { return totalSessions; }
    public void setTotalSessions(Integer totalSessions) { this.totalSessions = totalSessions; }
    
    public Double getAverageMoodScore() { return averageMoodScore; }
    public void setAverageMoodScore(Double averageMoodScore) { this.averageMoodScore = averageMoodScore; }
}

