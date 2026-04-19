package com.SocializerAI.controller;

import com.SocializerAI.service.AuthService;
import com.SocializerAI.dto.JwtResponse;
import com.SocializerAI.dto.LoginRequest;
import com.SocializerAI.dto.RegisterRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService svc;

    public AuthController(AuthService svc) {
        this.svc = svc;
    }

    @GetMapping("/health")
    public String health() {
        return "OK";
    }

    @PostMapping("/register")
    public JwtResponse register(@RequestBody RegisterRequest req) {
        return svc.register(req);
    }

    @PostMapping("/login")
    public JwtResponse login(@RequestBody LoginRequest req) {
        return svc.login(req);
    }

    @PostMapping("/anonymous")
    public JwtResponse anonymous() {
        return svc.anonymous();
    }
}
