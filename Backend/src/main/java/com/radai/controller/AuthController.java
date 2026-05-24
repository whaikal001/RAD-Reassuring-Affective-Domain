package com.radai.controller;

import com.radai.service.AuthService;
import com.radai.dto.JwtResponse;
import com.radai.dto.LoginRequest;
import com.radai.dto.RegisterRequest;
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

