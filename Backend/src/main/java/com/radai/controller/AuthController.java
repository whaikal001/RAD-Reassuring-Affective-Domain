package com.radai.controller;

import com.radai.service.AuthService;
import com.radai.dto.GoogleLoginRequest;
import com.radai.dto.JwtResponse;
import com.radai.dto.LoginRequest;
import com.radai.dto.RegisterRequest;
import com.radai.dto.ResendVerificationRequest;
import com.radai.dto.VerifyEmailRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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

    /** Sign in (or create an account) with a Google ID token from the browser. */
    @PostMapping("/google")
    public JwtResponse google(@RequestBody GoogleLoginRequest req) {
        return svc.googleLogin(req.getIdToken());
    }

    /** Confirm an email from the link the user received. */
    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verify(@RequestBody VerifyEmailRequest req) {
        try {
            svc.verifyEmail(req.getToken());
            return ResponseEntity.ok(Map.of("verified", true, "message", "Email verified successfully."));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("verified", false, "message", ex.getMessage()));
        }
    }

    /** Re-send the verification email. Always 200 so we don't leak which emails exist. */
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, Object>> resend(@RequestBody ResendVerificationRequest req) {
        svc.resendVerification(req.getEmail());
        return ResponseEntity.ok(Map.of("message", "If that email needs verifying, a new link is on its way."));
    }
}

