package com.radai.service.config;

import com.radai.service.empathy.ApproachSwitchPolicy;
import com.radai.service.risk.RiskAssessmentEngine;
import com.radai.service.trajectory.MoodTrajectoryEngine;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "radai.engine")
public class EngineProperties {

    private final Approach approach = new Approach();
    private final Risk risk = new Risk();
    private final Trajectory trajectory = new Trajectory();
    private final Ml ml = new Ml();

    public Approach getApproach() {
        return approach;
    }

    public Risk getRisk() {
        return risk;
    }

    public Trajectory getTrajectory() {
        return trajectory;
    }

    public Ml getMl() {
        return ml;
    }

    /** Copy the bound values into the process-wide registry the engines read. */
    @PostConstruct
    public void apply() {
        EngineTuning.approachHighCut = approach.highCut;
        EngineTuning.approachLowCut = approach.lowCut;
        EngineTuning.approachBandWeight = approach.bandWeight;
        EngineTuning.approachMessageWeight = approach.messageWeight;

        EngineTuning.riskCriticalCut = risk.criticalCut;
        EngineTuning.riskHighCut = risk.highCut;
        EngineTuning.riskModerateCut = risk.moderateCut;

        EngineTuning.trajectoryMinSamples = trajectory.minSamples;
        EngineTuning.trajectorySlopeThreshold = trajectory.slopeThreshold;
        EngineTuning.trajectoryVolatilityThreshold = trajectory.volatilityThreshold;

        EngineTuning.mlEmotionMinScore = ml.emotionMinScore;
        EngineTuning.mlCrisisIdeationThreshold = ml.crisisIdeationThreshold;
        EngineTuning.mlCrisisThreshold = ml.crisisThreshold;
    }

    public static class Approach {
        private double highCut = ApproachSwitchPolicy.DEFAULT_HIGH_CUT;
        private double lowCut = ApproachSwitchPolicy.DEFAULT_LOW_CUT;
        private double bandWeight = ApproachSwitchPolicy.DEFAULT_BAND_WEIGHT;
        private double messageWeight = ApproachSwitchPolicy.DEFAULT_MESSAGE_WEIGHT;

        public double getHighCut() { return highCut; }
        public void setHighCut(double highCut) { this.highCut = highCut; }
        public double getLowCut() { return lowCut; }
        public void setLowCut(double lowCut) { this.lowCut = lowCut; }
        public double getBandWeight() { return bandWeight; }
        public void setBandWeight(double bandWeight) { this.bandWeight = bandWeight; }
        public double getMessageWeight() { return messageWeight; }
        public void setMessageWeight(double messageWeight) { this.messageWeight = messageWeight; }
    }

    public static class Risk {
        private double criticalCut = RiskAssessmentEngine.DEFAULT_CRITICAL_CUT;
        private double highCut = RiskAssessmentEngine.DEFAULT_HIGH_CUT;
        private double moderateCut = RiskAssessmentEngine.DEFAULT_MODERATE_CUT;

        public double getCriticalCut() { return criticalCut; }
        public void setCriticalCut(double criticalCut) { this.criticalCut = criticalCut; }
        public double getHighCut() { return highCut; }
        public void setHighCut(double highCut) { this.highCut = highCut; }
        public double getModerateCut() { return moderateCut; }
        public void setModerateCut(double moderateCut) { this.moderateCut = moderateCut; }
    }

    public static class Trajectory {
        private int minSamples = MoodTrajectoryEngine.DEFAULT_MIN_SAMPLES;
        private double slopeThreshold = MoodTrajectoryEngine.DEFAULT_SLOPE_THRESHOLD;
        private double volatilityThreshold = MoodTrajectoryEngine.DEFAULT_VOLATILITY_THRESHOLD;

        public int getMinSamples() { return minSamples; }
        public void setMinSamples(int minSamples) { this.minSamples = minSamples; }
        public double getSlopeThreshold() { return slopeThreshold; }
        public void setSlopeThreshold(double slopeThreshold) { this.slopeThreshold = slopeThreshold; }
        public double getVolatilityThreshold() { return volatilityThreshold; }
        public void setVolatilityThreshold(double volatilityThreshold) { this.volatilityThreshold = volatilityThreshold; }
    }

    public static class Ml {
        private double emotionMinScore = 0.50;
        private double crisisIdeationThreshold = 0.60;
        private double crisisThreshold = 0.65;

        public double getEmotionMinScore() { return emotionMinScore; }
        public void setEmotionMinScore(double emotionMinScore) { this.emotionMinScore = emotionMinScore; }
        public double getCrisisIdeationThreshold() { return crisisIdeationThreshold; }
        public void setCrisisIdeationThreshold(double crisisIdeationThreshold) { this.crisisIdeationThreshold = crisisIdeationThreshold; }
        public double getCrisisThreshold() { return crisisThreshold; }
        public void setCrisisThreshold(double crisisThreshold) { this.crisisThreshold = crisisThreshold; }
    }
}
