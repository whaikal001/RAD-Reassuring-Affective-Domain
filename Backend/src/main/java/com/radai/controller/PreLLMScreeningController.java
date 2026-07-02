package com.radai.controller;

import com.radai.dto.ScreeningRequest;
import com.radai.dto.ScreeningResponse;
import com.radai.service.SessionHistoryService;
import com.radai.service.support.CrisisResources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/screening")
public class PreLLMScreeningController {

    private final SessionHistoryService sessionHistoryService;

    private static final java.util.UUID ANONYMOUS_UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    public PreLLMScreeningController(SessionHistoryService sessionHistoryService) {
        this.sessionHistoryService = sessionHistoryService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScreeningResponse screen(@RequestBody ScreeningRequest req,
                                    @RequestHeader(value = "X-User-ID", required = false) String userIdHeader,
                                    @RequestHeader(value = "X-Conversation-ID", required = false) String conversationId,
                                    @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        String language = (acceptLanguage != null && acceptLanguage.toLowerCase().startsWith("ms")) ? "ms" : "en";
        List<Integer> answers = req.getDass21_answers() == null ? new ArrayList<>() : req.getDass21_answers();

        int score = answers.stream().mapToInt(Integer::intValue).sum();

        String band;
        if (score >= 17) {
            band = "Extremely severe";
        } else if (score >= 13) {
            band = "Severe";
        } else if (score >= 10) {
            band = "Moderate";
        } else if (score >= 8) {
            band = "Mild";
        } else {
            band = "Normal";
        }

        String action;
        String message;
        List<String> resources = new ArrayList<>();

        switch (band) {
            case "Extremely severe":
            case "Severe":
                action = "intervention";
                message = "High stress detected. LLM access is blocked; please review and escalate to human operator.";
                resources.addAll(CrisisResources.forLanguage(language));
                break;
            case "Moderate":
                // Previously fell through to "allow"; moderate stress now gets preventive guidance
                // (and still proceeds to chat — only Severe/Extremely severe is blocked).
                action = "prevention";
                message = "Moderate stress detected. Provide preventive guidance and resources.";
                resources.add(CrisisResources.helpBanner(language));
                break;
            case "Mild":
                action = "prevention";
                message = "Mild stress detected. Provide preventive guidance and resources.";
                resources.add(CrisisResources.helpBanner(language));
                break;
            default:
                action = "allow";
                message = "No elevated stress detected for the Stress subscale.";
        }

        // Calculate emotion and intensity based on DASS-21 score and band
        String emotion = detectEmotionFromBand(band, score);
        Integer intensity = calculateIntensityFromScore(score);

        ScreeningResponse resp = new ScreeningResponse();
        resp.setAction(action);
        resp.setScore(score);
        resp.setBand(band);
        resp.setMessage(message);
        resp.setResources(resources);
        resp.setEmotion(emotion);      // NEW: Set detected emotion
        resp.setIntensity(intensity);  // NEW: Set intensity level

        // Persist a screening event into session history so anonymous flows are recorded
        try {
            java.util.UUID userUuid;
            if (userIdHeader == null || userIdHeader.isBlank()) {
                userUuid = ANONYMOUS_UUID;
            } else {
                try {
                    userUuid = java.util.UUID.fromString(userIdHeader);
                } catch (Exception ex) {
                    userUuid = ANONYMOUS_UUID;
                }
            }

            String sessId = (conversationId == null || conversationId.isBlank()) ? java.util.UUID.randomUUID().toString() : conversationId;

            String userMsg = "DASS-21 screening submitted";
            String botMsg = String.format("screening_result: action=%s, band=%s, score=%d", action, band, score);

            if (this.sessionHistoryService != null) {
                this.sessionHistoryService.saveFlowExchange(userUuid, sessId, userMsg, botMsg, band, score);
            }
        } catch (Exception logEx) {
            // Don't fail the screening response if logging fails
            System.err.println("Failed to persist screening event: " + logEx.getMessage());
        }

        return resp;
    }

    /**
     * Detect primary emotion based on DASS-21 screening results
     * DASS-21 measures Stress, Anxiety, and Depression (3 subscales of 7 items each)
     */
    private String detectEmotionFromBand(String band, int score) {
        switch (band) {
            case "Extremely severe":
            case "Severe":
                return "STRESSED";  // High stress → nervous/stressed state
            case "Moderate":
                return "ANXIOUS";   // Moderate stress → anxious state
            case "Mild":
                return "UNDERSTANDING";  // Mild stress → receptive/understanding state
            default:
                return "NEUTRAL";   // Normal → neutral/stable state
        }
    }

    /**
     * Calculate emotional intensity (1-10 scale) based on DASS-21 score
     * DASS-21 raw score ranges: 0-63 (21 items × 3 severity levels)
     * Maps to 1-10 intensity scale for character animation
     */
    private Integer calculateIntensityFromScore(int score) {
        // Normalize score to 1-10 range
        // 0 = 1, 63 = 10, linear mapping
        int intensity = (score * 9 / 63) + 1;
        return Math.max(1, Math.min(10, intensity));  // Clamp to 1-10 range
    }
}

