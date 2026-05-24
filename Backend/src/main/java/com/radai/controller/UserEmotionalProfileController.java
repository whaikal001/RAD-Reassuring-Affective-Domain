package com.radai.controller;

import org.springframework.web.bind.annotation.*;
import com.radai.model.UserEmotionalProfile;
import com.radai.service.UserEmotionalProfileService;
import java.util.UUID;

@RestController
@RequestMapping("/api/user/emotional-profile")
public class UserEmotionalProfileController {
    private final UserEmotionalProfileService svc;

    public UserEmotionalProfileController(UserEmotionalProfileService svc){ this.svc = svc; }

    @PostMapping
    public UserEmotionalProfile create(@RequestBody UserEmotionalProfile profile){ return svc.create(profile); }

    @GetMapping("/{userId}")
    public UserEmotionalProfile get(@PathVariable("userId") UUID userId){ return svc.get(userId); }

    @PutMapping("/{userId}")
    public UserEmotionalProfile update(@PathVariable("userId") UUID userId, @RequestBody UserEmotionalProfile profile){ return svc.update(userId, profile); }
}

