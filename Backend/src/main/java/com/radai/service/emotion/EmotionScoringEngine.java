package com.radai.service.emotion;

import com.radai.service.config.EngineTuning;
import com.radai.service.ml.MlGateway;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EmotionScoringEngine {

    /** Emotion returned when no keyword matches. */
    public static final String NEUTRAL = "neutral";

    /** Confidence reported for a no-match ({@code neutral}) result. */
    public static final double NEUTRAL_CONFIDENCE = 0.3;

    // Weighted lexicon: emotion -> (keyword -> weight). Multi-word phrases are more specific → heavier.
    private final Map<String, Map<String, Double>> lexicon;

    public EmotionScoringEngine() {
        this(defaultLexicon());
    }

    public EmotionScoringEngine(Map<String, Map<String, Double>> lexicon) {
        // Deep, unmodifiable copy so the engine is immutable.
        Map<String, Map<String, Double>> copy = new LinkedHashMap<>();
        lexicon.forEach((emotion, kw) -> copy.put(emotion, Map.copyOf(kw)));
        this.lexicon = Map.copyOf(copy);
    }

    public EmotionAssessment classify(String text) {
        EmotionAssessment lexicon = lexiconClassify(text);

        // Safety floor: keep a crisis emotion regardless of the model.
        if ("hopeless".equals(lexicon.emotion())) {
            return lexicon;
        }

        if (MlGateway.emotionAvailable() && text != null && !text.isBlank()) {
            MlGateway.EmotionResult ml = MlGateway.classifyEmotion(text);
            if (ml != null && ml.score() >= EngineTuning.mlEmotionMinScore) {
                String mapped = mapHfEmotion(ml.label());
                // Don't let an ML "neutral" erase a specific signal the lexicon already found.
                if (!(NEUTRAL.equals(mapped) && !NEUTRAL.equals(lexicon.emotion()))) {
                    return new EmotionAssessment(mapped, ml.score(), List.of(new Scored(mapped, ml.score())));
                }
            }
        }
        return lexicon;
    }

    /** Map the HuggingFace emotion model labels onto this project's emotion vocabulary. */
    private static String mapHfEmotion(String hfLabel) {
        if (hfLabel == null) {
            return NEUTRAL;
        }
        return switch (hfLabel.toLowerCase(Locale.ROOT)) {
            case "anger", "disgust" -> "anger";
            case "fear" -> "anxiety";
            case "joy" -> "joy";
            case "sadness" -> "sadness";
            default -> NEUTRAL; // neutral, surprise, or anything unrecognised
        };
    }

    /** The weighted-lexicon classification (the rule-based floor / fallback). */
    public EmotionAssessment lexiconClassify(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);

        Map<String, Double> scores = new LinkedHashMap<>();
        double total = 0.0;
        for (Map.Entry<String, Map<String, Double>> entry : lexicon.entrySet()) {
            double emotionScore = 0.0;
            for (Map.Entry<String, Double> kw : entry.getValue().entrySet()) {
                if (lower.contains(kw.getKey())) {
                    emotionScore += kw.getValue();
                }
            }
            if (emotionScore > 0) {
                scores.put(entry.getKey(), emotionScore);
                total += emotionScore;
            }
        }

        if (scores.isEmpty()) {
            return new EmotionAssessment(NEUTRAL, NEUTRAL_CONFIDENCE, List.of());
        }

        List<Scored> ranked = new ArrayList<>();
        scores.forEach((emotion, score) -> ranked.add(new Scored(emotion, score)));
        ranked.sort((a, b) -> Double.compare(b.score(), a.score()));

        Scored top = ranked.get(0);
        double confidence = top.score() / total; // winner's share of the evidence
        return new EmotionAssessment(top.emotion(), confidence, List.copyOf(ranked));
    }

    /** Convenience: just the winning emotion label (behaviour-compatible with the old fallback). */
    public String classifyLabel(String text) {
        return classify(text).emotion();
    }

    public static Map<String, Map<String, Double>> defaultLexicon() {
        Map<String, Map<String, Double>> l = new LinkedHashMap<>();

        l.put("hopeless", Map.ofEntries(
            Map.entry("suicide", 5.0), Map.entry("suicidal", 5.0), Map.entry("kill myself", 5.0),
            Map.entry("end my life", 5.0), Map.entry("want to die", 5.0), Map.entry("hurt myself", 4.0),
            Map.entry("harm myself", 4.0), Map.entry("not safe", 3.0), Map.entry("am i safe", 3.0),
            Map.entry("safe place", 3.0), Map.entry("unsafe", 3.0), Map.entry("dangerous", 3.0),
            Map.entry("nak mati", 5.0), Map.entry("mahu mati", 5.0),
            Map.entry("bunuh diri", 5.0), Map.entry("tak mahu hidup", 5.0)));

        l.put("stress", Map.ofEntries(
            Map.entry("stressed", 2.0), Map.entry("stress", 1.5), Map.entry("overwhelmed", 2.0),
            Map.entry("overwhelm", 1.5), Map.entry("pressure", 1.5), Map.entry("too much", 1.5),
            Map.entry("burnout", 2.0), Map.entry("burned out", 2.0)));

        l.put("anxiety", Map.ofEntries(
            Map.entry("anxiety", 2.0), Map.entry("anxious", 2.0), Map.entry("panic", 2.0),
            Map.entry("panicking", 2.0), Map.entry("worried", 1.5), Map.entry("worry", 1.5),
            Map.entry("afraid", 1.5), Map.entry("scared", 1.5), Map.entry("nervous", 1.5)));

        l.put("sadness", Map.ofEntries(
            Map.entry("depressed", 2.0), Map.entry("depression", 2.0), Map.entry("heartbroken", 2.0),
            Map.entry("sadness", 1.5), Map.entry("sad", 1.5), Map.entry("unhappy", 1.5),
            Map.entry("down", 1.0), Map.entry("blue", 1.0)));

        l.put("anger", Map.ofEntries(
            Map.entry("furious", 2.0), Map.entry("angry", 1.5), Map.entry("mad", 1.5),
            Map.entry("frustrated", 1.5), Map.entry("frustrating", 1.5), Map.entry("hate", 1.5)));

        l.put("exhaustion", Map.ofEntries(
            Map.entry("exhausted", 2.0), Map.entry("drained", 2.0), Map.entry("tired", 1.5),
            Map.entry("fatigue", 1.5)));

        l.put("loneliness", Map.ofEntries(
            Map.entry("lonely", 2.0), Map.entry("loneliness", 2.0), Map.entry("isolated", 1.5),
            Map.entry("alone", 1.5)));

        l.put("joy", Map.ofEntries(
            Map.entry("amazing", 2.0), Map.entry("wonderful", 2.0), Map.entry("excited", 1.5),
            Map.entry("grateful", 1.5), Map.entry("relieved", 1.5), Map.entry("happy", 1.5),
            Map.entry("great", 1.0)));

        return l;
    }

    /** One emotion and its accumulated weight. */
    public record Scored(String emotion, double score) {}

    public record EmotionAssessment(String emotion, double confidence, List<Scored> ranked) {}
}
