package com.radai.service.ml;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class MlGateway {

    private MlGateway() {
    }

    public interface EmotionFn { EmotionResult classify(String text); }
    public interface ZeroShotFn { Map<String, Double> classify(String text, List<String> labels); }
    public interface SafetyFn { double toxicity(String text); }

    private static volatile boolean enabled = false;
    private static volatile EmotionFn emotionFn;
    private static volatile ZeroShotFn zeroShotFn;
    private static volatile SafetyFn safetyFn;

    private static final int MAX_CACHE = 256;
    private static final Map<String, EmotionResult> emotionCache = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Double>> zeroShotCache = new ConcurrentHashMap<>();
    private static final Map<String, Double> safetyCache = new ConcurrentHashMap<>();

    /** Wire the ML functions in (called once at startup). */
    public static void configure(boolean on, EmotionFn emotion, ZeroShotFn zeroShot, SafetyFn safety) {
        enabled = on;
        emotionFn = emotion;
        zeroShotFn = zeroShot;
        safetyFn = safety;
    }

    public static void disable() {
        enabled = false;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static boolean emotionAvailable() {
        return enabled && emotionFn != null;
    }

    public static boolean zeroShotAvailable() {
        return enabled && zeroShotFn != null;
    }

    public static boolean safetyAvailable() {
        return enabled && safetyFn != null;
    }

    /** ML emotion label + score, or {@code null} if unavailable / failed. */
    public static EmotionResult classifyEmotion(String text) {
        if (!emotionAvailable() || isBlank(text)) {
            return null;
        }
        String key = cap(text);
        EmotionResult cached = emotionCache.get(key);
        if (cached != null) {
            return cached;
        }
        EmotionResult r = safe(() -> emotionFn.classify(text));
        if (r != null) {
            put(emotionCache, key, r);
        }
        return r;
    }

    /** ML zero-shot label → probability map, or an empty map if unavailable / failed. */
    public static Map<String, Double> classifyZeroShot(String text, List<String> labels) {
        if (!zeroShotAvailable() || isBlank(text)) {
            return Map.of();
        }
        String key = cap(text);
        Map<String, Double> cached = zeroShotCache.get(key);
        if (cached != null) {
            return cached;
        }
        Map<String, Double> r = safe(() -> zeroShotFn.classify(text, labels));
        if (r != null && !r.isEmpty()) {
            put(zeroShotCache, key, r);
            return r;
        }
        return Map.of();
    }

    /** ML toxicity score 0..1, or {@code -1} if unavailable / failed. */
    public static double classifySafety(String text) {
        if (!safetyAvailable() || isBlank(text)) {
            return -1.0;
        }
        String key = cap(text);
        Double cached = safetyCache.get(key);
        if (cached != null) {
            return cached;
        }
        Double r = safe(() -> safetyFn.toxicity(text));
        if (r != null) {
            put(safetyCache, key, r);
            return r;
        }
        return -1.0;
    }

    private static <T> T safe(Supplier<T> call) {
        try {
            return call.get();
        } catch (Exception e) {
            return null; // fail-safe: caller falls back to its rule-based floor
        }
    }

    private static <K, V> void put(Map<K, V> cache, K key, V value) {
        if (cache.size() >= MAX_CACHE) {
            cache.clear();
        }
        cache.put(key, value);
    }

    private static String cap(String text) {
        return text.length() > 512 ? text.substring(0, 512) : text;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** ML emotion result. */
    public record EmotionResult(String label, double score) {}
}
