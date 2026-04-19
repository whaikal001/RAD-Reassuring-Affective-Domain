package com.SocializerAI.service;

import org.springframework.stereotype.Service;
import com.SocializerAI.repository.UserEmotionalProfileRepository;
import com.SocializerAI.model.UserEmotionalProfile;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserEmotionalProfileService {
    private final UserEmotionalProfileRepository repo;

    public UserEmotionalProfileService(UserEmotionalProfileRepository repo){ this.repo = repo; }

    public UserEmotionalProfile create(UserEmotionalProfile profile){
        profile.setCreatedAt(LocalDateTime.now());
        profile.setUpdatedAt(LocalDateTime.now());
        return repo.save(profile);
    }

    public UserEmotionalProfile get(UUID userId){
        return repo.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Profile not found"));
    }

    public UserEmotionalProfile update(UUID userId, UserEmotionalProfile profile){
        var existing = repo.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Profile not found"));
        existing.setPrimaryEmotion(profile.getPrimaryEmotion());
        existing.setSecondaryEmotion(profile.getSecondaryEmotion());
        existing.setEmotionalIntensity(profile.getEmotionalIntensity());
        existing.setNotes(profile.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }
}
