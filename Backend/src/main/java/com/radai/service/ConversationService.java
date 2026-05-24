package com.radai.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.radai.repository.ConversationRepository;
import com.radai.repository.MessageRepository;
import com.radai.model.Conversation;
import com.radai.model.Message;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@Transactional
public class ConversationService {
    private final ConversationRepository repo;
    private final MessageRepository messageRepo;

    public ConversationService(ConversationRepository repo, MessageRepository messageRepo){
        this.repo = repo;
        this.messageRepo = messageRepo;
    }

    public Conversation start(UUID userId){
        return start(userId, "en");
    }

    public Conversation start(UUID userId, String lang){
        var c = new Conversation();
        c.setUserId(userId);
        c.setSessionId(UUID.randomUUID().toString());
        Conversation saved = repo.save(c);

        // Seed a greeting message so the user is welcomed immediately.
        Message greeting = new Message();
        greeting.setConversationId(saved.getId());
        greeting.setSenderType("ai");
        greeting.setMessageText(buildGreeting(lang));
        greeting.setDetectedEmotion("neutral");
        greeting.setEmotionConfidence(0.5);
        messageRepo.save(greeting);

        return saved;
    }

    public Conversation end(UUID id){
        var c = repo.findById(id).orElseThrow();
        c.setEndedAt(java.time.LocalDateTime.now());
        return repo.save(c);
    }

    public Conversation getConversation(UUID id){
        return repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Conversation not found: " + id));
    }

    public java.util.List<Conversation> getUserConversations(UUID userId){
        return repo.findByUserIdOrderByStartedAtDesc(userId);
    }

    private String buildGreeting(String lang){
        String l = (lang == null || lang.isBlank()) ? "en" : lang.toLowerCase();
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        String partOfDayEn = hour < 12 ? "morning" : hour < 17 ? "afternoon" : hour < 21 ? "evening" : "night";
        String partOfDayMs = hour < 12 ? "pagi" : hour < 17 ? "tengah hari" : hour < 21 ? "petang" : "malam";
        String dateTimeEn = now.format(DateTimeFormatter.ofPattern("EEEE, MMM d yyyy h:mm a"));
        String dateTimeMs = now.format(DateTimeFormatter.ofPattern("EEEE, d MMM yyyy h:mm a"));

        if (l.startsWith("ms")) {
            return "Selamat " + partOfDayMs + "! Hari ini " + dateTimeMs + ". Adakah anda sudah makan? Bagaimana perasaan emosi anda hari ini?";
        }
        return "Good " + partOfDayEn + "! Today is " + dateTimeEn + ". Have you eaten? How are you feeling today?";
    }

    // Restart a conversation by ending the current one (if active)
    // and starting a fresh session for the same user, including greeting
    public Conversation restart(UUID conversationId, String lang){
        var current = repo.findById(conversationId).orElseThrow();
        if (current.getEndedAt() == null) {
            current.setEndedAt(LocalDateTime.now());
            repo.save(current);
        }
        return start(current.getUserId(), lang);
    }
}

