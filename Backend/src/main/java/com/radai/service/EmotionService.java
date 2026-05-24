package com.radai.service;

import org.springframework.stereotype.Service;
import com.radai.repository.EmotionalHistoryRepository;
import com.radai.model.EmotionalHistory;
import java.util.*;

@Service
public class EmotionService {
    private final EmotionalHistoryRepository repo;

    public EmotionService(EmotionalHistoryRepository repo){ this.repo = repo; }

    public EmotionalHistory log(EmotionalHistory h){ return repo.save(h); }

    public List<EmotionalHistory> history(UUID userId){ return repo.findByUserIdOrderByLoggedAtAsc(userId); }
}

