package com.radai.repository;

import com.radai.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    List<Conversation> findByUserId(UUID userId);
    List<Conversation> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<Conversation> findByUserIdAndIsActive(UUID userId, Boolean isActive);
    Optional<Conversation> findTopByUserIdAndIsActiveOrderByStartedAtDesc(UUID userId, Boolean isActive);
    Optional<Conversation> findTopByUserIdAndSessionIdOrderByStartedAtDesc(UUID userId, String sessionId);
    Optional<Conversation> findTopByUserIdAndSessionIdAndIsActiveOrderByStartedAtDesc(UUID userId, String sessionId, Boolean isActive);
}

