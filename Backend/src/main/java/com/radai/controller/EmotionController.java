package com.radai.controller;

import com.radai.service.EmotionService;
import org.springframework.web.bind.annotation.*;
import com.radai.model.EmotionalHistory;
import java.util.*;

@RestController
@RequestMapping("/api/emotions")
public class EmotionController {
    private final EmotionService svc;

    public EmotionController(EmotionService svc){ this.svc = svc; }

    @PostMapping("/log") public EmotionalHistory log(@RequestBody EmotionalHistory h){ return svc.log(h); }

    @GetMapping("/history/{userId}") public List<EmotionalHistory> history(@PathVariable UUID userId){ return svc.history(userId); }
}

