package com.SocializerAI.service;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.SocializerAI.dto.AccountUpdateRequest;
import com.SocializerAI.repository.UserRepository;
import com.SocializerAI.model.User;
import java.util.UUID;
import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder){
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAll(){ return repo.findAll(); }

    public User get(UUID id){ return repo.findById(id).orElseThrow(); }

    public User update(UUID id, User u){
        var existing = repo.findById(id).orElseThrow();
        existing.setFullName(u.getFullName());
        existing.setPhone(u.getPhone());
        existing.setAvatarUrl(u.getAvatarUrl());
        existing.setBio(u.getBio());
        existing.setCurrentEmotionalState(u.getCurrentEmotionalState());
        existing.setIsActive(u.getIsActive());
        existing.setIsAnonymous(u.getIsAnonymous());
        existing.setIsVerified(u.getIsVerified());
        existing.setUpdatedAt(java.time.LocalDateTime.now());
        return repo.save(existing);
    }

    public User updateAccount(UUID id, AccountUpdateRequest req) {
        var existing = repo.findById(id).orElseThrow();

        String requestedUsername = req.getUsername() != null ? req.getUsername().trim() : null;
        if (requestedUsername != null && !requestedUsername.isEmpty() && !requestedUsername.equals(existing.getUsername())) {
            if (repo.existsByUsername(requestedUsername)) {
                throw new IllegalArgumentException("Username is already taken");
            }
            existing.setUsername(requestedUsername);
        }

        String newPassword = req.getNewPassword() != null ? req.getNewPassword().trim() : null;
        if (newPassword != null && !newPassword.isEmpty()) {
            if (Boolean.TRUE.equals(existing.getIsAnonymous())) {
                throw new IllegalArgumentException("Anonymous account cannot change password");
            }

            String currentPassword = req.getCurrentPassword();
            if (currentPassword == null || currentPassword.isBlank()) {
                throw new IllegalArgumentException("Current password is required");
            }
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("New password must be at least 6 characters");
            }

            String storedHash = existing.getPasswordHash();
            boolean isCurrentPasswordValid = false;

            if (storedHash != null && !storedHash.isBlank()) {
                try {
                    isCurrentPasswordValid = passwordEncoder.matches(currentPassword, storedHash);
                } catch (IllegalArgumentException ignored) {
                    // Fallback for legacy plain-text values in older local databases.
                    isCurrentPasswordValid = storedHash.equals(currentPassword);
                }
            }

            if (!isCurrentPasswordValid) {
                throw new IllegalArgumentException("Current password is incorrect");
            }

            existing.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        existing.setUpdatedAt(java.time.LocalDateTime.now());
        return repo.save(existing);
    }

    public void delete(UUID id){ repo.deleteById(id); }
}
