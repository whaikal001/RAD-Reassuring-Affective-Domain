package com.radai.service.emotion;

import com.radai.service.config.AppConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * EmotionClassifier: optional external API-backed classifier with heuristic fallback.
 */
public class EmotionClassifier {
    private final AppConfig config;

    public EmotionClassifier(AppConfig config) {
        this.config = config;
    }

    /**
     * Return emotion label (e.g., "sadness", "anxiety") or null if unavailable.
     */
    public String classifyEmotion(String text) {
        String apiUrl = config.getEmotionApiUrl();
        if (apiUrl == null || apiUrl.isEmpty()) {
            return null; // caller will fallback
        }

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            String apiKey = config.getEmotionApiKey();
            if (apiKey != null && !apiKey.isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
            conn.setDoOutput(true);

            String payload = String.format("{\"text\": %s}", quote(text));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    String body = sb.toString();
                    // naive parse: look for "emotion":"label"
                    int idx = body.indexOf("\"emotion\"");
                    if (idx != -1) {
                        int col = body.indexOf(':', idx);
                        int q1 = body.indexOf('"', col);
                        int q2 = body.indexOf('"', q1 + 1);
                        if (q1 != -1 && q2 != -1) {
                            return body.substring(q1 + 1, q2);
                        }
                    }
                    // try simple 'label' field
                    idx = body.indexOf("label");
                    if (idx != -1) {
                        int col = body.indexOf(':', idx);
                        int q1 = body.indexOf('"', col);
                        int q2 = body.indexOf('"', q1 + 1);
                        if (q1 != -1 && q2 != -1) {
                            return body.substring(q1 + 1, q2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // ignore and let caller fallback
        }

        return null;
    }

    private String quote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}

