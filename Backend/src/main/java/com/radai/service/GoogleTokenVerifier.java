package com.radai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Verifies a Google Identity Services ID token (the {@code credential} the browser
 * sends) by asking Google's tokeninfo endpoint and checking the audience matches
 * our configured OAuth Web client ID.
 *
 * <p>tokeninfo is the simplest server-side verification path — it validates the
 * signature and expiry for us. Fine for this app's traffic; for very high volume
 * you'd switch to local verification with the google-api-client library.
 */
@Service
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.google.client-id:}")
    private String clientId;

    /** Minimal verified profile extracted from a valid Google ID token. */
    public record GoogleProfile(String email, String name, String picture, String subject, boolean emailVerified) {}

    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }

    /**
     * @return the verified profile, or throws {@link IllegalArgumentException} if the
     *         token is invalid or was issued for a different client.
     */
    @SuppressWarnings("unchecked")
    public GoogleProfile verify(String idToken) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google sign-in is not configured (GOOGLE_CLIENT_ID is empty).");
        }
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Missing Google credential.");
        }

        Map<String, Object> info;
        try {
            info = restTemplate.getForObject(TOKENINFO_URL + idToken, Map.class);
        } catch (Exception ex) {
            log.warn("[GoogleTokenVerifier] tokeninfo call failed", ex);
            throw new IllegalArgumentException("Could not verify Google token.");
        }
        if (info == null) {
            throw new IllegalArgumentException("Invalid Google token.");
        }

        String aud = String.valueOf(info.get("aud"));
        if (!clientId.equals(aud)) {
            log.warn("[GoogleTokenVerifier] audience mismatch: token aud={} expected={}", aud, clientId);
            throw new IllegalArgumentException("Google token was issued for a different app.");
        }

        String email = (String) info.get("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google token has no email.");
        }

        boolean emailVerified = "true".equalsIgnoreCase(String.valueOf(info.get("email_verified")));
        String name = (String) info.getOrDefault("name", email);
        String picture = (String) info.get("picture");
        String subject = (String) info.get("sub");

        return new GoogleProfile(email, name, picture, subject, emailVerified);
    }
}
