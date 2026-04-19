package com.SocializerAI.chat.tts;

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
    private String ttsModel;

    private final RestTemplate restTemplate;
    
    public MultiLanguageTTSService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
            log.info("Generating TTS for text: '{}' in language: {}", text, normalizedLang);

            // Use HuggingFace TTS API
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + hfApiToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("inputs", text);
            body.put("language", normalizedLang.toLowerCase());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            
            String hfUrl = String.format("https://api-inference.huggingface.co/models/%s", ttsModel);
            
            ResponseEntity<byte[]> response = restTemplate.postForEntity(hfUrl, request, byte[].class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("TTS generated successfully for language: {}", normalizedLang);
                return response.getBody();
            } else {
                log.error("TTS generation failed with status: {}", response.getStatusCode());
                return new byte[0];
            }
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
