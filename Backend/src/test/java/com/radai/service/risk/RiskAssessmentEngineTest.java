package com.radai.service.risk;

import com.radai.service.risk.RiskAssessmentEngine.RiskResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the RiskAssessmentEngine: additive weighted scoring, the crisis-language floor, the
 * four discrete levels, and the margin-based confidence.
 */
class RiskAssessmentEngineTest {

    private static final double EPS = 1e-9;
    private final RiskAssessmentEngine engine = new RiskAssessmentEngine();

    @Test
    void neutralMessageIsLowRisk() {
        RiskResult r = engine.assess("I had a normal day, feeling okay.", 3, false);
        assertEquals("low", r.level());
        assertFalse(r.crisis());
        assertEquals(0.0, r.score(), EPS);
    }

    @Test
    void crisisLanguageIsCriticalAndFlagsCrisis() {
        RiskResult r = engine.assess("I want to die and I want to kill myself", 5, false);
        assertEquals("critical", r.level());
        assertTrue(r.crisis());
        assertTrue(r.score() >= RiskAssessmentEngine.DEFAULT_CRISIS_LANGUAGE_FLOOR - EPS);
        assertTrue(r.reasons().contains("matched_crisis_language"));
    }

    @Test
    void malayCrisisLanguageIsAlsoCritical() {
        RiskResult r = engine.assess("saya nak mati", 5, false);
        assertEquals("critical", r.level());
        assertTrue(r.crisis());
    }

    @Test
    void singleHighRiskTriggerPlusHighIntensityIsModerate() {
        // 0.2 (hopeless) + 0.2 (intensity>=8) = 0.4 -> moderate band [0.2, 0.45)
        RiskResult r = engine.assess("I feel hopeless", 9, false);
        assertEquals(0.4, r.score(), EPS);
        assertEquals("moderate", r.level());
        assertTrue(r.reasons().contains("high_risk_trigger:hopeless"));
        assertTrue(r.reasons().contains("high_intensity"));
    }

    @Test
    void contextCrisisRaisesScore() {
        // 0.2 (panic attack) + 0.4 (context) = 0.6 -> high band [0.45, 0.8)
        RiskResult r = engine.assess("I'm having a panic attack", 5, true);
        assertEquals(0.6, r.score(), EPS);
        assertEquals("high", r.level());
    }

    @Test
    void scoreIsCappedAtOne() {
        RiskResult r = engine.assess("suicide kill myself want to die hopeless worthless emergency", 10, true);
        assertTrue(r.score() <= 1.0 + EPS);
        assertEquals("critical", r.level());
    }

    @Test
    void confidenceIsHalfOnBandBoundaryAndHigherInBandCentre() {
        // Score exactly on the moderate cut (0.2) -> boundary -> confidence 0.5.
        RiskResult boundary = engine.assess("hopeless", 5, false); // 0.2 exactly
        assertEquals(0.2, boundary.score(), EPS);
        assertEquals(0.5, boundary.confidence(), 1e-6);

        // Score deeper inside a band should be more confident than a boundary score.
        RiskResult deeper = engine.assess("I feel hopeless", 9, false); // 0.4, centre of [0.2,0.45)
        assertTrue(deeper.confidence() > boundary.confidence());
    }

    @Test
    void rejectsInvalidCutOffOrdering() {
        try {
            new RiskAssessmentEngine(
                RiskAssessmentEngine.DEFAULT_CRISIS_TRIGGERS,
                RiskAssessmentEngine.DEFAULT_HIGH_RISK_TRIGGERS,
                RiskAssessmentEngine.DEFAULT_CRISIS_LANGUAGE_REGEX,
                0.7, 0.2, 0.2, 0.4, 0.9, 8,
                0.3, 0.5, 0.2); // critical(0.3) < high(0.5): invalid
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }
    }
}
