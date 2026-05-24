package com.radai.chat.tts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tts")
@CrossOrigin(origins = "*", maxAge = 3600)
public class TTSController {
    private static final Logger log = LoggerFactory.getLogger(TTSController.class);

    private final MultiLanguageTTSService ttsService;

    public TTSController(MultiLanguageTTSService ttsService) {
        this.ttsService = ttsService;
    }

    /**
     * Convert text to speech in specified language
     * 
     * @param text Text to convert (query param or JSON body)
     * @param language Language code or name (en, ms, english, malay, etc.)
     * @return Audio MP3/WAV bytes
     */
    @PostMapping("/generate")
    public ResponseEntity<byte[]> generateSpeech(
            @RequestParam(value = "text") String text,
            @RequestParam(value = "language", defaultValue = "en") String language) {

        log.info("TTS request: text='{}', language='{}'", text, language);

        byte[] audioBytes = ttsService.textToSpeech(text, language);

        if (audioBytes.length == 0) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("audio/mpeg"))
                .header("Content-Disposition", "attachment; filename=speech.mp3")
                .body(audioBytes);
    }

    /**
     * Get list of supported languages
     */
    @GetMapping("/languages")
    public ResponseEntity<List<String>> getSupportedLanguages() {
        return ResponseEntity.ok(ttsService.getSupportedLanguages());
    }

    /**
     * Health check for TTS service
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("TTS service is running");
    }
}

