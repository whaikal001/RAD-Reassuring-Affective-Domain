package com.radai.service.strategy;

import com.radai.service.strategy.CopingStrategyEngine.Recommendation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the CopingStrategyEngine recommender: emotion/stressor targeting, intensity fit,
 * ranking, top-N limiting, and the normalised relevance.
 */
class CopingStrategyEngineTest {

    private final CopingStrategyEngine engine = new CopingStrategyEngine();

    @Test
    void recommendsEmotionMatchingStrategyFirst() {
        List<Recommendation> recs = engine.recommend("anxiety", "academics", 7, 3);
        assertFalse(recs.isEmpty());
        // The top pick must actually target anxiety (or "any").
        var top = recs.get(0).strategy();
        assertTrue(top.emotions().contains("anxiety") || top.emotions().contains(CopingStrategyEngine.ANY));
    }

    @Test
    void respectsTopNLimit() {
        assertEquals(2, engine.recommend("stress", "work", 6, 2).size());
        assertEquals(1, engine.recommend("stress", "work", 6, 1).size());
    }

    @Test
    void resultsAreSortedByScoreDescending() {
        List<Recommendation> recs = engine.recommend("sadness", "family", 5, 5);
        for (int i = 1; i < recs.size(); i++) {
            assertTrue(recs.get(i - 1).score() >= recs.get(i).score());
        }
    }

    @Test
    void emotionAndStressorMatchOutranksGenericHabit() {
        // "sad_connect" targets sadness + family; generic habit targets any/any at low intensity.
        List<Recommendation> recs = engine.recommend("sadness", "family", 6, 12);
        double connect = scoreOf(recs, "sad_connect");
        double genericHabit = scoreOf(recs, "gen_habits");
        assertTrue(connect > genericHabit);
    }

    @Test
    void intensityFitBeatsIntensityMismatch() {
        // Low-intensity generic-habits strategy fits at intensity 2 but is penalised at intensity 10.
        double atFit = scoreOf(engine.recommend("neutral", null, 2, 12), "gen_habits");
        double atMismatch = scoreOf(engine.recommend("neutral", null, 10, 12), "gen_habits");
        assertTrue(atFit > atMismatch);
    }

    @Test
    void relevanceIsBetweenZeroAndOne() {
        for (Recommendation r : engine.recommend("anxiety", "academics", 8, 12)) {
            assertTrue(r.relevance() >= 0.0 && r.relevance() <= 1.0);
        }
    }

    private static double scoreOf(List<Recommendation> recs, String id) {
        return recs.stream().filter(r -> r.strategy().id().equals(id))
            .mapToDouble(Recommendation::score).findFirst().orElse(Double.NaN);
    }
}
