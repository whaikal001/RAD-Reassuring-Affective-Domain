package com.radai.service.config;

import com.radai.service.empathy.ApproachSwitchPolicy;
import com.radai.service.risk.RiskAssessmentEngine;
import com.radai.service.trajectory.MoodTrajectoryEngine;

public final class EngineTuning {

    private EngineTuning() {
    }

    // Empathy ↔ sympathy switch.
    public static volatile double approachHighCut = ApproachSwitchPolicy.DEFAULT_HIGH_CUT;
    public static volatile double approachLowCut = ApproachSwitchPolicy.DEFAULT_LOW_CUT;
    public static volatile double approachBandWeight = ApproachSwitchPolicy.DEFAULT_BAND_WEIGHT;
    public static volatile double approachMessageWeight = ApproachSwitchPolicy.DEFAULT_MESSAGE_WEIGHT;

    // Risk level cut-offs.
    public static volatile double riskCriticalCut = RiskAssessmentEngine.DEFAULT_CRITICAL_CUT;
    public static volatile double riskHighCut = RiskAssessmentEngine.DEFAULT_HIGH_CUT;
    public static volatile double riskModerateCut = RiskAssessmentEngine.DEFAULT_MODERATE_CUT;

    // Mood trajectory.
    public static volatile int trajectoryMinSamples = MoodTrajectoryEngine.DEFAULT_MIN_SAMPLES;
    public static volatile double trajectorySlopeThreshold = MoodTrajectoryEngine.DEFAULT_SLOPE_THRESHOLD;
    public static volatile double trajectoryVolatilityThreshold = MoodTrajectoryEngine.DEFAULT_VOLATILITY_THRESHOLD;

    // ML decision thresholds (used only when MlGateway is enabled; rules remain the floor).
    /** Minimum ML emotion score to prefer the model's label over the lexicon. */
    public static volatile double mlEmotionMinScore = 0.50;
    /** Zero-shot probability at/above which the model flags suicidal/self-harm ideation. */
    public static volatile double mlCrisisIdeationThreshold = 0.60;
    /** Zero-shot probability at/above which the model flags a (non-ideation) crisis/distress. */
    public static volatile double mlCrisisThreshold = 0.65;
}
