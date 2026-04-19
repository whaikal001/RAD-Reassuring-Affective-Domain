package com.SocializerAI.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.SocializerAI.repository.UserRepository;
import com.SocializerAI.model.User;
import org.springframework.http.ResponseEntity;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/emotions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> getEmotionAnalytics() {
        // Return sample aggregated data - frontend can query and render charts
        List<Map<String, Object>> out = new ArrayList<>();

        Map<String, Object> point1 = new HashMap<>();
        point1.put("date", "2026-04-12");
        point1.put("joy", 0.62);
        point1.put("sadness", 0.12);
        point1.put("anger", 0.05);
        out.add(point1);

        Map<String, Object> point2 = new HashMap<>();
        point2.put("date", "2026-04-13");
        point2.put("joy", 0.55);
        point2.put("sadness", 0.18);
        point2.put("anger", 0.06);
        out.add(point2);

        Map<String, Object> point3 = new HashMap<>();
        point3.put("date", "2026-04-14");
        point3.put("joy", 0.68);
        point3.put("sadness", 0.1);
        point3.put("anger", 0.03);
        out.add(point3);

        return out;
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> listUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId().toString());
            m.put("username", u.getUsername());
            m.put("email", u.getEmail());
            m.put("isActive", u.getIsActive());
            m.put("isAnonymous", u.getIsAnonymous());
            m.put("roles", u.getRoles());
            out.add(m);
        }
        return out;
    }

    @PostMapping("/users/{id}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> banUser(@PathVariable("id") UUID id) {
        var u = userRepository.findById(id);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        User user = u.get();
        user.setIsActive(false);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{id}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unbanUser(@PathVariable("id") UUID id) {
        var u = userRepository.findById(id);
        if (u.isEmpty()) return ResponseEntity.notFound().build();
        User user = u.get();
        user.setIsActive(true);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable("id") UUID id) {
        if (!userRepository.existsById(id)) return ResponseEntity.notFound().build();
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}

