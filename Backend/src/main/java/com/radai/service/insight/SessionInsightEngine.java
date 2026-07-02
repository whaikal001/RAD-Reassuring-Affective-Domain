package com.radai.service.insight;

import java.util.Locale;

public class SessionInsightEngine {

    /** Improvement % at/above which (with a low current intensity) a session counts as improved. */
    public static final double DEFAULT_IMPROVED_PCT = 20.0;
    /** Negative improvement % at/below which a session counts as worsened. */
    public static final double DEFAULT_WORSENED_PCT = -20.0;
    /** Current intensity at/above which a session counts as worsened regardless of delta. */
    public static final int DEFAULT_WORSENED_INTENSITY = 8;
    /** Current intensity at/below which an improvement is considered stabilised. */
    public static final int DEFAULT_STABLE_INTENSITY = 4;

    private final double improvedPct;
    private final double worsenedPct;
    private final int worsenedIntensity;
    private final int stableIntensity;

    public SessionInsightEngine() {
        this(DEFAULT_IMPROVED_PCT, DEFAULT_WORSENED_PCT, DEFAULT_WORSENED_INTENSITY, DEFAULT_STABLE_INTENSITY);
    }

    public SessionInsightEngine(double improvedPct, double worsenedPct, int worsenedIntensity, int stableIntensity) {
        this.improvedPct = improvedPct;
        this.worsenedPct = worsenedPct;
        this.worsenedIntensity = worsenedIntensity;
        this.stableIntensity = stableIntensity;
    }

 
    public SessionInsight summarize(int firstIntensity, int currentIntensity, int cycleCount,
                                    int approachSwitches, boolean crisis, String dominantEmotion, String language) {
        boolean isMalay = language != null && language.toLowerCase(Locale.ROOT).startsWith("ms");
        double improvementPct = (firstIntensity > 0)
            ? ((firstIntensity - currentIntensity) / (double) firstIntensity) * 100.0
            : 0.0;

        Status status;
        if (crisis) {
            status = Status.CRISIS;
        } else if (improvementPct >= improvedPct && currentIntensity <= stableIntensity) {
            status = Status.IMPROVED;
        } else if (improvementPct <= worsenedPct || currentIntensity >= worsenedIntensity) {
            status = Status.WORSENED;
        } else {
            status = Status.STABLE;
        }

        String emotion = (dominantEmotion == null || dominantEmotion.isBlank()) ? "—" : dominantEmotion;
        String summary = buildSummary(isMalay, status, firstIntensity, currentIntensity, cycleCount,
            approachSwitches, improvementPct, emotion);
        String recommendation = buildRecommendation(isMalay, status);

        return new SessionInsight(status, round1(improvementPct), summary, recommendation);
    }

    private String buildSummary(boolean isMalay, Status status, int first, int current, int cycles,
                                int switches, double pct, String emotion) {
        long rounded = Math.round(Math.abs(pct));
        if (isMalay) {
            String arah = status == Status.WORSENED ? "meningkat" : (status == Status.IMPROVED ? "menurun" : "stabil");
            return "Sepanjang " + cycles + " pertukaran, intensiti anda " + arah + " dari " + first + " ke "
                + current + " (" + rounded + "%). Emosi utama: " + emotion + ". Pertukaran pendekatan: " + switches + ".";
        }
        String dir = status == Status.WORSENED ? "rose" : (status == Status.IMPROVED ? "eased" : "held steady");
        return "Over " + cycles + " exchanges, your intensity " + dir + " from " + first + " to " + current
            + " (" + rounded + "%). Main emotion: " + emotion + ". Approach switches: " + switches + ".";
    }

    private String buildRecommendation(boolean isMalay, Status status) {
        return switch (status) {
            case CRISIS -> isMalay
                ? "Keselamatan diutamakan — sila gunakan sumber bantuan dan hubungi seseorang yang dipercayai."
                : "Safety first — please use the help resources and reach out to someone you trust.";
            case IMPROVED -> isMalay
                ? "Bagus. Kekalkan satu langkah kecil yang berkesan hari ini."
                : "Nice progress. Keep one small step that worked for you today.";
            case WORSENED -> isMalay
                ? "Ia masih berat. Pertimbangkan untuk bercakap dengan seseorang profesional."
                : "Things still feel heavy. Consider talking with a professional for extra support.";
            case STABLE -> isMalay
                ? "Anda stabil. Satu rutin kecil boleh membantu mengekalkannya."
                : "You're steady. One small routine can help you maintain this.";
        };
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    /** Session outcome status. */
    public enum Status {
        IMPROVED,
        STABLE,
        WORSENED,
        CRISIS
    }

    public record SessionInsight(Status status, double improvementPct, String summary, String recommendation) {}
}
