package com.SocializerAI.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.SocializerAI.model.EmotionalPattern;
import java.util.UUID;
import java.util.List;

public interface EmotionalPatternRepository extends JpaRepository<EmotionalPattern, UUID> {
    List<EmotionalPattern> findByUserId(UUID userId);
}
