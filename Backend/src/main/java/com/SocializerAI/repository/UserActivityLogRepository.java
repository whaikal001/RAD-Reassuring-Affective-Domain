package com.SocializerAI.repository;

import com.SocializerAI.model.UserActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, UUID> {
    
    List<UserActivityLog> findByUserId(UUID userId);
    
    List<UserActivityLog> findByUserIdAndTimestampBetween(UUID userId, LocalDateTime start, LocalDateTime end);
    
    List<UserActivityLog> findByActivityType(String activityType);
    
    List<UserActivityLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT ual FROM UserActivityLog ual WHERE ual.timestamp >= :since ORDER BY ual.timestamp DESC")
    List<UserActivityLog> findRecentActivity(@Param("since") LocalDateTime since);
    
    @Query(value = "SELECT activity_type, COUNT(*) as count FROM user_activity_logs GROUP BY activity_type", nativeQuery = true)
    List<Map<String, Object>> countByActivityType();
    
    @Query(value = "SELECT DATE(timestamp) as date, COUNT(*) as count FROM user_activity_logs WHERE timestamp >= :since GROUP BY DATE(timestamp) ORDER BY date DESC", nativeQuery = true)
    List<Map<String, Object>> countByDate(@Param("since") LocalDateTime since);
    
    @Query(value = "SELECT primary_emotion, COUNT(*) as count FROM user_activity_logs WHERE primary_emotion IS NOT NULL GROUP BY primary_emotion", nativeQuery = true)
    List<Map<String, Object>> emotionDistribution();
    
    @Query(value = "SELECT intensity_level, COUNT(*) as count FROM user_activity_logs WHERE intensity_level IS NOT NULL GROUP BY intensity_level", nativeQuery = true)
    List<Map<String, Object>> intensityDistribution();
    
    @Query(value = "SELECT country, COUNT(*) as count FROM user_activity_logs WHERE country IS NOT NULL GROUP BY country ORDER BY count DESC LIMIT 10", nativeQuery = true)
    List<Map<String, Object>> topCountries();
    
    @Query(value = "SELECT AVG(emotion_score) as avg_score FROM user_activity_logs WHERE emotion_score IS NOT NULL", nativeQuery = true)
    Double getAverageEmotionScore();
    
    long countByTimestampAfter(LocalDateTime timestamp);
    
    @Query(value = "SELECT COUNT(DISTINCT user_id) FROM user_activity_logs WHERE timestamp >= :since", nativeQuery = true)
    long countUniqueUsersAfter(@Param("since") LocalDateTime since);
}
