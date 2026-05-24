package com.radai.service;

import com.radai.chat.hf.HuggingFaceClient;
import com.radai.enums.ApproachType;
import com.radai.enums.IntensityLevel;
import com.radai.model.MonitoringContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Advanced HuggingFace Response Enhancer for Human-Like Responses
 * 
 * Features:
 * - Context-aware prompt engineering
 * - Emotion-specific response generation
 * - Conversation history integration
 * - Natural language personalization
 * - Response quality validation
 */
public class HuggingFaceResponseEnhancer {
    private static final Logger logger = LoggerFactory.getLogger(HuggingFaceResponseEnhancer.class);

    private final HuggingFaceClient huggingFaceClient;

    public HuggingFaceResponseEnhancer(HuggingFaceClient huggingFaceClient) {
        this.huggingFaceClient = huggingFaceClient;
    }

    /**
     * Generate human-like response with context awareness
     * 
     * @param userMessage The user's message
     * @param context Current conversation context
     * @param approach EMPATHY or SYMPATHY
     * @return Human-like AI response
     */
    public String generateHumanLikeResponse(String userMessage, MonitoringContext context, ApproachType approach) {
        return generateHumanLikeResponse(userMessage, context, approach, "en");
    }

    /**
     * Generate human-like response with context awareness and language support
     */
    public String generateHumanLikeResponse(String userMessage, MonitoringContext context, ApproachType approach, String language) {
        try {
            boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
            
            logger.info("Generating BlenderBot response for user message: {} (language: {}, approach: {})", userMessage, language, approach);

            // For Malay, prepend language instruction to help AI respond appropriately
            String approachPrefix = approach == ApproachType.SYMPATHY ? "[APPROACH: SYMPATHY] " : "[APPROACH: EMPATHY] ";
            String langPrefix = isMalay ? "[Respond in Malay/Bahasa Malaysia] " : "";
            String promptMessage = approachPrefix + langPrefix + userMessage;

            logger.debug("HF promptMessage (preview): {}", promptMessage.length() > 200 ? promptMessage.substring(0, 200) + "..." : promptMessage);

            String aiResponse = huggingFaceClient.generateReply(promptMessage, context.getUserId().toString());

            if (aiResponse != null && !aiResponse.isEmpty()) {
                logger.info("Generated BlenderBot draft response of length: {}", aiResponse.length());

                // Two-step quality loop: draft -> critique/refine.
                String refinedResponse = postProcessForNaturalness(aiResponse, context);
                if (!isResponseQuality(refinedResponse)) {
                    String retryPrompt = (isMalay ? "[Respond in Malay with empathy and one actionable step] " : "") +
                        "User said: " + userMessage + "\n" +
                        "Your previous response was unclear. Write a warmer, clearer reply in 3-5 sentences with one practical next step.";
                    String secondPass = huggingFaceClient.generateReply(retryPrompt, context.getUserId().toString());
                    if (secondPass != null && !secondPass.isBlank()) {
                        refinedResponse = postProcessForNaturalness(secondPass, context);
                    }
                }
                
                // If AI didn't respond in Malay, provide a fallback
                if (isMalay && !containsMalayWords(refinedResponse)) {
                    return getMalayFallbackResponse(context.getCurrentEmotion());
                }
                
                return refinedResponse;
            } else {
                logger.warn("BlenderBot returned empty response");
                return isMalay ? getMalayFallbackResponse(context.getCurrentEmotion()) : null;
            }

        } catch (Exception e) {
            logger.error("Error generating BlenderBot response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if response contains Malay words
     */
    private boolean containsMalayWords(String text) {
        String[] malayIndicators = {"saya", "anda", "kamu", "ini", "itu", "yang", "dan", "untuk", "dengan", "tidak", "boleh", "mahu"};
        String lowerText = text.toLowerCase();
        for (String word : malayIndicators) {
            if (lowerText.contains(word)) return true;
        }
        return false;
    }

    /**
     * Get Malay fallback response based on emotion
     */
    private String getMalayFallbackResponse(String emotion) {
        switch (emotion) {
            case "stress":
                return "Saya faham anda sedang menghadapi tekanan. Ia perkara biasa untuk berasa begini. Apakah yang sedang membebani fikiran anda sekarang?";
            case "anxiety":
                return "Perasaan cemas yang anda alami itu sah dan penting. Saya di sini untuk mendengar. Boleh anda kongsikan apa yang membuatkan anda berasa begini?";
            case "sadness":
                return "Saya minta maaf anda berasa begini. Kesedihan boleh sangat berat untuk ditanggung. Saya di sini untuk menyokong anda.";
            case "anger":
                return "Saya faham kekecewaan anda. Perasaan marah itu sah. Apakah yang berlaku yang membuatkan anda berasa begini?";
            case "exhaustion":
                return "Berasa letih itu sangat meletihkan. Anda berhak mendapat rehat dan sokongan. Bagaimana saya boleh membantu?";
            case "loneliness":
                return "Berasa keseorangan itu sangat sukar. Sila ketahui bahawa anda penting dan saya di sini untuk anda.";
            default:
                return "Terima kasih kerana berkongsi dengan saya. Saya di sini untuk mendengar dan menyokong anda. Bagaimana perasaan anda sekarang?";
        }
    }

    /**
     * Build sophisticated system prompt that guides AI behavior
     */
    private String buildSystemPrompt(MonitoringContext context, ApproachType approach) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a compassionate mental health support companion. ");
        prompt.append("Your role is to provide empathetic, supportive, and practical guidance.\n\n");

        // Approach-specific behavior
        if (approach == ApproachType.EMPATHY) {
            prompt.append("APPROACH: Empathy Mode\n");
            prompt.append("- Deeply understand the person's feelings and perspective\n");
            prompt.append("- Use phrases like 'I understand', 'That makes sense', 'I can see why'\n");
            prompt.append("- Validate their emotions before offering solutions\n");
            prompt.append("- Show genuine care and connection\n\n");
        } else {
            prompt.append("APPROACH: Sympathy Mode\n");
            prompt.append("- Show compassion and concern for their situation\n");
            prompt.append("- Use supportive language like 'I'm here for you', 'You're not alone'\n");
            prompt.append("- Focus on practical support and resources\n");
            prompt.append("- Be warm but slightly more solution-focused\n\n");
        }

        // Emotion-specific guidance
        String emotionGuidance = getEmotionSpecificGuidance(context.getCurrentEmotion());
        prompt.append(emotionGuidance).append("\n\n");

        // Intensity-specific guidance
        String intensityGuidance = getIntensitySpecificGuidance(context.getCurrentIntensityScore());
        prompt.append(intensityGuidance).append("\n\n");

        // General guidelines
        prompt.append("IMPORTANT GUIDELINES:\n");
        prompt.append("1. Keep responses conversational and warm\n");
        prompt.append("2. Use natural language, not robotic text\n");
        prompt.append("3. Include specific acknowledgments of what they shared\n");
        prompt.append("4. Balance validation with practical suggestions\n");
        prompt.append("5. Ask thoughtful follow-up questions when appropriate\n");
        prompt.append("6. Never pretend to be a doctor - be supportive, not clinical\n");
        prompt.append("7. Use personal pronouns (I, you, we) to feel more human\n");
        prompt.append("8. Include small empathetic touches (pauses, acknowledgments)\n");

        return prompt.toString();
    }

    /**
     * Get emotion-specific guidance for the AI
     */
    private String getEmotionSpecificGuidance(String emotion) {
        switch (emotion != null ? emotion.toLowerCase() : "") {
            case "anxiety":
                return "EMOTION CONTEXT: User experiencing anxiety\n" +
                       "- Acknowledge their worry and racing thoughts\n" +
                       "- Offer grounding techniques (breathing, 5 senses)\n" +
                       "- Remind them anxiety is temporary\n" +
                       "- Suggest one small action they can take";

            case "depression":
                return "EMOTION CONTEXT: User experiencing depression\n" +
                       "- Validate their feelings of hopelessness\n" +
                       "- Gently encourage small positive steps\n" +
                       "- Emphasize they're not alone in this\n" +
                       "- Focus on self-compassion";

            case "stress":
                return "EMOTION CONTEXT: User experiencing stress\n" +
                       "- Acknowledge the weight they're carrying\n" +
                       "- Help them identify key stressors\n" +
                       "- Suggest practical stress management\n" +
                       "- Remind them stress is manageable";

            case "loneliness":
                return "EMOTION CONTEXT: User feeling lonely\n" +
                       "- Affirm that loneliness is a valid emotion\n" +
                       "- Encourage connection (however small)\n" +
                       "- Suggest meaningful activities\n" +
                       "- Remind them of their worth and value";

            case "anger":
                return "EMOTION CONTEXT: User experiencing anger\n" +
                       "- Validate their feelings without judgment\n" +
                       "- Help them channel anger constructively\n" +
                       "- Suggest healthy outlets (exercise, journaling)\n" +
                       "- Explore underlying feelings";

            case "confusion":
                return "EMOTION CONTEXT: User feeling confused\n" +
                       "- Acknowledge confusion is normal\n" +
                       "- Help them organize thoughts\n" +
                       "- Ask clarifying questions\n" +
                       "- Provide clarity and direction";

            case "joy":
                return "EMOTION CONTEXT: User experiencing joy\n" +
                       "- Celebrate with them genuinely\n" +
                       "- Help them savor the moment\n" +
                       "- Encourage sharing the joy\n" +
                       "- Build on their positive momentum";

            case "grief":
                return "EMOTION CONTEXT: User experiencing grief\n" +
                       "- Honor their loss and pain\n" +
                       "- Validate all grief emotions\n" +
                       "- Encourage processing at their pace\n" +
                       "- Offer gentle support";

            default:
                return "EMOTION CONTEXT: General emotional support\n" +
                       "- Be present and attentive\n" +
                       "- Validate their feelings\n" +
                       "- Offer practical support";
        }
    }

    /**
     * Get intensity-specific guidance for the AI
     */
    private String getIntensitySpecificGuidance(int intensityScore) {
        IntensityLevel level = IntensityLevel.fromScore(intensityScore);

        switch (level) {
            case LOW:
                return "INTENSITY LEVEL: Low (1-4) - Mild concern\n" +
                       "- Approach is conversational and supportive\n" +
                       "- Focus on prevention and wellness\n" +
                       "- Share practical tips and strategies\n" +
                       "- Encourage positive coping mechanisms";

            case MODERATE:
                return "INTENSITY LEVEL: Moderate (5-7) - Significant concern\n" +
                       "- Approach is more focused and supportive\n" +
                       "- Validate the significance of their experience\n" +
                       "- Provide concrete coping strategies\n" +
                       "- Encourage professional support if needed";

            case HIGH:
                return "INTENSITY LEVEL: High (8-10) - Severe distress\n" +
                       "- Approach is caring, urgent, and supportive\n" +
                       "- Prioritize safety and immediate support\n" +
                       "- Provide crisis resources if necessary\n" +
                       "- Encourage immediate professional help\n" +
                       "- Be warm but also direct about support needed";

            default:
                return "INTENSITY LEVEL: Unknown\n" +
                       "- Maintain supportive approach";
        }
    }

    /**
     * Build user prompt with conversation context
     */
    private String buildUserPrompt(String userMessage, MonitoringContext context) {
        StringBuilder prompt = new StringBuilder();

        // Add conversation context if available
        if (context.getRecentUserMessages() != null && !context.getRecentUserMessages().isEmpty()) {
            int historySize = Math.min(3, context.getRecentUserMessages().size());
            List<String> recentHistory = context.getRecentUserMessages()
                .stream()
                .skip(Math.max(0, context.getRecentUserMessages().size() - historySize))
                .collect(Collectors.toList());

            if (!recentHistory.isEmpty()) {
                prompt.append("CONVERSATION CONTEXT:\n");
                for (String msg : recentHistory) {
                    prompt.append("- ").append(msg).append("\n");
                }
                prompt.append("\n");
            }
        }

        // Current message
        prompt.append("USER MESSAGE: ").append(userMessage).append("\n\n");

        // Instructions for response
        prompt.append("Please provide a warm, empathetic response that:\n");
        prompt.append("1. Acknowledges what they shared\n");
        prompt.append("2. Validates their feelings\n");
        prompt.append("3. Offers supportive guidance or suggestions\n");
        prompt.append("4. Feels natural and conversational (like a caring friend)\n");
        prompt.append("5. Is 2-4 sentences or a short paragraph\n");
        prompt.append("6. Ends with a gentle question or invitation to continue");

        return prompt.toString();
    }

    /**
     * Post-process AI response for maximum naturalness
     */
    private String postProcessForNaturalness(String aiResponse, MonitoringContext context) {
        if (aiResponse == null || aiResponse.isEmpty()) {
            return "";
        }

        String processed = aiResponse;

        // Remove AI model artifacts
        processed = processed.replaceAll("(?i)(^|\\n)(assistant|ai|bot):\\s*", "");
        processed = processed.replaceAll("(?i)(^|\\n)(human|user):\\s*", "");

        // Remove excessive quotation marks
        processed = processed.replaceAll("^\"|\"$", "");

        // Remove common AI prefixes
        processed = processed.replaceAll("(?i)^(here's|here is|i understand|i hear you|i can see).*?:\\s*", "");

        // Trim whitespace
        processed = processed.trim();

        // Format lists with proper spacing
        processed = formatListWithSpacing(processed);

        // Add human touches if missing
        if (!processed.contains("?")) {
            // Add a subtle ending question
            processed = addSubtleQuestion(processed, context.getCurrentEmotion());
        }

        // Ensure proper capitalization
        if (!processed.isEmpty()) {
            processed = Character.toUpperCase(processed.charAt(0)) + processed.substring(1);
        }

        // Limit length while preserving meaning
        if (processed.length() > 800) {
            processed = processed.substring(0, 797) + "...";
        }

        // Add human-like pauses if response is very short
        if (processed.length() < 100) {
            processed = addHumanTouches(processed);
        }

        logger.debug("Post-processed response length: {}", processed.length());
        return processed;
    }

    /**
     * Add subtle ending question for engagement
     */
    private String addSubtleQuestion(String response, String emotion) {
        String[] questions = {
            " How are you feeling about this?",
            " What feels most pressing to you right now?",
            " Would you like to talk more about this?",
            " Is there anything specific you'd like to address?",
            " How can I best support you?",
            " What would help you most right now?",
            " How does that resonate with you?"
        };

        // Choose question based on emotion
        String question = questions[(emotion != null ? emotion.hashCode() : 0) % questions.length];
        return response + question;
    }

    /**
     * Add human-like touches to very short responses
     */
    private String addHumanTouches(String response) {
        // Add natural flow
        if (!response.endsWith(".") && !response.endsWith("?") && !response.endsWith("!")) {
            response += ".";
        }

        return response;
    }

    /**
     * Format lists with proper spacing for better readability
     */
    private String formatListWithSpacing(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        // First, normalize spacing and line breaks
        response = response.trim();

        // Add newlines after periods that end sentences, but are followed by numbered items
        // e.g., "sentence. 1. Next item" -> "sentence.\n1. Next item"
        response = response.replaceAll("([.!?])\\s+([0-9]+\\.)\\s+", "$1\n$2 ");

        // Add newlines before numbered items that don't already have them
        // Matches: "text1. item" -> "text\n1. item"
        response = response.replaceAll("([^\\n])\\s+([0-9]+\\.)\\s+([^\\n])", "$1\n$2 $3");

        // Add newlines before bullet points (•, -, *, •)
        response = response.replaceAll("([^\\n])\\s+([•\\-\\*])\\s+", "$1\n$2 ");

        // Add newlines before bolded list items (**1. text** or **- text**)
        response = response.replaceAll("([^\\n])\\s+(\\*\\*[0-9]+\\.|\\*\\*[•\\-\\*])", "$1\n$2");

        // Ensure proper spacing after items (add newline before next sentence if it doesn't start with list marker)
        response = response.replaceAll("([0-9]+\\.)\\s+([^0-9•\\-\\*\\n][^.!?]*\\.)\\s+([A-Z])", "$1 $2\n$3");

        return response;
    }

    /**
     * Validate response quality
     */
    public boolean isResponseQuality(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }

        // Check length
        if (response.length() < 20 || response.length() > 1500) {
            return false;
        }

        // Check for human-like qualities
        boolean hasPronouns = response.matches("(?i).*(i|you|we|me|us).*");
        boolean hasQuestion = response.contains("?");
        boolean hasEmotionalContent = response.matches("(?i).*(understand|feel|hear|see|know).*");

        // At least 2 out of 3 human-like qualities
        int humanQualityScore = 0;
        if (hasPronouns) humanQualityScore++;
        if (hasQuestion) humanQualityScore++;
        if (hasEmotionalContent) humanQualityScore++;

        return humanQualityScore >= 2;
    }
}

