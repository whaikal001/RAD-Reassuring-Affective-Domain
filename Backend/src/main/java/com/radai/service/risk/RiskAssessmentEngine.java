package com.radai.service.risk;

import com.radai.service.config.EngineTuning;
import com.radai.service.crisis.CrisisDetectionEngine;
import com.radai.service.ml.MlGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RiskAssessmentEngine {

    // ----- default trigger lexicons (EN + Malay), matching the original FlowWithAIService lists -----

    public static final List<String> DEFAULT_CRISIS_TRIGGERS = List.of(
        "kill myself", "end my life", "suicide", "want to die", "no reason to live", "hurt myself",
        "nak mati", "mahu mati", "nak bunuh diri", "bunuh diri", "tak mahu hidup", "tak ingin hidup");

    public static final List<String> DEFAULT_HIGH_RISK_TRIGGERS = List.of(
        "hopeless", "cannot go on", "can't go on", "worthless", "panic attack", "emergency");

    /** Regex (case-insensitive) for explicit self-harm language that forces a crisis floor. */
    public static final String DEFAULT_CRISIS_LANGUAGE_REGEX =
        ".*(want to die|end my life|kill myself|suicide|nak mati|mahu mati|nak bunuh diri|bunuh diri|tak mahu hidup|tak ingin hidup).*";

    // ----- default weights -----
    public static final double DEFAULT_CRISIS_TRIGGER_WEIGHT = 0.7;
    public static final double DEFAULT_HIGH_RISK_TRIGGER_WEIGHT = 0.2;
    public static final double DEFAULT_HIGH_INTENSITY_WEIGHT = 0.2;
    public static final double DEFAULT_CONTEXT_CRISIS_WEIGHT = 0.4;
    public static final double DEFAULT_CRISIS_LANGUAGE_FLOOR = 0.9;
    public static final int DEFAULT_HIGH_INTENSITY_THRESHOLD = 8;

    // ----- default level cut-offs -----
    public static final double DEFAULT_CRITICAL_CUT = 0.8;
    public static final double DEFAULT_HIGH_CUT = 0.45;
    public static final double DEFAULT_MODERATE_CUT = 0.2;

    private final List<String> crisisTriggers;
    private final List<String> highRiskTriggers;
    private final String crisisLanguageRegex;
    private final double crisisTriggerWeight;
    private final double highRiskTriggerWeight;
    private final double highIntensityWeight;
    private final double contextCrisisWeight;
    private final double crisisLanguageFloor;
    private final int highIntensityThreshold;
    private final double criticalCut;
    private final double highCut;
    private final double moderateCut;

    
    public RiskAssessmentEngine() {
        this(DEFAULT_CRISIS_TRIGGERS, DEFAULT_HIGH_RISK_TRIGGERS, DEFAULT_CRISIS_LANGUAGE_REGEX,
            DEFAULT_CRISIS_TRIGGER_WEIGHT, DEFAULT_HIGH_RISK_TRIGGER_WEIGHT, DEFAULT_HIGH_INTENSITY_WEIGHT,
            DEFAULT_CONTEXT_CRISIS_WEIGHT, DEFAULT_CRISIS_LANGUAGE_FLOOR, DEFAULT_HIGH_INTENSITY_THRESHOLD,
            com.radai.service.config.EngineTuning.riskCriticalCut,
            com.radai.service.config.EngineTuning.riskHighCut,
            com.radai.service.config.EngineTuning.riskModerateCut);
    }

    public RiskAssessmentEngine(List<String> crisisTriggers, List<String> highRiskTriggers,
                                String crisisLanguageRegex, double crisisTriggerWeight,
                                double highRiskTriggerWeight, double highIntensityWeight,
                                double contextCrisisWeight, double crisisLanguageFloor,
                                int highIntensityThreshold, double criticalCut, double highCut,
                                double moderateCut) {
        if (!(moderateCut <= highCut && highCut <= criticalCut)) {
            throw new IllegalArgumentException("cut-offs must satisfy moderate <= high <= critical");
        }
        this.crisisTriggers = List.copyOf(crisisTriggers);
        this.highRiskTriggers = List.copyOf(highRiskTriggers);
        this.crisisLanguageRegex = crisisLanguageRegex;
        this.crisisTriggerWeight = crisisTriggerWeight;
        this.highRiskTriggerWeight = highRiskTriggerWeight;
        this.highIntensityWeight = highIntensityWeight;
        this.contextCrisisWeight = contextCrisisWeight;
        this.crisisLanguageFloor = crisisLanguageFloor;
        this.highIntensityThreshold = highIntensityThreshold;
        this.criticalCut = criticalCut;
        this.highCut = highCut;
        this.moderateCut = moderateCut;
    }
    public RiskResult assess(String userMessage, int intensity, boolean contextCrisis) {
        String lower = userMessage == null ? "" : userMessage.toLowerCase(Locale.ROOT);
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        for (String trigger : crisisTriggers) {
            if (lower.contains(trigger)) {
                score += crisisTriggerWeight;
                reasons.add("crisis_trigger:" + trigger);
            }
        }
        for (String trigger : highRiskTriggers) {
            if (lower.contains(trigger)) {
                score += highRiskTriggerWeight;
                reasons.add("high_risk_trigger:" + trigger);
            }
        }
        if (intensity >= highIntensityThreshold) {
            score += highIntensityWeight;
            reasons.add("high_intensity");
        }
        if (contextCrisis) {
            score += contextCrisisWeight;
            reasons.add("context_crisis");
        }
        if (crisisLanguageRegex != null && lower.matches(crisisLanguageRegex)) {
            score = Math.max(score, crisisLanguageFloor);
            reasons.add("matched_crisis_language");
        }

        double mlRisk = mlRiskSignal(userMessage);
        if (mlRisk > score) {
            reasons.add(String.format("ml_risk:%.2f", mlRisk));
            score = mlRisk;
        }

        score = Math.min(1.0, score);
        String level = level(score);
        boolean crisis = level.equals("critical");
        double confidence = bandConfidence(score);
        return new RiskResult(level, crisis, score, confidence, reasons);
    }

    private double mlRiskSignal(String text) {
        if (text == null || text.isBlank()) {
            return 0.0;
        }
        double ml = 0.0;

        if (MlGateway.zeroShotAvailable()) {
            Map<String, Double> zs = MlGateway.classifyZeroShot(text, CrisisDetectionEngine.CANDIDATE_LABELS);
            if (!zs.isEmpty()) {
                double ideation = zs.getOrDefault(CrisisDetectionEngine.LABEL_IDEATION, 0.0);
                double crisis = zs.getOrDefault(CrisisDetectionEngine.LABEL_CRISIS, 0.0);
                if (ideation >= EngineTuning.mlCrisisIdeationThreshold) {
                    ml = Math.max(ml, ideation);
                } else if (crisis >= EngineTuning.mlCrisisThreshold) {
                    ml = Math.max(ml, crisis * 0.9); // severe distress: strong but below explicit ideation
                }
            }
        }

        if (MlGateway.safetyAvailable()) {
            double toxicity = MlGateway.classifySafety(text); // -1 when unavailable
            if (toxicity >= 0.85) {
                ml = Math.max(ml, toxicity * 0.5); // very toxic language is a weak risk proxy
            }
        }

        return ml;
    }

    private String level(double score) {
        if (score >= criticalCut) return "critical";
        if (score >= highCut) return "high";
        if (score >= moderateCut) return "moderate";
        return "low";
    }

    private double bandConfidence(double score) {
        double lo;
        double hi;
        if (score >= criticalCut) {
            lo = criticalCut; hi = 1.0;
        } else if (score >= highCut) {
            lo = highCut; hi = criticalCut;
        } else if (score >= moderateCut) {
            lo = moderateCut; hi = highCut;
        } else {
            lo = 0.0; hi = moderateCut;
        }
        double halfWidth = (hi - lo) / 2.0;
        if (halfWidth <= 0) {
            return 1.0;
        }
        double margin = Math.min(score - lo, hi - score);
        double confidence = 0.5 + 0.5 * (margin / halfWidth);
        return Math.max(0.5, Math.min(1.0, confidence));
    }

    public record RiskResult(String level, boolean crisis, double score, double confidence, List<String> reasons) {}
}
