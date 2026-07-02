package com.radai.service.ml;

import com.radai.service.crisis.CrisisDetectionEngine;
import com.radai.service.crisis.CrisisDetectionEngine.CrisisResult;
import com.radai.service.emotion.EmotionScoringEngine;
import com.radai.service.risk.RiskAssessmentEngine;
import com.radai.service.risk.RiskAssessmentEngine.RiskResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives the engines through the {@link MlGateway} with <b>fake</b> ML functions (no network) to
 * verify the ML-primary behaviour and, crucially, that the keyword/lexicon floor still holds and the
 * system fails safe when ML errors.
 *
 * <p>{@link #reset()} disables the gateway after every test so the static state never leaks into the
 * rules-only unit tests.
 */
class MlEnginesIntegrationTest {

    private final CrisisDetectionEngine crisis = new CrisisDetectionEngine();
    private final EmotionScoringEngine emotion = new EmotionScoringEngine();
    private final RiskAssessmentEngine risk = new RiskAssessmentEngine();

    @AfterEach
    void reset() {
        MlGateway.disable();
    }

    // --- Crisis: ML adds recall on indirect phrasing with no keyword hits ---

    @Test
    void mlFlagsIdeationWhenKeywordsMiss() {
        // "everything feels pointless lately" has no crisis keyword; the ML model catches it.
        MlGateway.configure(true, null,
            (text, labels) -> Map.of(CrisisDetectionEngine.LABEL_IDEATION, 0.88,
                                     CrisisDetectionEngine.LABEL_CRISIS, 0.40), null);

        CrisisResult r = crisis.detect("everything feels pointless lately");
        assertTrue(r.crisis());
        assertTrue(r.suicidalIdeation());
        assertEquals(CrisisDetectionEngine.Severity.CRISIS, r.severity());
        assertTrue(r.reasons().stream().anyMatch(s -> s.startsWith("ml_ideation:")));
    }

    @Test
    void keywordFloorHoldsWhenMlIsSilent() {
        // ML returns nothing useful, but the explicit keyword must still flag crisis.
        MlGateway.configure(true, null, (text, labels) -> Map.of(), null);

        CrisisResult r = crisis.detect("I want to die");
        assertTrue(r.crisis());
        assertTrue(r.suicidalIdeation());
    }

    @Test
    void failsSafeToRulesWhenMlThrows() {
        // A throwing ML function must not break detection; the rule floor still applies.
        MlGateway.configure(true, null,
            (text, labels) -> { throw new RuntimeException("HF down"); }, null);

        CrisisResult explicit = crisis.detect("kill myself");
        assertTrue(explicit.suicidalIdeation());

        CrisisResult benign = crisis.detect("I had a pleasant lunch");
        assertFalse(benign.crisis());
    }

    // --- Emotion: ML-primary, lexicon fallback, crisis floor ---

    @Test
    void mlEmotionLabelWinsWhenConfident() {
        MlGateway.configure(true, text -> new MlGateway.EmotionResult("joy", 0.93), null, null);
        assertEquals("joy", emotion.classifyLabel("what a day"));
    }

    @Test
    void mlNeutralDoesNotEraseLexiconSignal() {
        // ML says neutral, but the lexicon clearly sees stress → keep the specific signal.
        MlGateway.configure(true, text -> new MlGateway.EmotionResult("neutral", 0.99), null, null);
        assertEquals("stress", emotion.classifyLabel("I am so stressed and overwhelmed"));
    }

    @Test
    void hopelessCrisisFloorIsNeverOverriddenByMl() {
        MlGateway.configure(true, text -> new MlGateway.EmotionResult("joy", 0.99), null, null);
        assertEquals("hopeless", emotion.classifyLabel("I want to die"));
    }

    // --- Risk: ML can raise the level; rules remain the floor ---

    @Test
    void mlRaisesRiskToCriticalOnHighIdeation() {
        MlGateway.configure(true, null,
            (text, labels) -> Map.of(CrisisDetectionEngine.LABEL_IDEATION, 0.92,
                                     CrisisDetectionEngine.LABEL_CRISIS, 0.30), null);

        RiskResult r = risk.assess("I don't see the point anymore", 5, false);
        assertEquals("critical", r.level());
        assertTrue(r.crisis());
        assertTrue(r.reasons().stream().anyMatch(s -> s.startsWith("ml_risk:")));
    }

    @Test
    void lowMlSignalDoesNotInflateRisk() {
        // Below the crisis thresholds → ML contributes nothing; a neutral message stays low.
        MlGateway.configure(true, null,
            (text, labels) -> Map.of(CrisisDetectionEngine.LABEL_IDEATION, 0.10,
                                     CrisisDetectionEngine.LABEL_CRISIS, 0.20), null);

        RiskResult r = risk.assess("just checking in, feeling okay", 3, false);
        assertEquals("low", r.level());
    }
}
