package com.SocializerAI.chat.hf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import jakarta.annotation.PostConstruct;

@Component
public class HuggingFaceClient {
    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceClient.class);
    
    @Value("${hf.api.token}") 
    private String hfToken;
    
    @Value("${hf.chat.model}") 
    private String chatModel;

    @Value("${hf.chat.api-mode:chat}")
    private String chatApiMode;
    
    @Value("${hf.emotion.model}") 
    private String emotionModel;

    @Value("${hf.screening.model}")
    private String screeningModel;

    @Value("${hf.safety.model}")
    private String safetyModel;

    @Value("${hf.tts.model:}")
    private String ttsModel;

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    
    // Conversation memory with enhanced context tracking
    private final Map<String, ConversationContext> conversationMemory = new ConcurrentHashMap<>();
    
    private static class ConversationContext {
        List<String> pastUserInputs = new ArrayList<>();      // actual user messages for API
        List<String> generatedResponses = new ArrayList<>();  // actual bot responses for API
        List<String> recentTopics = new ArrayList<>();        // topics for context tracking
        List<String> emotionHistory = new ArrayList<>();      // track emotions over conversation
        List<String> keyDetails = new ArrayList<>();          // important details user shared
        String dominantEmotion = "neutral";
        String userName = null;                               // user's name if shared
        int messageCount = 0;
        int positiveResponseCount = 0;                        // track engagement
        LocalDateTime sessionStartTime = LocalDateTime.now();
        LocalDateTime lastMessageTime = LocalDateTime.now();
        boolean needsEmpathy = true;
        boolean hasSharedProblem = false;
        String mainConcern = null;                            // primary issue discussed
        
        // Build context summary for AI system prompt
        String getContextSummary() {
            StringBuilder summary = new StringBuilder();
            if (userName != null) {
                summary.append("User's name: ").append(userName).append(". ");
            }
            if (mainConcern != null) {
                summary.append("Main concern: ").append(mainConcern).append(". ");
            }
            if (!emotionHistory.isEmpty()) {
                String recentEmotion = emotionHistory.get(emotionHistory.size() - 1);
                summary.append("Current emotion: ").append(recentEmotion).append(". ");
            }
            if (!keyDetails.isEmpty()) {
                summary.append("Key details shared: ").append(String.join(", ", keyDetails)).append(". ");
            }
            summary.append("Message count: ").append(messageCount).append(". ");
            return summary.toString();
        }
        
        // Extract key details from user message
        void extractKeyDetails(String message) {
            String lower = message.toLowerCase();
            
            // Extract name
            if (lower.contains("my name is ") || lower.contains("i'm ") || lower.contains("i am ")) {
                // Simple name extraction
                String[] words = message.split("\\s+");
                for (int i = 0; i < words.length - 1; i++) {
                    if (words[i].equalsIgnoreCase("is") || words[i].equalsIgnoreCase("i'm") || 
                        words[i].equalsIgnoreCase("am")) {
                        String potentialName = words[i + 1].replaceAll("[^a-zA-Z]", "");
                        if (potentialName.length() > 1 && Character.isUpperCase(potentialName.charAt(0))) {
                            userName = potentialName;
                            break;
                        }
                    }
                }
            }
            
            // Extract main concern if mentioned
            if (!hasSharedProblem) {
                if (lower.contains("problem") || lower.contains("struggling") || lower.contains("worried") ||
                    lower.contains("stressed") || lower.contains("anxious") || lower.contains("sad") ||
                    lower.contains("depressed") || lower.contains("overwhelmed")) {
                    hasSharedProblem = true;
                    // Extract a short summary as main concern
                    if (message.length() > 100) {
                        mainConcern = message.substring(0, 100) + "...";
                    } else {
                        mainConcern = message;
                    }
                }
            }
            
            // Extract specific details (school, work, relationships, etc.)
            if (lower.contains("exam") || lower.contains("test") || lower.contains("school") ||
                lower.contains("university") || lower.contains("college")) {
                addKeyDetail("academic concerns");
            }
            if (lower.contains("work") || lower.contains("job") || lower.contains("boss") ||
                lower.contains("colleague") || lower.contains("career")) {
                addKeyDetail("work-related stress");
            }
            if (lower.contains("relationship") || lower.contains("partner") || lower.contains("boyfriend") ||
                lower.contains("girlfriend") || lower.contains("friend") || lower.contains("family")) {
                addKeyDetail("relationship concerns");
            }
            if (lower.contains("sleep") || lower.contains("insomnia") || lower.contains("tired") ||
                lower.contains("exhausted")) {
                addKeyDetail("sleep issues");
            }
        }
        
        void addKeyDetail(String detail) {
            if (!keyDetails.contains(detail) && keyDetails.size() < 5) {
                keyDetails.add(detail);
            }
        }
        
        void addEmotion(String emotion) {
            emotionHistory.add(emotion);
            if (emotionHistory.size() > 10) {
                emotionHistory.remove(0);
            }
            dominantEmotion = emotion;
        }
    }

    @PostConstruct
    private void logModelConfig() {
        String tokenState = (hfToken != null && !hfToken.isBlank()) ? "present" : "missing";
        logger.info("HF models active - chat: {} (mode: {}), emotion: {}, screening: {}, safety: {}, tts: {}, token: {}", chatModel, chatApiMode, emotionModel, screeningModel, safetyModel, ttsModel == null ? "" : ttsModel, tokenState);
    }

    public String generateReply(String userText) throws Exception {
        return generateReply(userText, "anonymous");
    }
    
    public String generateReply(String userText, String userId) throws Exception {
        // HuggingFace API with retry for model loading
        logger.info("Calling HuggingFace AI for user: {}", userId);
        
        int maxRetries = 3;
        int retryDelayMs = 5000; // 5 seconds
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String aiResponse = callHuggingFaceAPI(userText, userId);
                logger.info("Successfully got AI response: {} chars", aiResponse.length());
                return aiResponse;
            } catch (Exception e) {
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                // Check if model is loading (503 error or "loading" in message)
                if (errorMsg.contains("loading") || errorMsg.contains("503") || errorMsg.contains("service unavailable")) {
                    if (attempt < maxRetries) {
                        logger.warn("Model is loading, waiting {} seconds before retry {}/{}...", retryDelayMs/1000, attempt, maxRetries);
                        Thread.sleep(retryDelayMs);
                        retryDelayMs *= 2; // Exponential backoff
                    } else {
                        logger.error("Model still loading after {} retries, giving up", maxRetries);
                        throw e;
                    }
                } else {
                    // Non-loading error, don't retry
                    throw e;
                }
            }
        }
        throw new Exception("Failed to get response after " + maxRetries + " retries");
    }
    
    private String callHuggingFaceAPI(String userText, String userId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hfToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "SocializerAI/1.0");
        
        // Get or create conversation history for this user
        ConversationContext ctx = conversationMemory.computeIfAbsent(userId, k -> new ConversationContext());
        
        // Extract key details from current message for memory
        ctx.extractKeyDetails(userText);
        
        // Build chat messages array
        List<Map<String, String>> messages = new ArrayList<>();
        
        // Build dynamic system prompt with conversation context
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are a compassionate mental health support chatbot like Woebot. ");
        systemPrompt.append("Be empathetic, warm, and conversational. Listen actively and validate feelings. ");
        systemPrompt.append("Ask thoughtful follow-up questions to understand the user better. ");
        systemPrompt.append("Offer gentle coping strategies when appropriate. Keep responses concise but caring. ");
        
        // Add personalized context if available
        String contextSummary = ctx.getContextSummary();
        if (!contextSummary.isBlank()) {
            systemPrompt.append("\n\nConversation context: ").append(contextSummary);
        }
        
        // Add guidance based on conversation stage
        if (ctx.messageCount == 0) {
            systemPrompt.append("\nThis is the start of the conversation. Greet warmly and ask how they're feeling.");
        } else if (ctx.messageCount < 3) {
            systemPrompt.append("\nEarly in conversation. Focus on understanding their situation without jumping to solutions.");
        } else if (ctx.hasSharedProblem && ctx.messageCount >= 3) {
            systemPrompt.append("\nThey've shared their concerns. You can offer gentle support or coping suggestions if appropriate.");
        }
        
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt.toString());
        messages.add(systemMsg);
        
        // Add conversation history (alternating user/assistant)
        for (int i = 0; i < ctx.pastUserInputs.size(); i++) {
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", ctx.pastUserInputs.get(i));
            messages.add(userMsg);
            
            if (i < ctx.generatedResponses.size()) {
                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", ctx.generatedResponses.get(i));
                messages.add(assistantMsg);
            }
        }
        
        // Add current user message
        Map<String, String> currentMsg = new HashMap<>();
        currentMsg.put("role", "user");
        currentMsg.put("content", userText);
        messages.add(currentMsg);

        String mode = chatApiMode == null ? "chat" : chatApiMode.trim().toLowerCase();
        if ("inference".equals(mode)) {
            return callInferenceEndpoint(userText, ctx, headers);
        }

        return callChatCompletionsEndpoint(userText, ctx, messages, headers);
    }

    private String callChatCompletionsEndpoint(String userText, ConversationContext ctx, List<Map<String, String>> messages, HttpHeaders headers) throws Exception {
        String url = "https://router.huggingface.co/v1/chat/completions";
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        
        // Create chat-completions payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", chatModel);
        payload.put("messages", messages);
        payload.put("max_tokens", 250);
        payload.put("temperature", 0.7);
        
        logger.info("Calling HuggingFace Router API - URL: {}, Model: {}, User Message: '{}'", url, chatModel, userText);
        
        try {
            ResponseEntity<String> res = rest.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            String responseBody = res.getBody();
            
            logger.info("HF API Response Status: {}", res.getStatusCode());
            logger.debug("HF API Response Body: {}", responseBody);
            
            if (res.getStatusCode() != HttpStatus.OK) {
                logger.error("HF API returned non-200 status: {}", res.getStatusCode());
                throw new Exception("API returned status: " + res.getStatusCode());
            }
            
            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("HF API returned empty response");
                throw new Exception("Empty response from API");
            }
            
            // Parse the response
            String generatedText = parseHFResponse(responseBody);
            
            if (generatedText == null || generatedText.isEmpty()) {
                logger.error("Could not extract text from response: {}", responseBody);
                throw new Exception("Could not extract text from API response");
            }
            
            // Clean up the response
            generatedText = cleanResponse(generatedText, userText);
            
            if (generatedText.length() < 10) {
                logger.warn("Response too short after cleaning: {} - {}", generatedText.length(), generatedText);
                throw new Exception("Response too short after cleaning: " + generatedText.length());
            }
            
            // Save to conversation history
            ctx.pastUserInputs.add(userText);
            ctx.generatedResponses.add(generatedText);
            ctx.messageCount++;
            ctx.lastMessageTime = LocalDateTime.now();
            
            // Keep history short (manage token limits for context window)
            if (ctx.pastUserInputs.size() > 5) {
                ctx.pastUserInputs.remove(0);
                ctx.generatedResponses.remove(0);
            }
            
            logger.info("Successfully generated {} characters from HuggingFace chat completions API", generatedText.length());
            return generatedText;

        } catch (HttpClientErrorException e) {
            logger.error("HTTP exception from HF API - Status: {}, Message: {}", e.getStatusCode(), e.getMessage());
            logger.error("Response Body: {}", e.getResponseBodyAsString());
            throw new Exception("HF API HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Exception calling HF API: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    private String callInferenceEndpoint(String userText, ConversationContext ctx, HttpHeaders headers) throws Exception {
        // Hugging Face deprecated api-inference.huggingface.co. Router is the supported endpoint.
        String url = "https://router.huggingface.co/hf-inference/models/" + chatModel;
        String prompt = buildInferencePrompt(userText, ctx);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_new_tokens", 220);
        parameters.put("temperature", 0.7);
        parameters.put("return_full_text", false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", prompt);
        payload.put("parameters", parameters);

        logger.info("Calling HuggingFace Router Inference API - URL: {}, Model: {}, User Message: '{}'", url, chatModel, userText);

        try {
            ResponseEntity<String> res = rest.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            String responseBody = res.getBody();

            logger.info("HF Inference API Response Status: {}", res.getStatusCode());
            logger.debug("HF Inference API Response Body: {}", responseBody);

            if (res.getStatusCode() != HttpStatus.OK) {
                logger.error("HF Inference API returned non-200 status: {}", res.getStatusCode());
                throw new Exception("Inference API returned status: " + res.getStatusCode());
            }

            if (responseBody == null || responseBody.trim().isEmpty()) {
                logger.error("HF Inference API returned empty response");
                throw new Exception("Empty response from Inference API");
            }

            String generatedText = parseHFResponse(responseBody);

            if (generatedText == null || generatedText.isEmpty()) {
                logger.error("Could not extract text from inference response: {}", responseBody);
                throw new Exception("Could not extract text from inference API response");
            }

            generatedText = cleanResponse(generatedText, userText);

            if (generatedText.length() < 10) {
                logger.warn("Inference response too short after cleaning: {} - {}", generatedText.length(), generatedText);
                throw new Exception("Inference response too short after cleaning: " + generatedText.length());
            }

            ctx.pastUserInputs.add(userText);
            ctx.generatedResponses.add(generatedText);
            ctx.messageCount++;
            ctx.lastMessageTime = LocalDateTime.now();

            if (ctx.pastUserInputs.size() > 5) {
                ctx.pastUserInputs.remove(0);
                ctx.generatedResponses.remove(0);
            }

            logger.info("Successfully generated {} characters from HuggingFace inference API", generatedText.length());
            return generatedText;
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP exception from HF API - Status: {}, Message: {}", e.getStatusCode(), e.getMessage());
            logger.error("Response Body: {}", e.getResponseBodyAsString());
            throw new Exception("HF API HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Exception calling HF API: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    private String buildInferencePrompt(String userText, ConversationContext ctx) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a compassionate mental health support assistant. ");
        prompt.append("Be empathetic, validating, safe, and practical. ");
        prompt.append("Keep answers concise (3-6 sentences) and include one helpful next step.\n\n");

        if (ctx != null && !ctx.getContextSummary().isBlank()) {
            prompt.append("Context: ").append(ctx.getContextSummary()).append("\n");
        }

        prompt.append("User: ").append(userText).append("\n");
        prompt.append("Assistant:");
        return prompt.toString();
    }
    
    /**
    * Parse HuggingFace API response - chat completions format
     */
    private String parseHFResponse(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            
            // Chat completions format: {"choices": [{"message": {"content": "..."}}]}
            if (root.isObject() && root.has("choices")) {
                JsonNode choices = root.get("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice.has("message")) {
                        JsonNode message = firstChoice.get("message");
                        if (message.has("content")) {
                            return message.get("content").asText();
                        }
                    }
                    // Fallback: text field directly on choice
                    if (firstChoice.has("text")) {
                        return firstChoice.get("text").asText();
                    }
                }
            }
            
            // Handle array response (legacy format)
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                if (first.isObject()) {
                    if (first.has("generated_text")) {
                        return first.get("generated_text").asText();
                    }
                } else if (first.isTextual()) {
                    return first.asText();
                }
            }
            
            // Handle object response (legacy conversational)
            if (root.isObject()) {
                if (root.has("generated_text")) {
                    return root.get("generated_text").asText();
                }
                if (root.has("conversation")) {
                    JsonNode conv = root.get("conversation");
                    if (conv.has("generated_responses")) {
                        JsonNode responses = conv.get("generated_responses");
                        if (responses.isArray() && responses.size() > 0) {
                            return responses.get(responses.size() - 1).asText();
                        }
                    }
                }
                if (root.has("text")) {
                    return root.get("text").asText();
                }
                if (root.has("response")) {
                    return root.get("response").asText();
                }
            }
            
        } catch (Exception e) {
            logger.warn("JSON parsing failed: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Clean up the generated response
     */
    private String cleanResponse(String text, String userInput) {
        // Strip echoed prompt only when it appears at the beginning.
        if (userInput != null && !userInput.isBlank() &&
                text.toLowerCase().startsWith(userInput.toLowerCase())) {
            text = text.substring(userInput.length()).trim();
        }
        
        // Remove common prefixes
        text = text.replaceAll("^(AI|Bot|Assistant|Human|User):\\s*", "");
        
        // Remove excessive whitespace
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    private String enhanceWithWoebotStyle(String userText, String aiResponse, String userId) {
        ConversationContext context = conversationMemory.computeIfAbsent(userId, k -> new ConversationContext());
        context.messageCount++;
        context.lastMessageTime = LocalDateTime.now();
        
        String lower = userText.toLowerCase();
        String emotion = detectEmotion(lower);
        context.dominantEmotion = emotion;
        
        // Track topics
        String topic = extractTopic(lower);
        if (topic != null && !context.recentTopics.contains(topic)) {
            context.recentTopics.add(topic);
        }
        
        boolean useSympathy = shouldUseSympathy(lower, emotion, context);
        
        // Build response with empathy opening + AI response + strategies + followup
        StringBuilder response = new StringBuilder();
        response.append(generateOpening(emotion, useSympathy, context));
        response.append("\\n\\n");
        
        if (aiResponse != null && !aiResponse.isEmpty()) {
            response.append(aiResponse);
        } else {
            response.append(generateValidation(emotion, userText));
        }
        
        response.append("\\n\\n");
        
        if (lower.contains("assignment") || lower.contains("stress")) {
            response.append(generateStrategies(emotion, userText, context));
            response.append("\\n\\n");
        }
        
        response.append(generateFollowUp(emotion, context));
        
        return response.toString();
    }
    
    private String detectEmotion(String text) {
        if (text.matches(".*(stress|overwhelm|pressure|too much).*")) return "stress";
        if (text.matches(".*(anxious|panic|worried|afraid|scared).*")) return "anxiety";
        if (text.matches(".*(sad|down|unhappy|depressed|down|blue).*")) return "sadness";
        if (text.matches(".*(angry|mad|frustrated|furious|hate).*")) return "anger";
        if (text.matches(".*(tired|exhausted|drained|fatigue).*")) return "exhaustion";
        if (text.matches(".*(lonely|isolated|alone).*")) return "loneliness";
        if (text.matches(".*(happy|great|wonderful|excited|amazing).*")) return "joy";
        return "neutral";
    }
    
    private String extractTopic(String text) {
        if (text.contains("assignment") || text.contains("homework") || text.contains("school")) return "school";
        if (text.contains("work") || text.contains("job") || text.contains("career")) return "work";
        if (text.contains("relationship") || text.contains("partner")) return "relationship";
        if (text.contains("family") || text.contains("parents")) return "family";
        if (text.contains("friend")) return "friends";
        return null;
    }
    
    private boolean shouldUseSympathy(String text, String emotion, ConversationContext context) {
        if (text.matches(".*(crisis|suicide|death|died|abuse|trauma).*")) return true;
        if (emotion.equals("sadness") && context.messageCount >= 3) return true;
        if (emotion.equals("anxiety") && context.messageCount >= 2) return true;
        return false;
    }
    
    private String generateOpening(String emotion, boolean useSympathy, ConversationContext context) {
        if (useSympathy) {
            return "I'm so sorry you're going through this. What you're feeling is real. 💙";
        }
        
        switch (emotion) {
            case "stress":
                return "I hear you – that stress is really overwhelming.";
            case "anxiety":
                return "I understand how anxiety can make everything feel impossible.";
            case "sadness":
                return "I can feel the weight in what you're saying.";
            case "anger":
                return "Your anger makes complete sense.";
            case "exhaustion":
                return "It sounds like you're running on empty.";
            case "loneliness":
                return "I hear the loneliness in your words.";
            default:
                return "I hear you, and I'm here to help.";
        }
    }
    
    private String generateValidation(String emotion, String text) {
        switch (emotion) {
            case "stress":
                return "Multiple deadlines at once is genuinely overwhelming. Your feelings are completely valid.";
            case "anxiety":
                return "Anxiety isn't weakness – it's real, and what you're feeling makes sense.";
            case "sadness":
                return "These feelings are real and deserve space and compassion.";
            case "anger":
                return "Your anger is telling you something important.";
            default:
                return "Your feelings are valid and deserve attention.";
        }
    }
    
    private String generateStrategies(String emotion, String text, ConversationContext context) {
        StringBuilder strategies = new StringBuilder();
        
        if (text.contains("assignment") || text.contains("homework")) {
            strategies.append("**What might help:**\\n");
            strategies.append("• Break it into 3-4 tiny tasks – just start with one\\n");
            strategies.append("• Work for 25 mins, then 5-min break (Pomodoro)\\n");
            strategies.append("• Ask for help or extension if needed\\n");
            strategies.append("• Done is better than perfect");
        } else {
            strategies.append("**Things that might help:**\\n");
            switch (emotion) {
                case "anxiety":
                    strategies.append("• 5-4-3-2-1 grounding: name things you see, touch, hear, smell, taste\\n");
                    strategies.append("• Try box breathing: in-4, hold-4, out-4, hold-4\\n");
                    strategies.append("• Move your body – even a short walk helps");
                    break;
                case "stress":
                    strategies.append("• Brain dump: write down everything you're worried about\\n");
                    strategies.append("• Pick ONE most urgent thing – ignore the rest for now\\n");
                    strategies.append("• Take 3 deep breaths and reset");
                    break;
                default:
                    strategies.append("• Take a pause and breathe\\n");
                    strategies.append("• Do one small thing that usually helps you\\n");
                    strategies.append("• Reach out to someone you trust");
            }
        }
        
        return strategies.toString();
    }
    
    private String generateFollowUp(String emotion, ConversationContext context) {
        if (context.messageCount == 1) {
            return "What feels most urgent right now?";
        } else if (context.messageCount >= 3) {
            return "How are you feeling now, compared to when we started talking?";
        } else {
            switch (emotion) {
                case "stress":
                    return "What's the one thing that, if handled, would give you the most relief?";
                case "anxiety":
                    return "What's the main thing you're anxious about?";
                case "sadness":
                    return "What would support look like for you right now?";
                default:
                    return "What would help you most right now?";
            }
        }
    }

    public Map<String,Object> classifyEmotion(String text) throws Exception {
        // Using new router endpoint
        String url = "https://router.huggingface.co/hf-inference/models/" + emotionModel;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hfToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "SocializerAI/1.0");
        
        Map<String,Object> payload = Map.of("inputs", text);
        
        try {
            ResponseEntity<String> res = rest.postForEntity(url, new HttpEntity<>(payload, headers), String.class);
            
            if (res.getStatusCode() != HttpStatus.OK) {
                logger.error("Emotion classification API error: {}", res.getStatusCode());
                return Map.of("label", "neutral", "score", 0.5);
            }
            
            JsonNode root = mapper.readTree(res.getBody());
            
            if (root.isArray() && root.size() > 0) {
                JsonNode first = root.get(0);
                if (first.isArray() && first.size() > 0) {
                    JsonNode best = first.get(0);
                    if (best.has("label") && best.has("score")) {
                        return Map.of(
                            "label", best.get("label").asText(),
                            "score", best.get("score").asDouble()
                        );
                    }
                }
            }
            
            return Map.of("label", "neutral", "score", 0.5);
            
        } catch (HttpClientErrorException e) {
            logger.warn("Emotion classification failed: {}", e.getStatusCode());
            return Map.of("label", "neutral", "score", 0.5);
        }
    }

    public Map<String,Object> classifySafety(String text) throws Exception {
        String url = "https://router.huggingface.co/hf-inference/models/" + safetyModel;
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(hfToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("User-Agent", "SocializerAI/1.0");

        Map<String,Object> payload = Map.of("inputs", text);

        try {
            ResponseEntity<String> res = rest.postForEntity(url, new HttpEntity<>(payload, headers), String.class);

            if (res.getStatusCode() != HttpStatus.OK) {
                logger.error("Safety classification API error: {}", res.getStatusCode());
                return Map.of("label", "non-toxic", "score", 0.0);
            }

            JsonNode root = mapper.readTree(res.getBody());
            double toxicityScore = extractToxicityScore(root);
            String label = toxicityScore >= 0.5 ? "toxic" : "non-toxic";

            return Map.of("label", label, "score", toxicityScore);
        } catch (HttpClientErrorException e) {
            logger.warn("Safety classification failed: {}", e.getStatusCode());
            return Map.of("label", "non-toxic", "score", 0.0);
        }
    }

    private double extractToxicityScore(JsonNode root) {
        double toxicity = 0.0;

        if (root == null || root.isNull()) {
            return toxicity;
        }

        // Typical HF output: [[{"label":"toxic","score":0.98}, {"label":"non-toxic","score":0.02}]]
        if (root.isArray() && root.size() > 0) {
            JsonNode first = root.get(0);
            if (first.isArray()) {
                for (JsonNode item : first) {
                    if (item.has("label") && item.has("score")) {
                        String label = item.get("label").asText("").toLowerCase();
                        double score = item.get("score").asDouble(0.0);
                        if (label.contains("toxic") && !label.contains("non")) {
                            toxicity = Math.max(toxicity, score);
                        }
                    }
                }
                return toxicity;
            }

            if (first.isObject() && first.has("label") && first.has("score")) {
                String label = first.get("label").asText("").toLowerCase();
                double score = first.get("score").asDouble(0.0);
                if (label.contains("toxic") && !label.contains("non")) {
                    return score;
                }
            }
        }

        return toxicity;
    }
}
