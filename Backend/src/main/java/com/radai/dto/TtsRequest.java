package com.radai.dto;

/**
 * Request payload for text-to-speech synthesis.
 */
public class TtsRequest {
    private String text;
    private String rate;
    private String pitch;
    private String language;

    public TtsRequest() {}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getRate() {
        return rate;
    }

    public void setRate(String rate) {
        this.rate = rate;
    }

    public String getPitch() {
        return pitch;
    }

    public void setPitch(String pitch) {
        this.pitch = pitch;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}

