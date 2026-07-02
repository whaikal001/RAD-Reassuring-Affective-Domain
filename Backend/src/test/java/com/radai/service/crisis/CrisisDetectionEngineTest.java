package com.radai.service.crisis;

import com.radai.service.crisis.CrisisDetectionEngine.CrisisResult;
import com.radai.service.crisis.CrisisDetectionEngine.Severity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the recall-focused CrisisDetectionEngine: explicit + passive ideation, self-harm, safety
 * concerns, distress, Malay coverage, and that indirect ideation (the old regex's blind spot) is caught.
 */
class CrisisDetectionEngineTest {

    private final CrisisDetectionEngine engine = new CrisisDetectionEngine();

    @Test
    void neutralTextIsNotCrisis() {
        CrisisResult r = engine.detect("I'm a bit tired but had a good day overall.");
        assertFalse(r.crisis());
        assertFalse(r.suicidalIdeation());
        assertEquals(Severity.NONE, r.severity());
    }

    @Test
    void explicitSuicideIsCrisisWithIdeation() {
        CrisisResult r = engine.detect("I want to die");
        assertTrue(r.crisis());
        assertTrue(r.suicidalIdeation());
        assertEquals(Severity.CRISIS, r.severity());
        assertTrue(r.confidence() >= 0.9);
    }

    @Test
    void passiveIdeationIsCaught() {
        // The key improvement: no explicit "suicide" word, but clear passive ideation.
        for (String msg : new String[]{
                "there's no reason to live anymore",
                "everyone would be better off without me",
                "I just can't go on like this"}) {
            CrisisResult r = engine.detect(msg);
            assertTrue(r.crisis(), "should flag crisis: " + msg);
            assertTrue(r.suicidalIdeation(), "should flag ideation: " + msg);
            assertEquals(Severity.CRISIS, r.severity());
        }
    }

    @Test
    void selfHarmIsCrisis() {
        CrisisResult r = engine.detect("sometimes I want to hurt myself");
        assertTrue(r.suicidalIdeation());
        assertEquals(Severity.CRISIS, r.severity());
    }

    @Test
    void malayIdeationIsCaught() {
        assertTrue(engine.detect("saya nak mati").suicidalIdeation());
        assertTrue(engine.detect("tak ada sebab untuk hidup").crisis());
    }

    @Test
    void distressWithoutIdeationIsElevatedNotIdeation() {
        CrisisResult r = engine.detect("this is an emergency, help me");
        assertTrue(r.crisis());
        assertFalse(r.suicidalIdeation());
        assertEquals(Severity.ELEVATED, r.severity());
    }

    @Test
    void reasonsAreReported() {
        CrisisResult r = engine.detect("I want to die and I feel unsafe");
        assertTrue(r.reasons().stream().anyMatch(s -> s.startsWith("explicit_suicide:")));
        assertTrue(r.reasons().stream().anyMatch(s -> s.startsWith("safety_concern:")));
    }
}
