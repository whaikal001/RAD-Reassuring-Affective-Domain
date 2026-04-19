package com.SocializerAI.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.SocializerAI.model.EmotionalHistory;
import java.util.List;
import java.util.UUID;

public interface EmotionalHistoryRepository extends JpaRepository<EmotionalHistory, UUID> {
    List<EmotionalHistory> findByUserIdOrderByLoggedAtAsc(UUID userId);
}
