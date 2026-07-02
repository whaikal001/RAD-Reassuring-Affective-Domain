package com.radai.service.empathy;

import com.radai.enums.ApproachType;
public class ApproachSwitchPolicy {

    /** At or above this combined signal → SYMPATHY. */
    public static final double DEFAULT_HIGH_CUT = 7.0;
    /** At or below this combined signal → EMPATHY. */
    public static final double DEFAULT_LOW_CUT = 4.0;
    /** Weight of the DASS-band baseline in the blend. */
    public static final double DEFAULT_BAND_WEIGHT = 0.5;
    /** Weight of the per-message intensity in the blend. */
    public static final double DEFAULT_MESSAGE_WEIGHT = 0.5;

    private final double highCut;
    private final double lowCut;
    private final double bandWeight;
    private final double messageWeight;

    public ApproachSwitchPolicy() {
        // Sourced from EngineTuning so the cut-offs/weights can be tuned via application.properties;
        // EngineTuning defaults to the constants above, so behaviour is unchanged unless configured.
        this(com.radai.service.config.EngineTuning.approachHighCut,
             com.radai.service.config.EngineTuning.approachLowCut,
             com.radai.service.config.EngineTuning.approachBandWeight,
             com.radai.service.config.EngineTuning.approachMessageWeight);
    }

    public ApproachSwitchPolicy(double highCut, double lowCut, double bandWeight, double messageWeight) {
        if (lowCut > highCut) {
            throw new IllegalArgumentException("lowCut (" + lowCut + ") must be <= highCut (" + highCut + ")");
        }
        this.highCut = highCut;
        this.lowCut = lowCut;
        this.bandWeight = bandWeight;
        this.messageWeight = messageWeight;
    }

    public static double bandBaseline(String dassBand) {
        if (dassBand == null) {
            return -1.0;
        }
        return switch (dassBand.trim().toLowerCase()) {
            case "normal" -> 1.0;
            case "mild" -> 3.0;
            case "moderate" -> 5.0;
            case "severe" -> 7.0;
            case "extremely severe" -> 9.0;
            default -> -1.0;
        };
    }

    /**
     * The combined 0-10 stress/intensity signal. If the band is known it is blended with the
     * per-message intensity; otherwise the per-message intensity is used on its own.
     */
    public double combinedSignal(String dassBand, int messageIntensity) {
        int msg = clampIntensity(messageIntensity);
        double baseline = bandBaseline(dassBand);
        if (baseline < 0) {
            return msg; // no usable band → intensity only
        }
        return bandWeight * baseline + messageWeight * msg;
    }

    public Decision decide(ApproachType current, boolean firstTurn, boolean crisis,
                           String dassBand, int messageIntensity) {
        ApproachType base = (current == ApproachType.SYMPATHY) ? ApproachType.SYMPATHY : ApproachType.EMPATHY;
        double signal = combinedSignal(dassBand, messageIntensity);

        // Safety override always wins.
        if (crisis) {
            return new Decision(ApproachType.SYMPATHY, signal,
                    "crisis detected → highest support (sympathy)", base != ApproachType.SYMPATHY);
        }

        // Sessions open in empathy regardless of the opening signal.
        if (firstTurn) {
            return new Decision(ApproachType.EMPATHY, signal,
                    "first turn → start in empathy", base != ApproachType.EMPATHY);
        }

        if (signal >= highCut) {
            return new Decision(ApproachType.SYMPATHY, signal,
                    String.format("signal %.2f >= high cut %.2f → sympathy", signal, highCut),
                    base != ApproachType.SYMPATHY);
        }

        if (signal <= lowCut) {
            return new Decision(ApproachType.EMPATHY, signal,
                    String.format("signal %.2f <= low cut %.2f → empathy", signal, lowCut),
                    base != ApproachType.EMPATHY);
        }

        // Dead-zone between the cutoffs → hold the current mode (no flapping).
        return new Decision(base, signal,
                String.format("signal %.2f in dead-zone (%.2f, %.2f) → keep %s", signal, lowCut, highCut, base),
                false);
    }

    private static int clampIntensity(int v) {
        return Math.max(0, Math.min(10, v));
    }

    public record Decision(ApproachType approach, double signal, String reason, boolean switched) {}
}
