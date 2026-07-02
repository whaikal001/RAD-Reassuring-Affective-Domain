package com.radai.service.crisis;

import com.radai.service.config.EngineTuning;
import com.radai.service.ml.MlGateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CrisisDetectionEngine {

    public static final List<String> DEFAULT_EXPLICIT_SUICIDE = List.of(
        "kill myself", "killing myself", "suicide", "suicidal", "want to die", "wanna die",
        "end my life", "take my own life", "end it all", "wish i was dead", "wish i were dead",
        // Malay
        "nak mati", "mahu mati", "hendak mati", "ingin mati", "nak bunuh diri", "bunuh diri",
        "tak mahu hidup", "tak ingin hidup", "tidak mahu hidup");

    public static final List<String> DEFAULT_PASSIVE_IDEATION = List.of(
        "no reason to live", "no point in living", "nothing to live for", "better off without me",
        "better off dead", "can't go on", "cannot go on", "don't want to be here",
        "dont want to be here", "tired of living", "give up on life", "end the pain",
        "want to disappear", "wish i could disappear",
        // Malay
        "tiada sebab untuk hidup", "tak ada sebab untuk hidup", "lebih baik saya tiada",
        "penat hidup", "tak larat nak hidup", "tak sanggup lagi", "hilang harapan sepenuhnya");

    public static final List<String> DEFAULT_SELF_HARM = List.of(
        "hurt myself", "harm myself", "cut myself", "cutting myself", "hurting myself",
        // Malay
        "cederakan diri", "sakiti diri", "lukakan diri");

    public static final List<String> DEFAULT_SAFETY_CONCERN = List.of(
        "not safe", "am i safe", "feeling unsafe", "unsafe", "dangerous", "in danger",
        // Malay
        "tak selamat", "tidak selamat", "dalam bahaya");

    public static final List<String> DEFAULT_DISTRESS = List.of(
        "emergency", "crisis", "help me", "i need help now",
        // Malay
        "kecemasan", "tolong saya", "bahaya");

    // Zero-shot candidate labels for the ML classifier (facebook/bart-large-mnli, multi-label).
    public static final String LABEL_IDEATION = "suicidal thoughts or self-harm";
    public static final String LABEL_CRISIS = "emotional crisis or severe distress";
    public static final String LABEL_NONE = "general conversation with little or no distress";
    public static final List<String> CANDIDATE_LABELS = List.of(LABEL_IDEATION, LABEL_CRISIS, LABEL_NONE);

    private final List<String> explicitSuicide;
    private final List<String> passiveIdeation;
    private final List<String> selfHarm;
    private final List<String> safetyConcern;
    private final List<String> distress;

    public CrisisDetectionEngine() {
        this(DEFAULT_EXPLICIT_SUICIDE, DEFAULT_PASSIVE_IDEATION, DEFAULT_SELF_HARM,
            DEFAULT_SAFETY_CONCERN, DEFAULT_DISTRESS);
    }

    public CrisisDetectionEngine(List<String> explicitSuicide, List<String> passiveIdeation,
                                 List<String> selfHarm, List<String> safetyConcern, List<String> distress) {
        this.explicitSuicide = List.copyOf(explicitSuicide);
        this.passiveIdeation = List.copyOf(passiveIdeation);
        this.selfHarm = List.copyOf(selfHarm);
        this.safetyConcern = List.copyOf(safetyConcern);
        this.distress = List.copyOf(distress);
    }

    /**
     * Classify the crisis level of a message. ML-primary (zero-shot) with the keyword rules kept as
     * a guaranteed floor: any ideation-bearing rule category <b>or</b> a confident ML signal flags
     * {@code suicidalIdeation} / {@link Severity#CRISIS}. If ML is unavailable or fails, the result
     * is exactly the rule-based classification (fail-safe).
     */
    public CrisisResult detect(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        List<String> reasons = new ArrayList<>();

        // --- Rule-based floor (keyword lexicons, EN + Malay) ---
        boolean explicit = anyMatch(lower, explicitSuicide, "explicit_suicide", reasons);
        boolean passive = anyMatch(lower, passiveIdeation, "passive_ideation", reasons);
        boolean self = anyMatch(lower, selfHarm, "self_harm", reasons);
        boolean safety = anyMatch(lower, safetyConcern, "safety_concern", reasons);
        boolean distressed = anyMatch(lower, distress, "distress", reasons);

        // --- ML signal (recall on indirect phrasing the keywords miss) ---
        boolean mlIdeation = false;
        boolean mlCrisis = false;
        double mlConfidence = 0.0;
        if (MlGateway.zeroShotAvailable() && text != null && !text.isBlank()) {
            Map<String, Double> scores = MlGateway.classifyZeroShot(text, CANDIDATE_LABELS);
            if (!scores.isEmpty()) {
                double ideationScore = scores.getOrDefault(LABEL_IDEATION, 0.0);
                double crisisScore = scores.getOrDefault(LABEL_CRISIS, 0.0);
                if (ideationScore >= EngineTuning.mlCrisisIdeationThreshold) {
                    mlIdeation = true;
                    mlConfidence = Math.max(mlConfidence, ideationScore);
                    reasons.add(String.format("ml_ideation:%.2f", ideationScore));
                } else if (crisisScore >= EngineTuning.mlCrisisThreshold) {
                    mlCrisis = true;
                    mlConfidence = Math.max(mlConfidence, crisisScore);
                    reasons.add(String.format("ml_crisis:%.2f", crisisScore));
                }
            }
        }

        // --- Combine: ML OR rules (ML never lowers a rule flag) ---
        boolean ideation = explicit || passive || self || safety || mlIdeation;
        boolean crisis = ideation || distressed || mlCrisis;

        Severity severity;
        double confidence;
        if (explicit || self) {
            severity = Severity.CRISIS;
            confidence = 0.95;
        } else if (mlIdeation) {
            severity = Severity.CRISIS;
            confidence = Math.max(0.75, mlConfidence);
        } else if (passive) {
            severity = Severity.CRISIS;
            confidence = 0.75;
        } else if (safety || mlCrisis) {
            severity = Severity.CRISIS;
            confidence = Math.max(0.65, mlConfidence);
        } else if (distressed) {
            severity = Severity.ELEVATED;
            confidence = 0.5;
        } else {
            severity = Severity.NONE;
            confidence = 0.0;
        }

        return new CrisisResult(crisis, ideation, severity, confidence, reasons);
    }

    private static boolean anyMatch(String lower, List<String> phrases, String tag, List<String> reasons) {
        boolean matched = false;
        for (String phrase : phrases) {
            if (lower.contains(phrase)) {
                reasons.add(tag + ":" + phrase);
                matched = true;
            }
        }
        return matched;
    }

    /** Discrete crisis severity. */
    public enum Severity {
        NONE,
        ELEVATED,
        CRISIS
    }

    /**
     * Outcome of crisis detection.
     *
     * @param crisis           true if any crisis or distress signal fired (run the crisis path)
     * @param suicidalIdeation true if ideation/self-harm/safety language fired
     * @param severity         discrete severity
     * @param confidence       0..1 confidence in the assessment
     * @param reasons          the matched phrase tags
     */
    public record CrisisResult(boolean crisis, boolean suicidalIdeation, Severity severity,
                               double confidence, List<String> reasons) {}
}
