package com.radai.controller;

import org.springframework.web.bind.annotation.*;
import com.radai.service.PatternService;
import com.radai.model.EmotionalPattern;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/emotions/patterns")
public class PatternController {
    private final PatternService svc;

    public PatternController(PatternService svc){ this.svc = svc; }

    @PostMapping
    public EmotionalPattern create(@RequestBody EmotionalPattern pattern){ return svc.create(pattern); }

    @GetMapping("/user/{userId}")
    public List<EmotionalPattern> byUser(@PathVariable("userId") UUID userId){ return svc.byUser(userId); }

    @GetMapping("/{id}")
    public EmotionalPattern get(@PathVariable("id") UUID id){ return svc.get(id); }

    @PutMapping("/{id}")
    public EmotionalPattern update(@PathVariable("id") UUID id, @RequestBody EmotionalPattern pattern){ return svc.update(id, pattern); }
}

