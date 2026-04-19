package com.SocializerAI.service;

import org.springframework.stereotype.Service;
import com.SocializerAI.repository.EmotionalPatternRepository;
import com.SocializerAI.model.EmotionalPattern;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Service
public class PatternService {
    private final EmotionalPatternRepository repo;

    public PatternService(EmotionalPatternRepository repo){ this.repo = repo; }

    public EmotionalPattern create(EmotionalPattern pattern){
        pattern.setCreatedAt(LocalDateTime.now());
        return repo.save(pattern);
    }

    public List<EmotionalPattern> byUser(UUID userId){
        return repo.findByUserId(userId);
    }

    public EmotionalPattern get(UUID id){
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Pattern not found"));
    }

    public EmotionalPattern update(UUID id, EmotionalPattern pattern){
        var existing = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Pattern not found"));
        existing.setPatternType(pattern.getPatternType());
        existing.setDominantEmotion(pattern.getDominantEmotion());
        existing.setAverageIntensity(pattern.getAverageIntensity());
        existing.setFrequencyCount(pattern.getFrequencyCount());
        existing.setAnalysisNotes(pattern.getAnalysisNotes());
        return repo.save(existing);
    }
}
