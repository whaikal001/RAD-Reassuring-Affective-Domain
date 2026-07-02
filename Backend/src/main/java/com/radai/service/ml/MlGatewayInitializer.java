package com.radai.service.ml;

import com.radai.chat.hf.HuggingFaceClient;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MlGatewayInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MlGatewayInitializer.class);

    private final HuggingFaceClient hf;

    @Value("${radai.ml.enabled:true}")
    private boolean mlEnabled;

    @Value("${hf.api.token:}")
    private String hfToken;

    public MlGatewayInitializer(HuggingFaceClient hf) {
        this.hf = hf;
    }

    @PostConstruct
    public void init() {
        boolean tokenPresent = (hfToken != null && !hfToken.isBlank())
            || (System.getenv("HF_API_TOKEN") != null && !System.getenv("HF_API_TOKEN").isBlank());
        boolean on = mlEnabled && tokenPresent;

        if (!on) {
            MlGateway.disable();
            logger.info("ML classification DISABLED (radai.ml.enabled={}, hfToken={}). Engines use rule-based floor only.",
                mlEnabled, tokenPresent ? "present" : "missing");
            return;
        }

        MlGateway.configure(true,
            // Emotion: HF text-classification model → label + score.
            text -> {
                try {
                    Map<String, Object> m = hf.classifyEmotion(text);
                    Object label = m.get("label");
                    Object score = m.get("score");
                    double s = (score instanceof Number n) ? n.doubleValue() : 0.0;
                    return new MlGateway.EmotionResult(String.valueOf(label), s);
                } catch (Exception e) {
                    return null;
                }
            },
            // Zero-shot: returns label → probability (already fail-safe / empty on error).
            hf::classifyZeroShot,
            // Safety: toxic-bert toxicity score 0..1.
            text -> {
                try {
                    Object score = hf.classifySafety(text).get("score");
                    return (score instanceof Number n) ? n.doubleValue() : 0.0;
                } catch (Exception e) {
                    return 0.0;
                }
            });

        logger.info("ML classification ENABLED via HuggingFace models (emotion + zero-shot crisis + toxicity).");
    }
}
