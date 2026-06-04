package com.radai.dto;

/** Body for POST /api/auth/google — the ID token (credential) returned by Google Identity Services. */
public class GoogleLoginRequest {
    private String idToken;

    public GoogleLoginRequest() {}

    public String getIdToken() { return idToken; }
    public void setIdToken(String idToken) { this.idToken = idToken; }
}
