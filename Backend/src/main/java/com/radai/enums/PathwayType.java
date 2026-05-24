package com.radai.enums;

public enum PathwayType {
    PREVENTION,
    INTERVENTION,
    STRESS_MANAGEMENT,
    ANXIETY_RELIEF,
    MOOD_BOOST,
    GENERAL_WELLBEING,
    CRISIS_INTERVENTION;

    public String getName() {
        String name = name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}

