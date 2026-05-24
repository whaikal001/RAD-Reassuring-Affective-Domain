package com.radai.controller;

import com.radai.dto.TtsRequest;
import com.radai.dto.TtsResponse;
import com.radai.service.TtsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tts")
public class TtsController {

    private final TtsService ttsService;

    public TtsController(TtsService ttsService) {
        this.ttsService = ttsService;
    }

    @PostMapping("/synthesize")
    public ResponseEntity<?> synthesize(@RequestBody TtsRequest request) {
        try {
            TtsResponse response = ttsService.synthesize(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}

