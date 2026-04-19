package com.SocializerAI.service.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight application config loader using environment variables.
 */
public class AppConfig {
    private final String emotionApiUrl;
    private final String emotionApiKey;
    private final String defaultHelpline;

    private AppConfig(String emotionApiUrl, String emotionApiKey, String defaultHelpline) {
        this.emotionApiUrl = emotionApiUrl;
        this.emotionApiKey = emotionApiKey;
        this.defaultHelpline = defaultHelpline;
    }

    public static AppConfig load() {
        String url = System.getenv("EMOTION_API_URL");
        String key = System.getenv("EMOTION_API_KEY");
        String helpline = System.getenv("DEFAULT_HELPLINE");

        if (helpline == null || helpline.isEmpty()) {
            helpline = "If you're in immediate danger, call your local emergency services.";
        }

        return new AppConfig(url, key, helpline);
    }

    public String getEmotionApiUrl() { return emotionApiUrl; }
    public String getEmotionApiKey() { return emotionApiKey; }
    public String getDefaultHelpline() { return defaultHelpline; }

    public Map<String, String> toMap() {
        Map<String, String> m = new HashMap<>();
        m.put("emotionApiUrl", emotionApiUrl == null ? "" : emotionApiUrl);
        m.put("defaultHelpline", defaultHelpline == null ? "" : defaultHelpline);
        return m;
    }
}
