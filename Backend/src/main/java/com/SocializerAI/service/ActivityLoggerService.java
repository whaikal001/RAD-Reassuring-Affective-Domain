package com.SocializerAI.service;

import com.SocializerAI.model.UserActivityLog;
import com.SocializerAI.repository.UserActivityLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ActivityLoggerService {
    
    private final UserActivityLogRepository activityLogRepository;
    
    public ActivityLoggerService(UserActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }
    
    /**
     * Log a user activity
     */
    public void logActivity(UUID userId, String activityType, String description) {
        UserActivityLog log = new UserActivityLog(userId, activityType);
        log.setDescription(description);
        activityLogRepository.save(log);
    }
    
    /**
     * Log a user activity with emotion data
     */
    public void logActivityWithEmotion(UUID userId, String activityType, String description,
                                      String primaryEmotion, Double emotionScore) {
        UserActivityLog log = new UserActivityLog(userId, activityType);
        log.setDescription(description);
        log.setPrimaryEmotion(primaryEmotion);
        log.setEmotionScore(emotionScore);
        log.setIntensityLevel(calculateIntensityLevel(emotionScore));
        activityLogRepository.save(log);
    }
    
    /**
     * Log a chat session
     */
    public void logChatSession(UUID userId, String sessionId, Long durationSeconds, Integer messageCount,
                               String primaryEmotion, Double emotionScore) {
        UserActivityLog log = new UserActivityLog(userId, "SESSION");
        log.setSessionId(sessionId);
        log.setDurationSeconds(durationSeconds);
        log.setMessageCount(messageCount);
        log.setPrimaryEmotion(primaryEmotion);
        log.setEmotionScore(emotionScore);
        log.setIntensityLevel(calculateIntensityLevel(emotionScore));
        log.setDescription(messageCount + " messages in " + durationSeconds + " seconds");
        activityLogRepository.save(log);
    }
    
    /**
     * Log message sent
     */
    public void logMessage(UUID userId, String sessionId, String primaryEmotion, Double emotionScore) {
        UserActivityLog log = new UserActivityLog(userId, "MESSAGE");
        log.setSessionId(sessionId);
        log.setPrimaryEmotion(primaryEmotion);
        log.setEmotionScore(emotionScore);
        log.setIntensityLevel(calculateIntensityLevel(emotionScore));
        activityLogRepository.save(log);
    }
    
    /**
     * Log user login
     */
    public void logLogin(UUID userId, String ipAddress, String userAgent) {
        UserActivityLog log = new UserActivityLog(userId, "LOGIN");
        log.setDescription("User logged in");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        activityLogRepository.save(log);
    }
    
    /**
     * Log report view
     */
    public void logReportView(UUID userId) {
        UserActivityLog log = new UserActivityLog(userId, "REPORT_VIEW");
        log.setDescription("User viewed wellness report");
        activityLogRepository.save(log);
    }
    
    /**
     * Log screening test completion
     */
    public void logScreeningTest(UUID userId, String testName, String result) {
        UserActivityLog log = new UserActivityLog(userId, "SCREENING_TEST");
        log.setDescription("Completed " + testName + " - Result: " + result);
        activityLogRepository.save(log);
    }
    
    /**
     * Calculate intensity level based on emotion score
     */
    private String calculateIntensityLevel(Double emotionScore) {
        if (emotionScore == null) return null;
        if (emotionScore >= 0.7) return "HIGH";
        if (emotionScore >= 0.4) return "MEDIUM";
        return "LOW";
    }
    
    /**
     * Get user activity logs
     */
    public List<UserActivityLog> getUserActivityLogs(UUID userId) {
        return activityLogRepository.findByUserId(userId);
    }
    
    /**
     * Get recent activity
     */
    public List<UserActivityLog> getRecentActivity(int hoursAgo) {
        LocalDateTime since = LocalDateTime.now().minusHours(hoursAgo);
        return activityLogRepository.findRecentActivity(since);
    }
    
    /**
     * Get activity count by type
     */
    public List<Map<String, Object>> getActivityCountByType() {
        return activityLogRepository.countByActivityType();
    }
    
    /**
     * Get activity count by date
     */
    public List<Map<String, Object>> getActivityCountByDate(int daysAgo) {
        LocalDateTime since = LocalDateTime.now().minusDays(daysAgo);
        return activityLogRepository.countByDate(since);
    }
    
    /**
     * Get emotion distribution
     */
    public List<Map<String, Object>> getEmotionDistribution() {
        return activityLogRepository.emotionDistribution();
    }
    
    /**
     * Get intensity distribution
     */
    public List<Map<String, Object>> getIntensityDistribution() {
        return activityLogRepository.intensityDistribution();
    }
    
    /**
     * Get top countries
     */
    public List<Map<String, Object>> getTopCountries() {
        return activityLogRepository.topCountries();
    }
    
    /**
     * Get average emotion score
     */
    public Double getAverageEmotionScore() {
        return activityLogRepository.getAverageEmotionScore();
    }
    
    /**
     * Get total activities today
     */
    public long getTodayActivityCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return activityLogRepository.countByTimestampAfter(today);
    }
    
    /**
     * Get unique users today
     */
    public long getTodayUniqueUserCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return activityLogRepository.countUniqueUsersAfter(today);
    }
}
