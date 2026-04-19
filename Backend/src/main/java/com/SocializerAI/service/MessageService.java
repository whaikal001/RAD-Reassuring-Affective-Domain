package com.SocializerAI.service;

import org.springframework.stereotype.Service;
import com.SocializerAI.repository.MessageRepository;
import com.SocializerAI.repository.ConversationRepository;
import com.SocializerAI.model.Message;
import com.SocializerAI.model.Conversation;
import com.SocializerAI.chat.hf.HuggingFaceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static final double SAFETY_WARN_THRESHOLD = 0.60;
    private static final double SAFETY_BLOCK_THRESHOLD = 0.85;
    private static final Set<String> TOXIC_KEYWORDS = Set.of(
            "babi", "bodo", "bodoh", "sampah", "bangsat", "sial",
            "fuck", "fucking", "bitch", "asshole", "idiot", "moron", "stupid"
    );
    
    private final MessageRepository repo;
    private final ConversationRepository conversationRepo;
    private final HuggingFaceClient hf;

    public MessageService(MessageRepository repo, ConversationRepository conversationRepo, HuggingFaceClient hf){
        this.repo = repo;
        this.conversationRepo = conversationRepo;
        this.hf = hf;
    }

    public Message save(Message m){ return repo.save(m); }

    public List<Message> history(UUID conversationId){ return repo.findByConversationIdOrderBySentAtAsc(conversationId); }

    public Message generateAiReply(UUID conversationId, String userText) {
        // If user requests to exit, end conversation and respond with goodbye
        if (isExitRequest(userText)) {
            Conversation conv = conversationRepo.findById(conversationId).orElse(null);
            if (conv != null && conv.getEndedAt() == null) {
                conv.setEndedAt(LocalDateTime.now());
                conversationRepo.save(conv);
            }
            Message goodbye = new Message();
            goodbye.setConversationId(conversationId);
            goodbye.setSenderType("ai");
            goodbye.setMessageText("Thanks for chatting. Take care and goodbye!");
            goodbye.setDetectedEmotion("warm");
            goodbye.setEmotionConfidence(0.9);
            return repo.save(goodbye);
        }

        try {
            if (isHeuristicallyToxic(userText)) {
                logger.warn("Blocked toxic user input via keyword safety filter");
                Message blocked = new Message();
                blocked.setConversationId(conversationId);
                blocked.setSenderType("ai");
                blocked.setMessageText("I can continue, but I cannot engage with harmful or abusive language. If you want, we can restart calmly and focus on what you need right now.");
                blocked.setDetectedEmotion("safety");
                blocked.setEmotionConfidence(0.95);
                return repo.save(blocked);
            }

            Map<String, Object> userSafety = hf.classifySafety(userText);
            double userToxicity = ((Number) userSafety.getOrDefault("score", 0.0)).doubleValue();
            if (userToxicity >= SAFETY_BLOCK_THRESHOLD) {
                logger.warn("Blocked toxic user input with score: {}", userToxicity);
                Message blocked = new Message();
                blocked.setConversationId(conversationId);
                blocked.setSenderType("ai");
                blocked.setMessageText("I can continue, but I cannot engage with harmful or abusive language. If you want, we can restart calmly and focus on what you need right now.");
                blocked.setDetectedEmotion("safety");
                blocked.setEmotionConfidence(userToxicity);
                return repo.save(blocked);
            }

            logger.info("Generating AI reply for: {}", userText.substring(0, Math.min(50, userText.length())));
            String aiText = hf.generateReply(userText);
            logger.info("AI reply generated successfully");

            Map<String, Object> aiSafety = hf.classifySafety(aiText);
            double aiToxicity = ((Number) aiSafety.getOrDefault("score", 0.0)).doubleValue();
            if (aiToxicity >= SAFETY_BLOCK_THRESHOLD) {
                logger.warn("Blocked toxic AI output with score: {}", aiToxicity);
                aiText = "I want to keep this conversation safe and supportive. Could you share what happened in a different way so I can help better?";
            } else if (aiToxicity >= SAFETY_WARN_THRESHOLD) {
                logger.info("Safety warning for AI output score: {}", aiToxicity);
                aiText = aiText + "\n\nLet's keep our conversation respectful and supportive.";
            }
            
            Map<String,Object> emo = hf.classifyEmotion(aiText);
            Message ai = new Message();
            ai.setConversationId(conversationId);
            ai.setSenderType("ai");
            ai.setMessageText(aiText);
            ai.setDetectedEmotion((String) emo.getOrDefault("label","neutral"));
            ai.setEmotionConfidence(((Number) emo.getOrDefault("score",0.8)).doubleValue());
            return repo.save(ai);
        } catch (Exception e) {
            logger.error("Failed to generate AI reply: {}", e.getMessage(), e);
            Message fallback = new Message();
            fallback.setConversationId(conversationId);
            fallback.setSenderType("ai");
            fallback.setMessageText("Sorry, I couldn't generate a reply right now. " + e.getMessage());
            fallback.setDetectedEmotion("neutral");
            fallback.setEmotionConfidence(0.5);
            return repo.save(fallback);
        }
    }

    private boolean isExitRequest(String text) {
        if (text == null) return false;
        String t = text.trim().toLowerCase();
        return t.equals("exit") || t.equals("quit") || t.equals("bye") || t.equals("goodbye") || t.equals("stop") || t.equals("off")
                || t.contains("end conversation") || t.contains("i want to exit") || t.contains("i want to off") || t.contains("turn off");
    }

    private boolean isHeuristicallyToxic(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        String normalized = text.toLowerCase().replaceAll("[^a-z\\s]", " ").replaceAll("\\s+", " ").trim();
        if (normalized.isBlank()) {
            return false;
        }

        for (String keyword : TOXIC_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
