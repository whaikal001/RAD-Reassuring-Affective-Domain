package com.SocializerAI.controller;

import org.springframework.web.bind.annotation.*;
import com.SocializerAI.model.UserPreferences;
import com.SocializerAI.service.UserPreferencesService;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/preferences")
public class UserPreferencesController {
    private final UserPreferencesService svc;

    public UserPreferencesController(UserPreferencesService svc){ this.svc = svc; }

    @PostMapping
    public UserPreferences create(@RequestBody UserPreferences preferences){ return svc.create(preferences); }

    @GetMapping("/{userId}")
    public UserPreferences get(@PathVariable("userId") UUID userId){ return svc.get(userId); }

    @PutMapping("/{userId}")
    public UserPreferences update(@PathVariable("userId") UUID userId, @RequestBody UserPreferences preferences){ return svc.update(userId, preferences); }
}
