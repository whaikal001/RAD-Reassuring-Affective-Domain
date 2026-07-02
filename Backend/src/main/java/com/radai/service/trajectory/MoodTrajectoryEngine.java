package com.radai.service.trajectory;

import java.util.List;

public class MoodTrajectoryEngine {

    public static final int DEFAULT_MIN_SAMPLES = 3;
    /** |slope| (intensity units per sample) below this is treated as flat. */
    public static final double DEFAULT_SLOPE_THRESHOLD = 0.3;
    /** Std-dev of successive deltas at/above this marks the series volatile. */
    public static final double DEFAULT_VOLATILITY_THRESHOLD = 2.5;

    private final int minSamples;
    private final double slopeThreshold;
    private final double volatilityThreshold;

    public MoodTrajectoryEngine() {
        // Sourced from EngineTuning (tunable via application.properties); defaults to the constants.
        this(com.radai.service.config.EngineTuning.trajectoryMinSamples,
             com.radai.service.config.EngineTuning.trajectorySlopeThreshold,
             com.radai.service.config.EngineTuning.trajectoryVolatilityThreshold);
    }

    public MoodTrajectoryEngine(int minSamples, double slopeThreshold, double volatilityThreshold) {
        if (minSamples < 2) {
            throw new IllegalArgumentException("minSamples must be >= 2");
        }
        this.minSamples = minSamples;
        this.slopeThreshold = slopeThreshold;
        this.volatilityThreshold = volatilityThreshold;
    }

    /**
     * Analyse an ordered (oldest → newest) list of mood points.
     */
    public Trajectory analyze(List<MoodPoint> points) {
        int n = points == null ? 0 : points.size();
        if (n < minSamples) {
            return new Trajectory(Trend.INSUFFICIENT_DATA, 0.0, 0.0, 0.0,
                n == 0 ? 0.0 : points.get(n - 1).intensity(), 0.0, n);
        }

        // Means.
        double meanX = (n - 1) / 2.0; // indices 0..n-1
        double meanY = 0.0;
        for (MoodPoint p : points) {
            meanY += p.intensity();
        }
        meanY /= n;

        // Least-squares slope/intercept and R².
        double sxx = 0.0;
        double sxy = 0.0;
        double ssTot = 0.0;
        for (int i = 0; i < n; i++) {
            double dx = i - meanX;
            double dy = points.get(i).intensity() - meanY;
            sxx += dx * dx;
            sxy += dx * dy;
            ssTot += dy * dy;
        }
        double slope = sxx == 0 ? 0.0 : sxy / sxx;
        double intercept = meanY - slope * meanX;

        double ssRes = 0.0;
        for (int i = 0; i < n; i++) {
            double predicted = intercept + slope * i;
            double residual = points.get(i).intensity() - predicted;
            ssRes += residual * residual;
        }
        // Flat data (no variance) is perfectly explained by a horizontal line.
        double r2 = ssTot == 0 ? 1.0 : Math.max(0.0, 1.0 - ssRes / ssTot);

        // Volatility = std-dev of successive deltas.
        double volatility = successiveDeltaStdDev(points);

        // Forecast one step past the last sample, clamped to the 0-10 scale.
        double forecast = clamp(intercept + slope * n);

        Trend trend;
        double confidence;
        if (volatility >= volatilityThreshold) {
            trend = Trend.VOLATILE;
            confidence = Math.min(1.0, volatility / (2.0 * volatilityThreshold));
        } else if (slope <= -slopeThreshold) {
            trend = Trend.IMPROVING;
            confidence = r2;
        } else if (slope >= slopeThreshold) {
            trend = Trend.WORSENING;
            confidence = r2;
        } else {
            trend = Trend.STABLE;
            confidence = r2;
        }

        return new Trajectory(trend, slope, r2, volatility, forecast, confidence, n);
    }

    private static double successiveDeltaStdDev(List<MoodPoint> points) {
        int n = points.size();
        if (n < 2) {
            return 0.0;
        }
        int m = n - 1;
        double mean = 0.0;
        for (int i = 1; i < n; i++) {
            mean += points.get(i).intensity() - points.get(i - 1).intensity();
        }
        mean /= m;
        double var = 0.0;
        for (int i = 1; i < n; i++) {
            double delta = points.get(i).intensity() - points.get(i - 1).intensity();
            var += (delta - mean) * (delta - mean);
        }
        var /= m;
        return Math.sqrt(var);
    }

    private static double clamp(double v) {
        return Math.max(0.0, Math.min(10.0, v));
    }

    /** A single observation in the series. Intensity is the 0-10 mood proxy (lower = better). */
    public record MoodPoint(int intensity) {}

    /** Trend classifications. */
    public enum Trend {
        IMPROVING,
        WORSENING,
        STABLE,
        VOLATILE,
        INSUFFICIENT_DATA
    }

    public record Trajectory(Trend trend, double slope, double rSquared, double volatility,
                             double forecast, double confidence, int sampleSize) {}
}
