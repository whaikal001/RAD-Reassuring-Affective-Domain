package com.radai.service.trajectory;

import com.radai.service.trajectory.MoodTrajectoryEngine.MoodPoint;
import com.radai.service.trajectory.MoodTrajectoryEngine.Trajectory;
import com.radai.service.trajectory.MoodTrajectoryEngine.Trend;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the MoodTrajectoryEngine: trend classification (improving = falling intensity),
 * volatility detection, the R² confidence, and the clamped forecast.
 */
class MoodTrajectoryEngineTest {

    private final MoodTrajectoryEngine engine = new MoodTrajectoryEngine();

    private static List<MoodPoint> points(int... intensities) {
        return Arrays.stream(intensities).mapToObj(MoodPoint::new).toList();
    }

    @Test
    void tooFewSamplesIsInsufficientData() {
        Trajectory t = engine.analyze(points(6, 4));
        assertEquals(Trend.INSUFFICIENT_DATA, t.trend());
        assertEquals(0.0, t.confidence(), 1e-9);
        assertEquals(2, t.sampleSize());
    }

    @Test
    void fallingIntensityIsImproving() {
        Trajectory t = engine.analyze(points(8, 6, 4, 2));
        assertEquals(Trend.IMPROVING, t.trend());
        assertTrue(t.slope() < 0);
        assertEquals(1.0, t.rSquared(), 1e-9); // perfectly linear
        assertEquals(0.0, t.forecast(), 1e-9); // 8 + (-2)*4 = 0, clamped
    }

    @Test
    void risingIntensityIsWorsening() {
        Trajectory t = engine.analyze(points(2, 4, 6, 8));
        assertEquals(Trend.WORSENING, t.trend());
        assertTrue(t.slope() > 0);
    }

    @Test
    void flatIntensityIsStableWithFullConfidence() {
        Trajectory t = engine.analyze(points(5, 5, 5, 5));
        assertEquals(Trend.STABLE, t.trend());
        assertEquals(0.0, t.volatility(), 1e-9);
        assertEquals(1.0, t.confidence(), 1e-9);
        assertEquals(5.0, t.forecast(), 1e-9);
    }

    @Test
    void oscillatingSeriesIsVolatile() {
        Trajectory t = engine.analyze(points(1, 9, 1, 9, 1));
        assertEquals(Trend.VOLATILE, t.trend());
        assertTrue(t.volatility() >= MoodTrajectoryEngine.DEFAULT_VOLATILITY_THRESHOLD);
    }

    @Test
    void forecastIsClampedToScale() {
        // Steeply rising series would forecast above 10; must clamp.
        Trajectory t = engine.analyze(points(4, 6, 8, 10));
        assertTrue(t.forecast() <= 10.0);
        assertTrue(t.forecast() >= 0.0);
    }

    @Test
    void confidenceReflectsFitQuality() {
        // A gently-declining series with small wobble: IMPROVING (not volatile) with r2 below 1.
        Trajectory t = engine.analyze(points(9, 8, 6, 5, 3, 2));
        assertEquals(Trend.IMPROVING, t.trend());
        assertTrue(t.rSquared() < 1.0);
        assertTrue(t.confidence() == t.rSquared()); // linear trends report R² as confidence
        assertTrue(t.confidence() <= 1.0 && t.confidence() >= 0.0);
    }
}
