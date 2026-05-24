package com.radai.dto;

import com.radai.enums.PathwayType;
import com.radai.enums.ApproachType;

/**
 * DTO for flow engine responses to be returned to the client
 */
public record ChatbotFlowRequest(
    String userMessage,
    int intensityScore,
    String language
) {
    /**
     * Request to process through the chatbot flow system
     * - userMessage: What the user said
     * - intensityScore: 1-10 scale of emotional intensity (from user self-assessment)
     * - language: "en" or "ms" (English or Malay)
     */
}


