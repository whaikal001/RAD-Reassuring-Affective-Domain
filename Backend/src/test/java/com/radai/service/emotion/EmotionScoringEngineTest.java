package com.radai.service.emotion;

import com.radai.service.emotion.EmotionScoringEngine.EmotionAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the weighted-lexicon EmotionScoringEngine: winner selection, ranking, the neutral
 * fallback, safety-word dominance, and the confidence (share-of-evidence) measure.
 */
class EmotionScoringEngineTest {

    private final EmotionScoringEngine engine = new EmotionScoringEngine();

    @Test
    void noKeywordMatchIsNeutralWithLowConfidence() {
        EmotionAssessment a = engine.classify("The weather is fine and the report is due.");
        assertEquals(EmotionScoringEngine.NEUTRAL, a.emotion());
        assertEquals(EmotionScoringEngine.NEUTRAL_CONFIDENCE, a.confidence(), 1e-9);
        assertTrue(a.ranked().isEmpty());
    }

    @Test
    void picksTheDominantEmotion() {
        assertEquals("anxiety", engine.classifyLabel("I feel so anxious and my worry keeps growing"));
        assertEquals("stress", engine.classifyLabel("I'm so stressed and overwhelmed, it's too much"));
    }

    @Test
    void safetyWordsDominateOverOrdinaryEmotions() {
        // "sad" would normally score for sadness, but crisis language must win.
        EmotionAssessment a = engine.classify("I feel sad and I want to die");
        assertEquals("hopeless", a.emotion());
    }

    @Test
    void confidenceIsWinnersShareOfEvidence() {
        // Only anxiety words present → the winner holds all the evidence → confidence 1.0.
        EmotionAssessment pure = engine.classify("anxious and panicking");
        assertEquals("anxiety", pure.emotion());
        assertEquals(1.0, pure.confidence(), 1e-9);

        // Mixed signals → confidence strictly below 1.0.
        EmotionAssessment mixed = engine.classify("I am angry but also a bit sad");
        assertTrue(mixed.confidence() < 1.0);
        assertTrue(mixed.confidence() > 0.0);
    }

    @Test
    void rankingIsOrderedByScoreDescending() {
        EmotionAssessment a = engine.classify("furious and angry, a little sad");
        assertEquals("anger", a.ranked().get(0).emotion());
        for (int i = 1; i < a.ranked().size(); i++) {
            assertTrue(a.ranked().get(i - 1).score() >= a.ranked().get(i).score());
        }
    }

    @Test
    void handlesNullText() {
        assertEquals(EmotionScoringEngine.NEUTRAL, engine.classifyLabel(null));
    }
}
