package com.SocializerAI.enums;

public enum ApproachType {
    EMPATHY,
    SYMPATHY,
    MINDFULNESS,
    COGNITIVE_REFRAMING,
    BREATHING_EXERCISES,
    GRATITUDE_PRACTICE,
    POSITIVE_AFFIRMATION,
    ACTIVITY_SUGGESTION,
    PROFESSIONAL_RESOURCES;

    public ApproachType opposite() {
        if (this == EMPATHY) return SYMPATHY;
        if (this == SYMPATHY) return EMPATHY;
        return EMPATHY;
    }
}
