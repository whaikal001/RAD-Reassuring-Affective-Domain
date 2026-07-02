package com.radai.service;

import org.springframework.stereotype.Service;
import com.radai.repository.EmotionalHistoryRepository;
import com.radai.repository.ConversationRepository;
import com.radai.repository.ReportRepository;
import com.radai.model.Report;
import com.radai.service.trajectory.MoodTrajectoryEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ReportService {
    private final EmotionalHistoryRepository historyRepo;
    private final ConversationRepository conversationRepo;
    private final ReportRepository reportRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportService(EmotionalHistoryRepository hr, ConversationRepository cr, ReportRepository rr){
        this.historyRepo = hr;
        this.conversationRepo = cr;
        this.reportRepo = rr;
    }

    public Report generateReport(UUID userId, LocalDateTime start, LocalDateTime end, String type) throws Exception {
        var history = historyRepo.findByUserIdOrderByLoggedAtAsc(userId).stream()
                .filter(h -> !h.getLoggedAt().isBefore(start) && !h.getLoggedAt().isAfter(end))
                .collect(Collectors.toList());

        int totalSessions = (int) conversationRepo.findByUserId(userId).stream()
            .filter(c -> c.getStartedAt() != null)
            .filter(c -> {
                LocalDateTime startedAt = c.getStartedAt();
                LocalDateTime endedAt = c.getEndedAt();

                // Count sessions that overlap with report period, not just sessions started in range.
                boolean startedBeforeOrOnEnd = !startedAt.isAfter(end);
                boolean endedAfterOrOnStart = endedAt == null || !endedAt.isBefore(start);
                return startedBeforeOrOnEnd && endedAfterOrOnStart;
            })
            .count();

        if (totalSessions == 0 && !history.isEmpty()) {
            // Fallback for legacy data where emotional history exists but conversation rows are incomplete.
            totalSessions = (int) history.stream()
                .map(h -> h.getLoggedAt().toLocalDate())
                .distinct()
                .count();
        }

        double avgIntensity = history.stream().mapToInt(h -> h.getIntensity()).average().orElse(0);
        double avgMoodScore = history.stream().mapToDouble(h -> h.getSentimentScore()).average().orElse(0);
        String dominantEmotion = history.stream().collect(Collectors.groupingBy(h -> h.getEmotionalState(), Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("neutral");

        Map<String, Long> emotionDistribution = history.stream()
            .collect(Collectors.groupingBy(h -> h.getEmotionalState(), Collectors.counting()));

        List<Map<String, Object>> intensityTrend = history.stream()
            .sorted(Comparator.comparing(h -> h.getLoggedAt()))
            .limit(24)
            .map(h -> {
                Map<String, Object> point = new HashMap<>();
                point.put("timestamp", h.getLoggedAt().toString());
                point.put("intensity", h.getIntensity());
                point.put("sentiment", h.getSentimentScore());
                point.put("emotion", h.getEmotionalState());
                return point;
            })
            .collect(Collectors.toList());

        // Mood trajectory over the (oldest→newest) intensity series: trend, forecast, confidence.
        MoodTrajectoryEngine trajectoryEngine = new MoodTrajectoryEngine();
        List<MoodTrajectoryEngine.MoodPoint> moodPoints = history.stream()
            .map(h -> new MoodTrajectoryEngine.MoodPoint(h.getIntensity()))
            .collect(Collectors.toList());
        MoodTrajectoryEngine.Trajectory trajectory = trajectoryEngine.analyze(moodPoints);

        Map<String, Object> moodTrajectory = new LinkedHashMap<>();
        moodTrajectory.put("trend", trajectory.trend().name());
        moodTrajectory.put("slope", trajectory.slope());
        moodTrajectory.put("forecastNextIntensity", trajectory.forecast());
        moodTrajectory.put("volatility", trajectory.volatility());
        moodTrajectory.put("rSquared", trajectory.rSquared());
        moodTrajectory.put("confidence", trajectory.confidence());
        moodTrajectory.put("sampleSize", trajectory.sampleSize());

        Map<String,Object> summary = Map.of(
                "entries", history.size(),
            "totalSessions", totalSessions,
                "averageIntensity", avgIntensity,
            "averageMoodScore", avgMoodScore,
                "dominantEmotion", dominantEmotion,
            "emotionDistribution", emotionDistribution,
            "intensityTrend", intensityTrend,
            "moodTrajectory", moodTrajectory,
                "timeRange", start + " to " + end
        );

        Report r = new Report();
        r.setUserId(userId); r.setReportType(type); r.setStartDate(start); r.setEndDate(end);
        r.setTotalSessions(totalSessions);
        r.setAverageMoodScore(avgMoodScore);
        r.setSummary(mapper.writeValueAsString(summary));
        return reportRepo.save(r);
    }

    public List<Report> getReports(UUID userId){ return reportRepo.findByUserIdOrderByGeneratedAtDesc(userId); }

    public Report accessReport(UUID id){
        var r = reportRepo.findById(id).orElseThrow();
        // Report doesn't have accessedCount or lastAccessed fields, so removing these
        return r;
    }
}

