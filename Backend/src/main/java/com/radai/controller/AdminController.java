package com.radai.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.radai.repository.UserRepository;
import com.radai.repository.UserActivityLogRepository;
import com.radai.model.User;
import com.radai.model.UserActivityLog;
import com.radai.service.ActivityLoggerService;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final UserActivityLogRepository activityLogRepository;
    private final ActivityLoggerService activityLoggerService;

    public AdminController(UserRepository userRepository, UserActivityLogRepository activityLogRepository,
                         ActivityLoggerService activityLoggerService) {
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
        this.activityLoggerService = activityLoggerService;
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

    // ===== New Analytics Endpoints =====

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Total users
        stats.put("totalUsers", userRepository.count());
        
        // Active users today
        stats.put("activeTodayCount", activityLoggerService.getTodayUniqueUserCount());
        
        // Today activities
        stats.put("totalActivitiesCount", activityLoggerService.getTodayActivityCount());
        
        // Average emotion score
        Double avgEmotion = activityLoggerService.getAverageEmotionScore();
        stats.put("averageEmotionScore", avgEmotion != null ? avgEmotion : 0.0);
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/dashboard/activity-trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getActivityTrend(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> trend = activityLoggerService.getActivityCountByDate(days);
        return ResponseEntity.ok(trend);
    }

    /** Daily active users (distinct users per day) — the "total users using the app" trend. */
    @GetMapping("/dashboard/active-users-trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getActiveUsersTrend(@RequestParam(defaultValue = "30") int days) {
        List<Map<String, Object>> trend = activityLoggerService.getDailyActiveUsers(days);
        return ResponseEntity.ok(trend);
    }

    @GetMapping("/dashboard/activity-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getActivityTypeDistribution() {
        List<Map<String, Object>> distribution = activityLoggerService.getActivityCountByType();
        return ResponseEntity.ok(distribution);
    }

    @GetMapping("/dashboard/emotions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getEmotionDistribution() {
        List<Map<String, Object>> emotions = activityLoggerService.getEmotionDistribution();
        return ResponseEntity.ok(emotions);
    }

    @GetMapping("/dashboard/intensity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getIntensityDistribution() {
        List<Map<String, Object>> intensity = activityLoggerService.getIntensityDistribution();
        return ResponseEntity.ok(intensity);
    }

    @GetMapping("/dashboard/countries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getTopCountries() {
        List<Map<String, Object>> countries = activityLoggerService.getTopCountries();
        return ResponseEntity.ok(countries);
    }

    @GetMapping("/dashboard/recent-activity")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserActivityLog>> getRecentActivity(@RequestParam(defaultValue = "24") int hours) {
        List<UserActivityLog> recent = activityLoggerService.getRecentActivity(hours);
        return ResponseEntity.ok(recent);
    }

    @GetMapping("/dashboard/user-stats/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserActivityLog>> getUserStats(@PathVariable("userId") UUID userId) {
        List<UserActivityLog> logs = activityLoggerService.getUserActivityLogs(userId);
        return ResponseEntity.ok(logs);
    }
}


