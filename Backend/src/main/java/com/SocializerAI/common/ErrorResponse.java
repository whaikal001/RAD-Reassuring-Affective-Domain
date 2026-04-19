package com.SocializerAI.common;

public class ErrorResponse {
    private String message;
    private String path;
    private int status;

    // Default constructor
    public ErrorResponse() {}

    // Parameterized constructor
    public ErrorResponse(String message, String path, int status) {
        this.message = message;
        this.path = path;
        this.status = status;
    }

    // Getters & Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}
