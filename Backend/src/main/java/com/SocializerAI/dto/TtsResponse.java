package com.SocializerAI.dto;

/**
 * Response returned after TTS synthesis.
 */
public class TtsResponse {
    private String ssml;
    private String voiceHint;
    private String language;

    public TtsResponse() {}

    public TtsResponse(String ssml, String voiceHint, String language) {
        this.ssml = ssml;
        this.voiceHint = voiceHint;
        this.language = language;
    }

    public String getSsml() {
        return ssml;
    }

    public void setSsml(String ssml) {
        this.ssml = ssml;
    }

    public String getVoiceHint() {
        return voiceHint;
    }

    public void setVoiceHint(String voiceHint) {
        this.voiceHint = voiceHint;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
