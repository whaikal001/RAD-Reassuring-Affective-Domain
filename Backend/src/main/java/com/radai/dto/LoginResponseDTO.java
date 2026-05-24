package com.radai.dto;

public class LoginResponseDTO {
    private String token;
    private String username;
    private String userId;
    private String email;
    private long expiresIn;
    private String message;

    public LoginResponseDTO(String token, String message) {
        this.token = token;
        this.message = message;
    }

    public LoginResponseDTO(String token, String username, String userId, String email, long expiresIn) {
        this.token = token;
        this.username = username;
        this.userId = userId;
        this.email = email;
        this.expiresIn = expiresIn;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

