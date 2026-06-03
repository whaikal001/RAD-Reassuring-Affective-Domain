package com.radai.service.empathy;

import com.radai.enums.ApproachType;
import com.radai.service.empathy.ApproachSwitchPolicy.Decision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the empathy&lt;-&gt;sympathy switch mechanic:
 * direction (HIGH→SYMPATHY, LOW→EMPATHY), "both combined" signal (DASS band + per-message
 * intensity), and hysteresis (the four outcomes: switch E→S, switch S→E, stay E, stay S),
 * plus the first-turn and crisis overrides.
 */
class ApproachSwitchPolicyTest {

    private static final double EPS = 1e-9;
    private final ApproachSwitchPolicy policy = new ApproachSwitchPolicy();

    // ----- band baseline mapping -----

    @Test
    void bandBaselineMapsEveryBand() {
        assertEquals(1.0, ApproachSwitchPolicy.bandBaseline("Normal"), EPS);
        assertEquals(3.0, ApproachSwitchPolicy.bandBaseline("Mild"), EPS);
        assertEquals(5.0, ApproachSwitchPolicy.bandBaseline("Moderate"), EPS);
        assertEquals(7.0, ApproachSwitchPolicy.bandBaseline("Severe"), EPS);
        assertEquals(9.0, ApproachSwitchPolicy.bandBaseline("Extremely severe"), EPS);
    }

    @Test
    void bandBaselineIsCaseAndWhitespaceInsensitive() {
        assertEquals(7.0, ApproachSwitchPolicy.bandBaseline("  SEVERE  "), EPS);
        assertEquals(9.0, ApproachSwitchPolicy.bandBaseline("extremely severe"), EPS);
    }

    @Test
    void bandBaselineReturnsNegativeForUnknownOrNull() {
        assertEquals(-1.0, ApproachSwitchPolicy.bandBaseline(null), EPS);
        assertEquals(-1.0, ApproachSwitchPolicy.bandBaseline("gibberish"), EPS);
    }

    // ----- combined signal ("both combined") -----

    @Test
    void combinedSignalBlendsBandAndIntensityEqually() {
        assertEquals(7.5, policy.combinedSignal("Severe", 8), EPS);   // 0.5*7 + 0.5*8
        assertEquals(1.5, policy.combinedSignal("Normal", 2), EPS);   // 0.5*1 + 0.5*2
        assertEquals(5.5, policy.combinedSignal("Moderate", 6), EPS); // 0.5*5 + 0.5*6
    }

    @Test
    void combinedSignalFallsBackToIntensityWhenBandUnknown() {
        assertEquals(9.0, policy.combinedSignal(null, 9), EPS);
        assertEquals(3.0, policy.combinedSignal(null, 3), EPS);
    }

    @Test
    void combinedSignalClampsIntensityTo0To10() {
        assertEquals(10.0, policy.combinedSignal(null, 15), EPS);
        assertEquals(0.0, policy.combinedSignal(null, -3), EPS);
    }

    // ----- the four hysteresis outcomes -----

    @Test
    void switchesEmpathyToSympathyWhenSignalAtOrAboveHighCut() {
        Decision d = policy.decide(ApproachType.EMPATHY, false, false, "Severe", 8); // 7.5 >= 7
        assertEquals(ApproachType.SYMPATHY, d.approach());
        assertTrue(d.switched());
        assertEquals(7.5, d.signal(), EPS);
    }

    @Test
    void switchesSympathyToEmpathyWhenSignalAtOrBelowLowCut() {
        Decision d = policy.decide(ApproachType.SYMPATHY, false, false, "Normal", 2); // 1.5 <= 4
        assertEquals(ApproachType.EMPATHY, d.approach());
        assertTrue(d.switched());
        assertEquals(1.5, d.signal(), EPS);
    }

    @Test
    void staysEmpathyInDeadZone() {
        Decision d = policy.decide(ApproachType.EMPATHY, false, false, "Moderate", 6); // 5.5 in (4,7)
        assertEquals(ApproachType.EMPATHY, d.approach());
        assertFalse(d.switched());
    }

    @Test
    void staysSympathyInDeadZone() {
        Decision d = policy.decide(ApproachType.SYMPATHY, false, false, "Moderate", 6); // 5.5 in (4,7)
        assertEquals(ApproachType.SYMPATHY, d.approach());
        assertFalse(d.switched());
    }

    // ----- cutoffs are inclusive -----

    @Test
    void highCutIsInclusive() {
        Decision d = policy.decide(ApproachType.EMPATHY, false, false, "Severe", 7); // 7.0 == high cut
        assertEquals(ApproachType.SYMPATHY, d.approach());
    }

    @Test
    void lowCutIsInclusive() {
        Decision d = policy.decide(ApproachType.SYMPATHY, false, false, "Normal", 7); // 0.5*1+0.5*7 = 4.0 == low cut
        assertEquals(ApproachType.EMPATHY, d.approach());
    }

    // ----- band-less (intensity only) still drives the switch -----

    @Test
    void intensityOnlyDrivesSwitchWhenNoBand() {
        assertEquals(ApproachType.SYMPATHY, policy.decide(ApproachType.EMPATHY, false, false, null, 9).approach());
        assertEquals(ApproachType.EMPATHY, policy.decide(ApproachType.SYMPATHY, false, false, null, 3).approach());
    }

    // ----- overrides -----

    @Test
    void firstTurnAlwaysStartsInEmpathyEvenWithHighSignal() {
        Decision d = policy.decide(ApproachType.EMPATHY, true, false, "Extremely severe", 10); // signal 9.5
        assertEquals(ApproachType.EMPATHY, d.approach());
        assertFalse(d.switched());
    }

    @Test
    void crisisForcesSympathyAndBeatsFirstTurn() {
        Decision d = policy.decide(ApproachType.EMPATHY, true, true, "Normal", 0); // low signal, first turn
        assertEquals(ApproachType.SYMPATHY, d.approach());
        assertTrue(d.switched());
    }

    // ----- no flip-flop across a small fluctuation through the dead-zone -----

    @Test
    void doesNotFlapWhenSignalDriftsThroughDeadZone() {
        // Start SYMPATHY at high signal, drift down into the dead-zone: must stay SYMPATHY.
        ApproachType mode = policy.decide(ApproachType.EMPATHY, false, false, "Severe", 9).approach(); // -> SYMPATHY
        assertEquals(ApproachType.SYMPATHY, mode);
        mode = policy.decide(mode, false, false, "Moderate", 6).approach(); // 5.5 dead-zone -> stay SYMPATHY
        assertEquals(ApproachType.SYMPATHY, mode);
        mode = policy.decide(mode, false, false, "Mild", 6).approach(); // 0.5*3+0.5*6 = 4.5 dead-zone -> stay SYMPATHY
        assertEquals(ApproachType.SYMPATHY, mode);
        mode = policy.decide(mode, false, false, "Normal", 2).approach(); // 1.5 <= 4 -> finally EMPATHY
        assertEquals(ApproachType.EMPATHY, mode);
    }
}
