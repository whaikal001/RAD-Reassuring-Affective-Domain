package com.SocializerAI.enums;

public enum IntensityLevel {
    LOW,
    MODERATE,
    HIGH,
    SEVERE;

    public static IntensityLevel fromScore(int score) {
        if (score <= 0) return LOW;
        if (score <= 4) return LOW;
        if (score <= 7) return MODERATE;
        return HIGH;
    }
}
