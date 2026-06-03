package com.radai.dto;

import com.radai.enums.PathwayType;
import com.radai.enums.ApproachType;

/**
 * DTO for flow engine responses to be returned to the client
 */
public record ChatbotFlowRequest(
    String userMessage,
    int intensityScore,
    String language,
    String dassBand
) {
    /**
     * Request to process through the chatbot flow system
     * - userMessage: What the user said
     * - intensityScore: 1-10 scale of emotional intensity (from user self-assessment)
     * - language: "en" or "ms" (English or Malay)
     * - dassBand: DASS screening band (Normal/Mild/Moderate/Severe/Extremely severe), optional.
     *   Used as the stable baseline in the empathy<->sympathy switch. May be null when the client
     *   has not run screening; the switch then relies on per-message intensity alone.
     */
}


