package com.radai.service.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CopingStrategyEngine {

    /** Tag meaning "applies to any emotion / any stressor". */
    public static final String ANY = "any";

    public static final double DEFAULT_BASE = 0.5;
    public static final double DEFAULT_EMOTION_WEIGHT = 2.0;
    public static final double DEFAULT_STRESSOR_WEIGHT = 1.5;
    public static final double DEFAULT_INTENSITY_WEIGHT = 1.0;
    public static final double DEFAULT_INTENSITY_PENALTY = 0.25;

    private final List<Strategy> catalog;
    private final double base;
    private final double emotionWeight;
    private final double stressorWeight;
    private final double intensityWeight;
    private final double intensityPenalty;
    private final double maxScore; // used to normalise relevance to 0..1

    public CopingStrategyEngine() {
        this(defaultCatalog(), DEFAULT_BASE, DEFAULT_EMOTION_WEIGHT, DEFAULT_STRESSOR_WEIGHT,
            DEFAULT_INTENSITY_WEIGHT, DEFAULT_INTENSITY_PENALTY);
    }

    public CopingStrategyEngine(List<Strategy> catalog, double base, double emotionWeight,
                                double stressorWeight, double intensityWeight, double intensityPenalty) {
        if (catalog == null || catalog.isEmpty()) {
            throw new IllegalArgumentException("catalog must not be empty");
        }
        this.catalog = List.copyOf(catalog);
        this.base = base;
        this.emotionWeight = emotionWeight;
        this.stressorWeight = stressorWeight;
        this.intensityWeight = intensityWeight;
        this.intensityPenalty = intensityPenalty;
        this.maxScore = base + emotionWeight + stressorWeight + intensityWeight;
    }

    public List<Recommendation> recommend(String emotion, String stressor, int intensity, int topN) {
        String e = normalize(emotion);
        String s = normalize(stressor);
        int clampedIntensity = Math.max(0, Math.min(10, intensity));

        List<Recommendation> scored = new ArrayList<>();
        for (Strategy strategy : catalog) {
            double score = base;

            if (matches(strategy.emotions(), e)) {
                score += emotionWeight;
            }
            if (matches(strategy.stressors(), s)) {
                score += stressorWeight;
            }

            if (clampedIntensity >= strategy.minIntensity() && clampedIntensity <= strategy.maxIntensity()) {
                score += intensityWeight;
            } else {
                int distance = clampedIntensity < strategy.minIntensity()
                    ? strategy.minIntensity() - clampedIntensity
                    : clampedIntensity - strategy.maxIntensity();
                score -= intensityPenalty * distance;
            }

            double relevance = Math.max(0.0, Math.min(1.0, score / maxScore));
            scored.add(new Recommendation(strategy, score, relevance));
        }

        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        return scored.subList(0, Math.min(topN, scored.size()));
    }

    private static boolean matches(Set<String> tags, String value) {
        if (tags.contains(ANY)) {
            return true;
        }
        return value != null && tags.contains(value);
    }

    private static String normalize(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        return v.trim().toLowerCase(Locale.ROOT);
    }

    public static List<Strategy> defaultCatalog() {
        List<Strategy> c = new ArrayList<>();

        c.add(new Strategy("anx_breathing", "Use a 4-6 breathing cycle: inhale for 4 seconds, exhale for 6 seconds for 2 minutes.",
            Set.of("anxiety", "stress"), Set.of(ANY), 4, 10));
        c.add(new Strategy("anx_grounding", "Try the 5-4-3-2-1 grounding exercise to return attention to the present moment.",
            Set.of("anxiety"), Set.of(ANY), 5, 10));
        c.add(new Strategy("anx_nextstep", "Reduce uncertainty by writing one immediate next step instead of solving everything at once.",
            Set.of("anxiety", "stress"), Set.of("academics", "work"), 3, 8));

        c.add(new Strategy("str_chunk", "Break tasks into one 10-minute action and one optional follow-up action.",
            Set.of("stress"), Set.of("academics", "work"), 3, 8));
        c.add(new Strategy("str_control", "Name what is controllable today versus what is not controllable today.",
            Set.of("stress", "anxiety"), Set.of(ANY), 4, 9));
        c.add(new Strategy("str_reset", "Do a short body reset: drink water, unclench your jaw, drop your shoulders, then restart.",
            Set.of("stress", "anger"), Set.of(ANY), 2, 10));

        c.add(new Strategy("sad_activation", "Use gentle activation: one shower, one meal, one short walk.",
            Set.of("sadness", "exhaustion"), Set.of(ANY), 3, 9));
        c.add(new Strategy("sad_connect", "Message one trusted person with a simple check-in instead of waiting to feel ready.",
            Set.of("sadness", "loneliness"), Set.of("family", "friendship", "relationship"), 3, 10));

        c.add(new Strategy("ang_pause", "Pause before replying and label the trigger in one sentence.",
            Set.of("anger"), Set.of(ANY), 4, 10));
        c.add(new Strategy("ang_boundary", "Convert the frustration into a boundary request using calm, concrete wording.",
            Set.of("anger"), Set.of("relationship", "work", "family"), 3, 8));

        c.add(new Strategy("lon_connect", "Schedule one low-pressure connection today: a text, a short call, or a shared activity.",
            Set.of("loneliness", "sadness"), Set.of("friendship", "family"), 2, 9));

        c.add(new Strategy("gen_habits", "Track sleep, hydration, and movement to prevent emotional dips.",
            Set.of(ANY), Set.of(ANY), 0, 4));
        c.add(new Strategy("gen_reflect", "Use a weekly reflection: what helped, what drained, what to repeat.",
            Set.of(ANY), Set.of(ANY), 0, 5));

        return c;
    }

    public record Strategy(String id, String text, Set<String> emotions, Set<String> stressors,
                           int minIntensity, int maxIntensity) {}

    /**
     * A ranked recommendation.
     *
     * @param strategy  the recommended strategy
     * @param score     the raw additive score
     * @param relevance the score normalised to 0..1 (confidence of the match)
     */
    public record Recommendation(Strategy strategy, double score, double relevance) {}
}
