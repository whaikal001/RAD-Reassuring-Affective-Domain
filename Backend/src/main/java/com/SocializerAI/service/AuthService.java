package com.SocializerAI.service;

import com.SocializerAI.dto.JwtResponse;
import com.SocializerAI.dto.LoginRequest;
import com.SocializerAI.dto.RegisterRequest;
import com.SocializerAI.config.JwtUtil;
import com.SocializerAI.model.User;
import com.SocializerAI.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authManager, JwtUtil jwtUtil, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
        
        // Set optional fields if provided
        if (req.getAge() != null) {
            user.setAge(req.getAge());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }

        // Save user to database
        User savedUser = userRepository.save(user);
        
        // Generate JWT token using the saved user's UUID and include roles
        String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getRoles());
        return new JwtResponse(token, savedUser.getUsername(), savedUser.getRoles(), savedUser.getId().toString());
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
        return new JwtResponse(token, user.getUsername(), user.getRoles(), user.getId().toString());
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
