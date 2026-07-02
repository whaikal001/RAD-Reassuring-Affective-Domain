package com.radai.service.insight;

import com.radai.service.insight.SessionInsightEngine.SessionInsight;
import com.radai.service.insight.SessionInsightEngine.Status;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the SessionInsightEngine: status classification, improvement %, crisis override, the
 * unknown-opening guard, and localized summaries.
 */
class SessionInsightEngineTest {

    private final SessionInsightEngine engine = new SessionInsightEngine();

    @Test
    void bigDropToLowIntensityIsImproved() {
        SessionInsight i = engine.summarize(8, 3, 5, 1, false, "anxiety", "en");
        assertEquals(Status.IMPROVED, i.status());
        assertEquals(62.5, i.improvementPct(), 1e-6); // (8-3)/8*100
        assertTrue(i.summary().contains("eased"));
    }

    @Test
    void risingIntensityIsWorsened() {
        SessionInsight i = engine.summarize(4, 9, 6, 2, false, "stress", "en");
        assertEquals(Status.WORSENED, i.status());
        assertTrue(i.improvementPct() < 0);
    }

    @Test
    void highCurrentIntensityIsWorsenedEvenWithoutDrop() {
        SessionInsight i = engine.summarize(8, 8, 3, 0, false, "sadness", "en");
        assertEquals(Status.WORSENED, i.status()); // current >= 8
    }

    @Test
    void modestChangeIsStable() {
        SessionInsight i = engine.summarize(6, 5, 4, 1, false, "stress", "en");
        assertEquals(Status.STABLE, i.status());
    }

    @Test
    void crisisOverridesEverything() {
        SessionInsight i = engine.summarize(8, 2, 5, 1, true, "hopeless", "en");
        assertEquals(Status.CRISIS, i.status()); // even though intensity dropped a lot
    }

    @Test
    void unknownOpeningIntensityGivesZeroImprovement() {
        SessionInsight i = engine.summarize(0, 5, 2, 0, false, "neutral", "en");
        assertEquals(0.0, i.improvementPct(), 1e-9);
    }

    @Test
    void malaySummaryIsLocalized() {
        SessionInsight i = engine.summarize(8, 3, 5, 1, false, "anxiety", "ms");
        assertTrue(i.summary().contains("pertukaran"));
        assertTrue(i.recommendation().length() > 0);
    }
}
