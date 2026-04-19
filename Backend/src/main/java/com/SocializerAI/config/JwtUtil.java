package com.SocializerAI.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(@Value("${app.jwt.secret}") String secret,
                   @Value("${app.jwt.expiration}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    // Generate token with UUID subject (for anonymous or registered users with UUID IDs)
    public String generateToken(UUID userId) {
        return generateToken(userId.toString());
    }

    // Generate token with String subject (username, email, or UUID string)
    public String generateToken(String subject) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Generate token with roles claim (comma-separated)
    public String generateToken(String subject, String rolesCsv) {
        return Jwts.builder()
                .setSubject(subject)
                .claim("roles", rolesCsv)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Generate token with UUID subject and roles claim
    public String generateToken(UUID userId, String rolesCsv) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("roles", rolesCsv)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Extract subject as String
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extract subject as UUID (if you know it’s a UUID)
    public UUID extractSubjectAsUUID(String token) {
        return UUID.fromString(extractSubject(token));
    }

    // Generic claim extractor
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claimsResolver.apply(claims);
    }

    public String extractRoles(String token) {
        return extractClaim(token, c -> c.get("roles", String.class));
    }

    // Validate token against expected subject
    public boolean validateToken(String token, String expectedSubject) {
        final String subject = extractSubject(token);
        return (subject.equals(expectedSubject) && !isTokenExpired(token));
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}
