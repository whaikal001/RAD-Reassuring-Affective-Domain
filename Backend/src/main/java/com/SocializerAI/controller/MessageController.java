package com.SocializerAI.controller;

import com.SocializerAI.service.MessageService;
import com.SocializerAI.repository.ConversationRepository;
import org.springframework.web.bind.annotation.*;
import com.SocializerAI.model.Message;
import java.util.*;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService svc;
    private final ConversationRepository conversationRepo;

    public MessageController(MessageService svc, ConversationRepository conversationRepo){
        this.svc = svc;
        this.conversationRepo = conversationRepo;
    }

    @PostMapping("/{conversationId}/send")
    public List<Message> send(@PathVariable("conversationId") UUID conversationId, @RequestBody Map<String,String> body){
        var convOpt = conversationRepo.findById(conversationId);
        if (convOpt.isPresent() && convOpt.get().getEndedAt() != null) {
            // Conversation already ended; return history without saving new messages
            return svc.history(conversationId);
        }

        String text = body.get("text");
        Message userMsg = new Message();
        userMsg.setConversationId(conversationId);
        userMsg.setSenderType("user");
        userMsg.setMessageText(text);
        svc.save(userMsg);
        svc.generateAiReply(conversationId, text);
        return svc.history(conversationId);
    }

    @GetMapping("/{conversationId}")
    public List<Message> history(@PathVariable("conversationId") UUID conversationId){ return svc.history(conversationId); }
}
