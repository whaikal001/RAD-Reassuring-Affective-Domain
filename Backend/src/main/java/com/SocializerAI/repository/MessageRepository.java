package com.SocializerAI.repository;

import com.SocializerAI.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByConversationId(UUID conversationId);
    List<Message> findByConversationIdOrderByTimestampAsc(UUID conversationId);
    List<Message> findByConversationIdOrderBySentAtAsc(UUID conversationId);
}
