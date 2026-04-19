package com.SocializerAI.controller;

import com.SocializerAI.service.ConversationService;
import com.SocializerAI.model.Conversation;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * REST Controller for conversation management
 * Handles conversation lifecycle: start, restart, end, and retrieval
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /**
     * Start a new conversation
     * 
     * POST /api/conversations/start?lang=en&userId=...
     * 
     * Creates a new conversation for the authenticated user with a greeting message
     * 
     * @param lang Language code (en, ms, etc.)
     * @param userId User ID (optional - if not provided, uses guest user)
     * @return New conversation with greeting
     */
    @PostMapping("/start")
    public Conversation startConversation(
            @RequestParam(name = "lang", defaultValue = "en") String lang,
            @RequestParam(name = "userId", required = false) String userId) {
        // Use provided userId or create a guest user UUID
        // Treat "undefined" as null/empty
        UUID userUUID;
        if (userId != null && !userId.isBlank() && !userId.equals("undefined")) {
            userUUID = UUID.fromString(userId);
        } else {
            userUUID = UUID.randomUUID();
        }
        return conversationService.start(userUUID, lang);
    }

    /**
     * Get a specific conversation by ID
     * 
     * GET /api/conversations/{conversationId}
     * 
     * @param conversationId Conversation ID
     * @return Conversation details
     */
    @GetMapping("/{conversationId}")
    public Conversation getConversation(@PathVariable("conversationId") UUID conversationId) {
        return conversationService.getConversation(conversationId);
    }
    
    /**
     * Get all conversations for a user
     * 
     * GET /api/conversations/user/{userId}
     * 
     * @param userId User ID
     * @return List of conversations
     */
    @GetMapping("/user/{userId}")
    public java.util.List<Conversation> getUserConversations(@PathVariable("userId") UUID userId) {
        return conversationService.getUserConversations(userId);
    }

    /**
     * Restart a conversation
     * 
     * POST /api/conversations/{conversationId}/restart?lang=en
     * 
     * Ends the current conversation and starts a new one with the same user
     * 
     * @param conversationId Current conversation ID
     * @param lang Language code (en, ms, etc.)
     * @return New conversation
     */
    @PostMapping("/{conversationId}/restart")
    public Conversation restartConversation(
            @PathVariable("conversationId") UUID conversationId,
            @RequestParam(name = "lang", defaultValue = "en") String lang) {
        return conversationService.restart(conversationId, lang);
    }

    /**
     * End a conversation
     * 
     * POST /api/conversations/{conversationId}/end
     * 
     * @param conversationId Conversation ID
     * @return Updated conversation with endedAt timestamp
     */
    @PostMapping("/{conversationId}/end")
    public Conversation endConversation(@PathVariable("conversationId") UUID conversationId) {
        return conversationService.end(conversationId);
    }
}
