package com.radai.dto;

/** Body for POST /api/auth/verify — the token from the verification email link. */
public class VerifyEmailRequest {
    private String token;

    public VerifyEmailRequest() {}

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
}
