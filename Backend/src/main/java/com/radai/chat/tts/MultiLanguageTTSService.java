package com.radai.chat.tts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class MultiLanguageTTSService {
    private static final Logger log = LoggerFactory.getLogger(MultiLanguageTTSService.class);

    @Value("${hf.api.token}")
    private String hfApiToken;

    @Value("${hf.tts.model}")
    private String ttsModel; // backward-compatible default (falls back to EN)

    @Value("${hf.tts.model.en}")
    private String ttsModelEn;

    @Value("${hf.tts.model.ms}")
    private String ttsModelMs;

    private final RestTemplate restTemplate;
    
    public MultiLanguageTTSService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Remove emoji characters and colon-style emoji names from text.
     * Examples removed: "😊", "🏃‍♂️", ":smile:", ":thumbsup:"
     */
    private String removeEmojisAndNames(String input) {
        if (input == null) return null;

        String s = input;

        // Remove colon-wrapped emoji names like :smile:
            s = s.replaceAll(":" + "[a-zA-Z0-9_+\\-]+" + ":", "");

        // Remove common Unicode emoji ranges (surrogate pairs)
        // Remove emojis represented by surrogate pairs (e.g. U+1F600..U+1F64F)
        s = s.replaceAll("[\\uD83C-\\uDBFF][\\uDC00-\\uDFFF]", "");

        // Remove miscellaneous symbols and dingbats
        s = s.replaceAll("[\\u2600-\\u26FF]", "");
        s = s.replaceAll("[\\u2700-\\u27BF]", "");

        // Remove variation selector and zero-width joiner leftovers
        s = s.replaceAll("[\\uFE0F\\u200D]", "");

        // Trim extra whitespace created by removals
        return s.replaceAll("\\s+", " ").trim();
    }

    // Language code mappings for language names
    private static final Map<String, String> LANGUAGE_CODES = Map.ofEntries(
        Map.entry("en", "English"),
        Map.entry("english", "English"),
        Map.entry("ms", "Malay"),
        Map.entry("malay", "Malay"),
        Map.entry("id", "Indonesian"),
        Map.entry("indonesian", "Indonesian"),
        Map.entry("zh", "Chinese"),
        Map.entry("ja", "Japanese"),
        Map.entry("spanish", "Spanish"),
        Map.entry("fr", "French")
    );

    /**
     * Convert text to speech with language support
     * Supports: English, Malay, Indonesian, Chinese, Japanese, Spanish, French, etc.
     * 
     * @param text Text to convert
     * @param language Language code (en, ms, id, etc.) or name (english, malay, etc.)
     * @return Audio bytes
     */
    public byte[] textToSpeech(String text, String language) {
        try {
            String normalizedLang = normalizeLanguage(language);
            // Remove emoji characters and colon-style emoji names (e.g. :smile:)
            String cleaned = removeEmojisAndNames(text);
            log.info("Generating TTS for text (cleaned): '{}' in language: {}", cleaned, normalizedLang);

            // Use HuggingFace TTS API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + hfApiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("inputs", cleaned);
            body.put("language", normalizedLang.toLowerCase());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            // Choose model per language: Malay -> ttsModelMs, English -> ttsModelEn, otherwise fallback to configured ttsModel
            String modelToUse = ttsModel; // default
            if (normalizedLang != null) {
                if ("Malay".equalsIgnoreCase(normalizedLang)) {
                    modelToUse = (ttsModelMs != null && !ttsModelMs.isBlank()) ? ttsModelMs : ttsModel;
                } else if ("English".equalsIgnoreCase(normalizedLang)) {
                    modelToUse = (ttsModelEn != null && !ttsModelEn.isBlank()) ? ttsModelEn : ttsModel;
                } else {
                    // use EN model as a safe default when language unknown
                    modelToUse = (ttsModelEn != null && !ttsModelEn.isBlank()) ? ttsModelEn : ttsModel;
                }
            }

            String hfUrl = String.format("https://router.huggingface.co/hf-inference/models/%s", modelToUse);

            try {
                ResponseEntity<byte[]> response = restTemplate.postForEntity(hfUrl, request, byte[].class);
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    log.info("TTS generated successfully for language: {} using model: {}", normalizedLang, modelToUse);
                    return response.getBody();
                } else {
                    log.error("TTS generation failed with status: {} using model: {}", response.getStatusCode(), modelToUse);
                    // fall through to try fallback below
                }
            } catch (org.springframework.web.client.HttpClientErrorException hce) {
                // Common case: 400 Bad Request when model isn't hosted by provider
                int statusCode = hce.getStatusCode().value();
                log.warn("HF request failed for model {} with status {}: {}", modelToUse, statusCode, hce.getResponseBodyAsString());
                if (statusCode == HttpStatus.BAD_REQUEST.value()) {
                    // try fallback to English model if not already using it
                    String fallback = (ttsModelEn != null && !ttsModelEn.isBlank()) ? ttsModelEn : ttsModel;
                    if (!fallback.equals(modelToUse)) {
                        log.info("Retrying TTS using fallback model: {}", fallback);
                        String fallbackUrl = String.format("https://router.huggingface.co/hf-inference/models/%s", fallback);
                        try {
                            ResponseEntity<byte[]> fallbackResp = restTemplate.postForEntity(fallbackUrl, request, byte[].class);
                            if (fallbackResp.getStatusCode() == HttpStatus.OK && fallbackResp.getBody() != null) {
                                log.info("TTS generated successfully using fallback model: {}", fallback);
                                return fallbackResp.getBody();
                            } else {
                                log.error("Fallback TTS generation failed with status: {}", fallbackResp.getStatusCode());
                            }
                        } catch (Exception ex) {
                            log.error("Fallback model request failed", ex);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error during HF TTS request", e);
            }

            return new byte[0];
        } catch (Exception e) {
            log.error("Error generating TTS for language: {}", language, e);
            return new byte[0];
        }
    }

    /**
     * Normalize language input to standard language name
     */
    private String normalizeLanguage(String input) {
        if (input == null || input.isEmpty()) {
            return "English"; // Default to English
        }

        String lower = input.toLowerCase().trim();
        
        // Check direct mapping
        if (LANGUAGE_CODES.containsKey(lower)) {
            return LANGUAGE_CODES.get(lower);
        }

        // Check if it's a language name
        for (String lang : LANGUAGE_CODES.values()) {
            if (lang.equalsIgnoreCase(input)) {
                return lang;
            }
        }

        // Default to English if not found
        log.warn("Unknown language '{}', defaulting to English", input);
        return "English";
    }

    /**
     * Get supported languages
     */
    public List<String> getSupportedLanguages() {
        return new ArrayList<>(LANGUAGE_CODES.values());
    }
}

