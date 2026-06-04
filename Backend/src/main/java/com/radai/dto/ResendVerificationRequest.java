package com.radai.dto;

/** Body for POST /api/auth/resend-verification — the email to re-send a link to. */
public class ResendVerificationRequest {
    private String email;

    public ResendVerificationRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
