package com.radai.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "report_insights")
public class ReportInsight {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "report_id", nullable = false)
    private UUID reportId;
    
    @Column(name = "insight_type")
    private String insightType;
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "priority")
    private Integer priority;
    
    public ReportInsight() {}
    
    public ReportInsight(UUID reportId, String insightType, String content) {
        this.reportId = reportId;
        this.insightType = insightType;
        this.content = content;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }
    
    public String getInsightType() { return insightType; }
    public void setInsightType(String insightType) { this.insightType = insightType; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
}

