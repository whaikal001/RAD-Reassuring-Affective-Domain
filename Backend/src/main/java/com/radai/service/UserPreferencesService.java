package com.radai.service;

import org.springframework.stereotype.Service;
import com.radai.repository.UserPreferencesRepository;
import com.radai.model.UserPreferences;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserPreferencesService {
    private final UserPreferencesRepository repo;

    public UserPreferencesService(UserPreferencesRepository repo){ this.repo = repo; }

    public UserPreferences create(UserPreferences preferences){
        preferences.setCreatedAt(LocalDateTime.now());
        preferences.setUpdatedAt(LocalDateTime.now());
        return repo.save(preferences);
    }

    public UserPreferences get(UUID userId){
        return repo.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Preferences not found"));
    }

    public UserPreferences update(UUID userId, UserPreferences preferences){
        var existing = repo.findByUserId(userId).orElseThrow(() -> new IllegalArgumentException("Preferences not found"));
        existing.setTheme(preferences.getTheme());
        existing.setLanguage(preferences.getLanguage());
        existing.setTimezone(preferences.getTimezone());
        existing.setNotificationsEnabled(preferences.getNotificationsEnabled());
        existing.setEmailNotifications(preferences.getEmailNotifications());
        existing.setUpdatedAt(LocalDateTime.now());
        return repo.save(existing);
    }
}

