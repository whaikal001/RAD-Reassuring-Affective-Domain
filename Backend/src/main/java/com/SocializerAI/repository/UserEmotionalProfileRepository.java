package com.SocializerAI.repository;

import com.SocializerAI.model.UserEmotionalProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserEmotionalProfileRepository extends JpaRepository<UserEmotionalProfile, UUID> {
    Optional<UserEmotionalProfile> findByUserId(UUID userId);
}
