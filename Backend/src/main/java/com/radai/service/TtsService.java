package com.radai.service;

import com.radai.dto.TtsRequest;
import com.radai.dto.TtsResponse;
import org.springframework.stereotype.Service;

@Service
public class TtsService {

    public TtsResponse synthesize(TtsRequest request) {
        String text = request.getText() != null ? request.getText().trim() : "";
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Text is required for TTS synthesis");
        }

        String language = normalizeLanguage(request.getLanguage());
        String voiceHint = mapVoiceHint(language);
        String rate = mapRate(request.getRate());
        String pitch = mapPitch(request.getPitch());

        String escapedText = escapeXml(text);
        String ssml = "<speak><prosody rate=\"" + rate + "\" pitch=\"" + pitch + "\">" + escapedText + "</prosody></speak>";

        return new TtsResponse(ssml, voiceHint, language);
    }

    private String mapRate(String rate) {
        if (rate == null) {
            return "normal";
        }
        return switch (rate.toUpperCase()) {
            case "SLOW" -> "slow";
            case "FAST" -> "fast";
            default -> "normal";
        };
    }

    private String mapPitch(String pitch) {
        if (pitch == null) {
            return "0%";
        }
        return switch (pitch.toUpperCase()) {
            case "LOW" -> "-2%";
            case "HIGH" -> "+2%";
            default -> "0%";
        };
    }

    private String normalizeLanguage(String language) {
        if (language == null) {
            return "en";
        }
        String normalized = language.trim().toLowerCase();
        if (normalized.startsWith("ms")) {
            return "ms";
        }
        return "en";
    }

    private String mapVoiceHint(String language) {
        return language.equals("ms") ? "ms-MY" : "en-US";
    }

    private String escapeXml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}

