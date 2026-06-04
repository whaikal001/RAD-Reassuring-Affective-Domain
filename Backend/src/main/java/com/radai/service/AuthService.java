package com.radai.service;

import com.radai.dto.JwtResponse;
import com.radai.dto.LoginRequest;
import com.radai.dto.RegisterRequest;
import com.radai.config.JwtUtil;
import com.radai.model.User;
import com.radai.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${app.verification.expiry-hours:24}")
    private long verificationExpiryHours;

    public AuthService(AuthenticationManager authManager, JwtUtil jwtUtil, UserRepository userRepository,
                       PasswordEncoder passwordEncoder, EmailService emailService, GoogleTokenVerifier googleTokenVerifier) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    // Handle registration - save user with encoded password
    public JwtResponse register(RegisterRequest req) {
        // Check if user already exists
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new RuntimeException("Username already taken");
        }

        // Create new user entity
        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        // default role
        user.setRoles("USER");
        user.setAuthProvider("LOCAL");

        // New local accounts start unverified; issue a verification token + expiry.
        user.setIsVerified(false);
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));

        // Set optional fields if provided
        if (req.getAge() != null) {
            user.setAge(req.getAge());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }

        // Save user to database
        User savedUser = userRepository.save(user);

        // Fire off the verification email (never blocks/throws — see EmailService).
        emailService.sendVerificationEmail(savedUser, verificationToken);

        // We allow login before verification (with an in-app nudge), so still return a token.
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRoles());
        return buildResponse(token, savedUser);
    }

    // Handle login with Spring Security authentication
    public JwtResponse login(LoginRequest req) {
        Authentication auth = authManager.authenticate(
            new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        // Find user by email to get the actual UUID
        var user = userRepository.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token using the user's UUID and include roles
        String token = jwtUtil.generateToken(user.getId(), user.getRoles());
        return buildResponse(token, user);
    }

    // ---- Email verification ---------------------------------------------------

    /** Marks the account matching {@code token} as verified; throws if invalid/expired. */
    public void verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Missing verification token");
        }
        User user = userRepository.findByVerificationToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid or already-used verification link"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            // Idempotent: clicking an old link after verifying is a no-op success.
            user.setVerificationToken(null);
            user.setVerificationTokenExpiresAt(null);
            userRepository.save(user);
            return;
        }

        if (user.getVerificationTokenExpiresAt() != null
                && user.getVerificationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This verification link has expired. Please request a new one.");
        }

        user.setIsVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }

    /** Re-issues a verification token and (re)sends the email. Silent if already verified. */
    public void resendVerification(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Email is required");
        }
        var maybeUser = userRepository.findByEmail(email);
        if (maybeUser.isEmpty()) {
            // Don't reveal whether an email exists; just succeed quietly.
            return;
        }
        User user = maybeUser.get();
        if (Boolean.TRUE.equals(user.getIsVerified())) {
            return;
        }
        String verificationToken = UUID.randomUUID().toString();
        user.setVerificationToken(verificationToken);
        user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(verificationExpiryHours));
        userRepository.save(user);
        emailService.sendVerificationEmail(user, verificationToken);
    }

    // ---- Google sign-in -------------------------------------------------------

    /** Verifies a Google ID token, then logs in (or creates) the matching user. */
    public JwtResponse googleLogin(String idToken) {
        GoogleTokenVerifier.GoogleProfile profile = googleTokenVerifier.verify(idToken);

        User user = userRepository.findByEmail(profile.email()).orElseGet(() -> {
            // First Google sign-in for this email: create a verified account.
            User u = new User();
            u.setEmail(profile.email());
            u.setUsername(uniqueUsernameFrom(profile.email()));
            u.setFullName(profile.name());
            u.setPasswordHash(""); // no local password for Google accounts
            u.setRoles("USER");
            u.setAuthProvider("GOOGLE");
            u.setIsVerified(true); // Google already verified the email
            if (profile.picture() != null) {
                u.setAvatarUrl(profile.picture());
            }
            return u;
        });

        // Existing local account signing in with Google: trust Google's verified email.
        if (Boolean.TRUE.equals(profile.emailVerified()) && !Boolean.TRUE.equals(user.getIsVerified())) {
            user.setIsVerified(true);
            user.setVerificationToken(null);
            user.setVerificationTokenExpiresAt(null);
        }
        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRoles());
        return buildResponse(token, savedUser);
    }

    private String uniqueUsernameFrom(String email) {
        String base = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        base = base.replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (base.isBlank()) base = "user";
        String candidate = base;
        int suffix = 0;
        while (userRepository.findByUsername(candidate).isPresent()) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    private JwtResponse buildResponse(String token, User user) {
        JwtResponse response = new JwtResponse(token, user.getUsername(), user.getRoles(), user.getId().toString());
        response.setVerified(Boolean.TRUE.equals(user.getIsVerified()));
        return response;
    }

    // Handle anonymous login
    public JwtResponse anonymous() {
        UUID anonId = UUID.randomUUID();
        
        // Create anonymous user in database
        User anonUser = new User();
        anonUser.setId(anonId);
        anonUser.setEmail("anon_" + anonId + "@anonymous.local");
        anonUser.setUsername("anon_" + anonId);
        anonUser.setFullName("Anonymous User");
        anonUser.setPasswordHash(""); // No password for anonymous
        anonUser.setIsAnonymous(true);
        anonUser.setIsActive(true);
        anonUser.setIsVerified(false);
        anonUser.setRoles("ANONYMOUS");
        
        // Save anonymous user to database and use the persisted id in case the JPA generator overrides it
        User savedUser = userRepository.save(anonUser);
        UUID persistedUserId = savedUser.getId();
        
        // Generate JWT token using the anonymous user's UUID and include roles
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRoles());
        return new JwtResponse(token, savedUser.getUsername(), savedUser.getRoles(), savedUser.getId().toString());
    }
}

