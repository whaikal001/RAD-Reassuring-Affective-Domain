package com.radai.service;

import com.radai.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * Sends transactional emails (currently just account verification).
 *
 * <p>Sending is resilient by design: if {@code app.mail.enabled} is false or no SMTP
 * username is configured, the verification link is logged instead of emailed so that
 * registration never fails just because mail isn't set up yet (e.g. local dev / demo).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.mail.from:RadAI <no-reply@radai.local>}")
    private String fromAddress;

    @Value("${app.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Builds the link the user clicks to verify their email. */
    public String buildVerificationLink(String token) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/verify-email?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }

    /**
     * Sends (or logs) the verification email. Never throws — a mail failure must not
     * roll back or block the registration that triggered it.
     */
    public void sendVerificationEmail(User user, String token) {
        String link = buildVerificationLink(token);

        if (!mailEnabled || smtpUsername == null || smtpUsername.isBlank()) {
            log.warn("[EmailService] Mail disabled/unconfigured — verification link for {}: {}", user.getEmail(), link);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(user.getEmail());
            message.setSubject("Verify your RadAI email");
            message.setText(
                "Hi " + displayName(user) + ",\n\n" +
                "Welcome to RadAI! Please confirm your email address by opening the link below:\n\n" +
                link + "\n\n" +
                "This link expires in 24 hours. If you didn't create a RadAI account, you can ignore this email.\n\n" +
                "— The RadAI team"
            );
            mailSender.send(message);
            log.info("[EmailService] Verification email sent to {}", user.getEmail());
        } catch (Exception ex) {
            // Log and move on — the user can request a resend from the app.
            log.error("[EmailService] Failed to send verification email to {} (link: {})", user.getEmail(), link, ex);
        }
    }

    private String displayName(User user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername();
        return "there";
    }
}
