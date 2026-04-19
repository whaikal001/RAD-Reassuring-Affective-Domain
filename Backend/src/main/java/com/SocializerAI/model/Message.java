package com.SocializerAI.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;
    
    @Column(name = "sender")
    private String sender; // "user" or "bot"
    
    @Column(name = "sender_type")
    private String senderType; // Alternative field for sender type
    
    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText; // Alternative field for content
    
    @Column(columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    
    @Column(name = "emotion")
    private String emotion;
    
    @Column(name = "detected_emotion")
    private String detectedEmotion; // Alternative field for emotion
    
    @Column(name = "intensity_score")
    private Integer intensityScore;
    
    @Column(name = "emotion_confidence")
    private Double emotionConfidence;
    
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    public Message(UUID conversationId, String sender, String content) {
        this();
        this.conversationId = conversationId;
        this.sender = sender;
        this.content = content;
    }
    
    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; this.senderType = sender; }
    
    public String getSenderType() { return senderType != null ? senderType : sender; }
    public void setSenderType(String senderType) { this.senderType = senderType; this.sender = senderType; }
    
    public String getContent() { return content != null ? content : messageText; }
    public void setContent(String content) { this.content = content; this.messageText = content; }
    
    public String getMessageText() { return messageText != null ? messageText : content; }
    public void setMessageText(String messageText) { this.messageText = messageText; this.content = messageText; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public LocalDateTime getSentAt() { return sentAt != null ? sentAt : timestamp; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; this.timestamp = sentAt; }
    
    public String getEmotion() { return emotion != null ? emotion : detectedEmotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; this.detectedEmotion = emotion; }
    
    public String getDetectedEmotion() { return detectedEmotion != null ? detectedEmotion : emotion; }
    public void setDetectedEmotion(String detectedEmotion) { this.detectedEmotion = detectedEmotion; this.emotion = detectedEmotion; }
    
    public Integer getIntensityScore() { return intensityScore; }
    public void setIntensityScore(Integer intensityScore) { this.intensityScore = intensityScore; }
    
    public Double getEmotionConfidence() { return emotionConfidence; }
    public void setEmotionConfidence(Double emotionConfidence) { this.emotionConfidence = emotionConfidence; }
}
