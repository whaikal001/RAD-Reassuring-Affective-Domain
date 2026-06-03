package com.radai.service;

import com.radai.chat.hf.HuggingFaceClient;
import com.radai.model.FlowResponse;
import com.radai.model.MonitoringContext;
import com.radai.enums.PathwayType;
import com.radai.enums.ApproachType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integration service combining Mental Health Flow System with HuggingFace AI
 * 
 * Flow: User Input
 *   → Greeting (first message)
 *   → Emotional Screening (detect emotion, intensity, pathway)
 *   → AI Response (main empathetic content)
 *   → Strategies (when appropriate)
 *   → Follow-up (engagement questions)
 *   → User
 */
public class FlowWithAIService {
    private static final Logger logger = LoggerFactory.getLogger(FlowWithAIService.class);

    private final ChatbotFlowEngine flowEngine;
    private final HuggingFaceResponseEnhancer aiEnhancer;
    private final MonitoringAndScreeningService screeningService;
    private final ResponseTemplateService templateService;
    private final boolean useAI;
    private final com.radai.service.empathy.ApproachSwitchPolicy approachPolicy =
        new com.radai.service.empathy.ApproachSwitchPolicy();
    
    // Track conversation state per user
    private final Map<UUID, ConversationState> conversationStates = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> userGoals = new ConcurrentHashMap<>();
    
    private static class ConversationState {
        int messageCount = 0;
        String detectedEmotion = "neutral";
        int lastIntensity = 5;
        boolean greetingSent = false;
        Deque<String> recentUserMessages = new ArrayDeque<>();
        String memorySummary = "";
        String lastSuccessfulMode = "ai-primary";
        int lowStateStreak = 0;
        int highStateStreak = 0;
    }

    private record RiskAssessment(String level, boolean crisis, double score, List<String> reasons) {}

    private static final Map<String, List<String>> KNOWLEDGE_SNIPPETS = Map.of(
        "anxiety", List.of(
            "Use a 4-6 breathing cycle: inhale for 4 seconds, exhale for 6 seconds for 2 minutes.",
            "Try the 5-4-3-2-1 grounding exercise to return attention to the present moment.",
            "Reduce uncertainty by writing one immediate next step instead of solving everything at once."
        ),
        "stress", List.of(
            "Break tasks into one 10-minute action and one optional follow-up action.",
            "Name what is controllable today versus what is not controllable today.",
            "Use a short body reset: drink water, unclench jaw, drop shoulders, then restart."
        ),
        "sadness", List.of(
            "Use gentle activation: one shower, one meal, one short walk.",
            "Message one trusted person with a simple check-in instead of waiting to feel ready.",
            "Lower expectations for today and prioritize rest plus one meaningful action."
        ),
        "anger", List.of(
            "Pause before replying and label the trigger in one sentence.",
            "Use a physical reset first: walk, stretch, or cold water on face.",
            "Convert anger into a boundary request using calm, concrete wording."
        ),
        "loneliness", List.of(
            "Schedule one low-pressure connection today: text, short call, or shared activity.",
            "Use community touchpoints like study groups, clubs, or volunteering.",
            "Start with consistency over depth when rebuilding social energy."
        ),
        "neutral", List.of(
            "Track sleep, hydration, and movement to prevent emotional dips.",
            "Use weekly reflection: what helped, what drained, what to repeat.",
            "Build one small routine that protects your energy daily."
        )
    );

    private static final List<String> CRISIS_LINES = List.of(
        "I am really glad you told me this. Your safety matters right now.",
        "If you might hurt yourself, please call emergency services now and move to a safer place with someone you trust.",
        "If available in your region, contact your local crisis hotline immediately for urgent support."
    );

    private static final String FALLBACK_RESPONSE =
        "Thank you for sharing this with me. I am here with you. Let's take one slow breath together, and then tell me the one thing that feels heaviest right now.";

    private static final String[] GOAL_TRIGGERS = new String[] {
        "goal:", "my goal", "i want to improve", "i want to work on", "target"
    };

    private static final String[] POSITIVE_OUTCOME_TRIGGERS = new String[] {
        "that helped", "this helped", "worked for me", "i feel better", "i am calmer"
    };

    private static final String[] CRISIS_TRIGGERS = new String[] {
        "kill myself", "end my life", "suicide", "want to die", "no reason to live", "hurt myself",
        "nak mati", "mahu mati", "nak bunuh diri", "bunuh diri", "tak mahu hidup", "tak ingin hidup"
    };

    private static final String[] HIGH_RISK_TRIGGERS = new String[] {
        "hopeless", "cannot go on", "can't go on", "worthless", "panic attack", "emergency"
    };

    private static final int MAX_MEMORY_MESSAGES = 12;

    private enum PersonalizationState {
        STABILIZE,
        SUPPORT,
        COACH
    }

    public FlowWithAIService(ChatbotFlowEngine flowEngine, 
                            HuggingFaceClient huggingFaceClient,
                            boolean useAI) {
        this.flowEngine = flowEngine;
        this.aiEnhancer = new HuggingFaceResponseEnhancer(huggingFaceClient);
        this.screeningService = new MonitoringAndScreeningService();
        this.templateService = new ResponseTemplateService();
        this.useAI = useAI;
    }

    /**
     * Process message with integrated Flow System + AI
     * 
     * @param userId User ID
     * @param conversationId Conversation ID
     * @param userMessage User's message
     * @param intensityScore Emotional intensity (1-10)
     * @return Enhanced FlowResponse with greeting, AI content, strategies, follow-up
     */
    public FlowResponse processWithAI(UUID userId, String conversationId, String userMessage, int intensityScore) {
        return processWithAI(userId, conversationId, userMessage, intensityScore, "en");
    }

    /**
     * Process message with integrated Flow System + AI (with language support)
     */
    public FlowResponse processWithAI(UUID userId, String conversationId, String userMessage, int intensityScore, String language) {
        return processWithAI(userId, conversationId, userMessage, intensityScore, language, null);
    }

    /**
     * Process message with integrated Flow System + AI, supplying the DASS screening band as the
     * stable baseline for the empathy&lt;-&gt;sympathy switch. {@code dassBand} may be null.
     */
    public FlowResponse processWithAI(UUID userId, String conversationId, String userMessage, int intensityScore, String language, String dassBand) {
        Instant started = Instant.now();
        try {
            logger.info("Processing message with integrated Flow+AI for user {}", userId);

            // Get or create conversation state
            ConversationState state = conversationStates.computeIfAbsent(userId, k -> new ConversationState());
            state.messageCount++;
            trackMemory(state, userMessage);
            captureGoal(userId, userMessage);

            // Get or create monitoring context
            MonitoringContext context = flowEngine.getContext(userId);
            if (context == null) {
                context = new MonitoringContext(userId, conversationId);
            }

            // Capture the screening band once as the session baseline for approach switching.
            if (dassBand != null && !dassBand.isBlank() && context.getStressBand() == null) {
                context.setStressBand(dassBand);
            }

            // Step 1: Emotional screening
            screeningService.assessEmotionalState(context, userMessage, intensityScore);
            String detectedEmotion = context.getCurrentEmotion();
            PathwayType pathway = screeningService.determinePathway(context);
            ApproachType approach = determineApproach(context, state.messageCount <= 1);
            
            state.detectedEmotion = detectedEmotion;
            state.lastIntensity = intensityScore;

            RiskAssessment risk = assessRisk(userMessage, intensityScore, context);
            if (risk.crisis()) {
                FlowResponse crisis = buildCrisisResponse(conversationId, language, detectedEmotion, state, risk);
                crisis.withMetadata("responseLatencyMs", Duration.between(started, Instant.now()).toMillis());
                return crisis;
            }
            
            logger.info("Screening: emotion={}, intensity={}, pathway={}", detectedEmotion, intensityScore, pathway);
            
            FlowResponse response = new FlowResponse(conversationId);
            
            // Step 2: Add greeting (first message only)
            if (!state.greetingSent) {
                String greeting = GreetingService.generateTimeBasedGreeting(language);
                response.withGreeting(greeting);
                state.greetingSent = true;
            }

            String memorySummary = buildMemorySummary(state, detectedEmotion, intensityScore);
            List<String> snippets = retrieveKnowledgeSnippets(detectedEmotion, context.getDominantStressor());
            response.withAssessment(buildAssessmentLine(language, memorySummary, snippets));
            
            // Step 3: Get AI response for main content. Force AI usage with retries so templates are not used.
            String aiResponse = null;
            String responseMode = "ai-primary";
            if (useAI) {
                aiResponse = aiEnhancer.generateHumanLikeResponse(userMessage, context, approach, language);
                if (aiResponse == null || aiResponse.isBlank()) {
                    logger.warn("Initial AI response empty. Retrying with forced SYMPATHY prompts to ensure AI reply is returned.");
                    // Try a few stronger retries forcing SYMPATHY approach and clearer instruction
                    for (int attempt = 1; attempt <= 3 && (aiResponse == null || aiResponse.isBlank()); attempt++) {
                        String forcedInstruction = "[FORCE_SYMPATHY] Please reply with a compassionate, sympathetic tone in 3-5 short sentences. ";
                        String forcedMsg = forcedInstruction + userMessage;
                        try {
                            aiResponse = aiEnhancer.generateHumanLikeResponse(forcedMsg, context, ApproachType.SYMPATHY, language);
                        } catch (Exception ex) {
                            logger.warn("AI retry attempt {} threw exception: {}", attempt, ex.getMessage());
                        }
                        if (aiResponse != null && !aiResponse.isBlank()) {
                            logger.info("AI retry succeeded on attempt {}", attempt);
                            break;
                        } else {
                            logger.debug("AI retry attempt {} produced empty response", attempt);
                        }
                    }
                } else {
                    responseMode = "ai-primary";
                }
                if (aiResponse != null && !aiResponse.isBlank()) {
                    responseMode = "ai-primary";
                } else {
                    // keep ai-primary but mark that retries failed; will attempt final forced prompt below
                    responseMode = "ai-failed-retries";
                }
            }
            if (aiResponse != null && !aiResponse.isEmpty()) {
                String refined = refineResponseQuality(aiResponse, detectedEmotion, intensityScore, language);
                response.withMainContent(applySafetyPostCheck(refined, risk, language));
                state.lastSuccessfulMode = "ai-primary";
            } else {
                // As a last resort, attempt one final AI call with a very explicit system-style instruction
                logger.warn("AI still empty after retries; attempting one final forced AI call before using template fallback");
                try {
                    String finalForced = "[FINAL_FORCE_SYMPATHY] Respond sympathetically and directly: provide heartfelt support in 3 sentences and one immediate practical step." + userMessage;
                    aiResponse = aiEnhancer.generateHumanLikeResponse(finalForced, context, ApproachType.SYMPATHY, language);
                } catch (Exception ex) {
                    logger.error("Final forced AI call failed: {}", ex.getMessage());
                }

                if (aiResponse != null && !aiResponse.isBlank()) {
                    String refined = refineResponseQuality(aiResponse, detectedEmotion, intensityScore, language);
                    response.withMainContent(applySafetyPostCheck(refined, risk, language));
                    state.lastSuccessfulMode = "ai-primary";
                    responseMode = "ai-primary";
                } else {
                    // If AI fails completely, use static safe fallback
                    response.withMainContent(FALLBACK_RESPONSE);
                    responseMode = "static-fallback";
                    state.lastSuccessfulMode = "static-fallback";
                    logger.warn("AI failed after retries, using static safe response for user {}", userId);
                }
            }
            
            // Step 4: Add strategies for moderate/high intensity
            if (intensityScore >= 6 || pathway == PathwayType.INTERVENTION) {
                response.withStrategies(buildAdaptiveStrategies(context, snippets, language, userId));
            }
            
            // Step 5: Add follow-up question (every 2-3 messages or when appropriate)
            PersonalizationState personalizationState = decidePersonalizationState(state, intensityScore, risk);
            if (state.messageCount % 2 == 0 || intensityScore >= 7) {
                String followUp = generatePersonalizedFollowUp(context, detectedEmotion, language, personalizationState, userId, state.messageCount);
                response.withFollowUp(followUp);
            }

            if (hasPositiveSignal(userMessage)) {
                response.withMetadata("copingSignal", "positive");
            }
            
            // Step 6: Set metadata
            response.withPathway(pathway)
                    .withApproach(approach)
                    .withIntensity(intensityScore)
                    .withEmotion(detectedEmotion)
                    .withCycleNumber(state.messageCount)
                    .withLoopContinuation(true)
                    .withSessionEnd(false, null)
                    .withSessionDuration(context.getSessionDurationMinutes());

            response.withMetadata("safetyLevel", risk.level())
                    .withMetadata("riskScore", risk.score())
                    .withMetadata("riskReasons", risk.reasons())
                    .withMetadata("memorySummary", memorySummary)
                    .withMetadata("retrievalSnippets", snippets)
                    .withMetadata("personalizationState", personalizationState.name())
                    .withMetadata("activeGoals", getGoals(userId))
                    .withMetadata("responseMode", responseMode)
                    .withMetadata("responseLatencyMs", Duration.between(started, Instant.now()).toMillis());
            
            logger.info("Integrated response generated for user {} - emotion: {}, cycle: {}", 
                       userId, detectedEmotion, state.messageCount);
            return response;

        } catch (Exception e) {
            logger.error("Error in integrated processing: {}", e.getMessage(), e);
            return new FlowResponse(conversationId)
                .withMainContent("I'm sorry, I encountered an issue. Could you try telling me again what's on your mind?")
                .withLoopContinuation(true);
        }
    }

    public List<String> getGoals(UUID userId) {
        return userGoals.getOrDefault(userId, Collections.emptyList());
    }

    public List<String> addGoal(UUID userId, String goal) {
        if (goal == null || goal.isBlank()) {
            return getGoals(userId);
        }

        List<String> goals = userGoals.computeIfAbsent(userId, key -> new ArrayList<>());
        String normalized = goal.trim();
        if (!goals.contains(normalized)) {
            goals.add(normalized);
        }
        return goals;
    }

    public void clearGoals(UUID userId) {
        userGoals.remove(userId);
    }
    
    /**
     * Determine approach (EMPATHY vs SYMPATHY) for this turn from the stress/intensity signal.
     *
     * <p>Shares {@link com.radai.service.empathy.ApproachSwitchPolicy} with the flow engine:
     * HIGH stress/intensity &rarr; SYMPATHY, LOW &rarr; EMPATHY, with hysteresis. The session opens
     * in EMPATHY and any detected crisis forces SYMPATHY.
     */
    private ApproachType determineApproach(MonitoringContext context, boolean firstTurn) {
        boolean crisis = context.isCrisisDetected() || context.isSuicidalIdeationDetected();

        com.radai.service.empathy.ApproachSwitchPolicy.Decision decision = approachPolicy.decide(
            context.getCurrentApproach(), firstTurn, crisis,
            context.getStressBand(), context.getCurrentIntensityScore());

        if (decision.switched()) {
            logger.info("Approach switch {} -> {} | {}",
                context.getCurrentApproach(), decision.approach(), decision.reason());
        } else {
            logger.debug("Approach kept {} | {}", decision.approach(), decision.reason());
        }
        return decision.approach();
    }
    
    /**
     * Generate contextual follow-up questions based on emotion
     */
    private String generateContextualFollowUp(MonitoringContext context, String emotion, String language) {
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
        
        switch (emotion) {
            case "stress":
                return isMalay ? "Apakah satu perkara kecil yang mungkin dapat membantu mengurangkan tekanan itu sekarang?" 
                              : "What's one small thing that might help reduce some of that pressure right now?";
            case "anxiety":
                return isMalay ? "Adakah sesuatu yang khusus membuatkan anda berasa cemas, atau ia lebih kepada perasaan umum?"
                              : "Is there something specific that's making you feel anxious, or is it more of a general feeling?";
            case "sadness":
                return isMalay ? "Adakah anda ingin bercakap lebih lanjut tentang apa yang membuatkan anda berasa begini?"
                              : "Would you like to talk more about what's been making you feel this way?";
            case "anger":
                return isMalay ? "Pada pendapat anda, apakah yang mencetuskan perasaan kekecewaan ini?"
                              : "What do you think triggered these feelings of frustration?";
            case "exhaustion":
                return isMalay ? "Bilakah kali terakhir anda dapat berehat dengan betul?"
                              : "When was the last time you were able to rest properly?";
            case "loneliness":
                return isMalay ? "Adakah seseorang yang anda percayai yang boleh anda hubungi?"
                              : "Is there someone you trust that you could reach out to?";
            default:
                return isMalay ? "Bagaimana lagi saya boleh menyokong anda hari ini?"
                              : "How else can I support you today?";
        }
    }

    /**
     * Get monitoring context (for state inspection)
     */
    public MonitoringContext getContext(UUID userId) {
        return flowEngine.getContext(userId);
    }

    /**
     * End conversation
     */
    public void endConversation(UUID userId, String conversationId) {
        flowEngine.endConversation(userId, conversationId);
    }

    /**
     * Reset active in-memory session state for a user.
     */
    public void resetUserSession(UUID userId) {
        conversationStates.remove(userId);
        flowEngine.clearUserContext(userId);
        clearGoals(userId);
    }

    /**
     * Check if AI is enabled
     */
    public boolean isAIEnabled() {
        return useAI;
    }

    /**
     * Temporarily disable/enable AI
     */
    public void setAIEnabled(boolean enabled) {
        logger.info("AI mode set to: {}", enabled);
    }

    private void trackMemory(ConversationState state, String userMessage) {
        state.recentUserMessages.addLast(userMessage);
        while (state.recentUserMessages.size() > MAX_MEMORY_MESSAGES) {
            state.recentUserMessages.removeFirst();
        }
    }

    private String buildMemorySummary(ConversationState state, String emotion, int intensity) {
        List<String> sampled = new ArrayList<>();
        int index = 0;
        for (String message : state.recentUserMessages) {
            if (index % 3 == 0) {
                sampled.add(message.length() > 72 ? message.substring(0, 72) + "..." : message);
            }
            index++;
        }

        String summary = "Recent pattern: emotion=" + emotion + ", intensity=" + intensity +
            ", turns=" + state.messageCount + ", notes=" + String.join(" | ", sampled);
        state.memorySummary = summary;
        return summary;
    }

    private RiskAssessment assessRisk(String userMessage, int intensity, MonitoringContext context) {
        String lower = userMessage == null ? "" : userMessage.toLowerCase();
        double score = 0.0;
        List<String> reasons = new ArrayList<>();

        for (String trigger : CRISIS_TRIGGERS) {
            if (lower.contains(trigger)) {
                score += 0.7;
                reasons.add("crisis_trigger:" + trigger);
            }
        }

        for (String trigger : HIGH_RISK_TRIGGERS) {
            if (lower.contains(trigger)) {
                score += 0.2;
                reasons.add("high_risk_trigger:" + trigger);
            }
        }

        if (intensity >= 8) {
            score += 0.2;
            reasons.add("high_intensity");
        }
        if (context.isSuicidalIdeationDetected() || context.isCrisisDetected()) {
            score += 0.4;
            reasons.add("context_crisis");
        }

        if (lower.matches(".*(want to die|end my life|kill myself|suicide|nak mati|mahu mati|nak bunuh diri|bunuh diri|tak mahu hidup|tak ingin hidup).*")) {
            score = Math.max(score, 0.9);
            reasons.add("matched_crisis_language");
        }

        if (score >= 0.8) {
            return new RiskAssessment("critical", true, Math.min(1.0, score), reasons);
        }
        if (score >= 0.45) {
            return new RiskAssessment("high", false, Math.min(1.0, score), reasons);
        }
        if (score >= 0.2) {
            return new RiskAssessment("moderate", false, Math.min(1.0, score), reasons);
        }
        return new RiskAssessment("low", false, Math.min(1.0, score), reasons);
    }

    private FlowResponse buildCrisisResponse(String conversationId, String language, String detectedEmotion,
                                             ConversationState state, RiskAssessment risk) {
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
        String main = isMalay
            ? "Terima kasih kerana berkongsi perkara ini. Keselamatan anda sangat penting sekarang. Jika anda mungkin mencederakan diri, sila hubungi talian kecemasan tempatan dengan segera dan dapatkan seseorang yang dipercayai untuk berada bersama anda sekarang."
            : String.join(" ", CRISIS_LINES);

        String followUp = isMalay
            ? "Boleh anda beritahu saya sama ada anda berada di tempat yang selamat sekarang?"
            : "Can you tell me if you are in a safe place right now?";

        return new FlowResponse(conversationId)
            .withMainContent(main)
            .withFollowUp(followUp)
            .withEmotion(detectedEmotion)
            .withIntensity(Math.max(9, state.lastIntensity))
            .withPathway(PathwayType.INTERVENTION)
            .withApproach(ApproachType.SYMPATHY)
            .withMetadata("safetyLevel", risk.level())
            .withMetadata("riskScore", risk.score())
            .withMetadata("riskReasons", risk.reasons())
            .withMetadata("responseMode", "crisis-protocol");
    }

    private List<String> retrieveKnowledgeSnippets(String emotion, String stressor) {
        List<String> emotionSnippets = KNOWLEDGE_SNIPPETS.getOrDefault(emotion, KNOWLEDGE_SNIPPETS.get("neutral"));
        List<String> snippets = new ArrayList<>(emotionSnippets.subList(0, Math.min(2, emotionSnippets.size())));

        if (stressor != null && !stressor.isBlank()) {
            snippets.add("Current stressor focus: " + stressor + ". Start with one small actionable step.");
        }
        return snippets;
    }

    private String buildAssessmentLine(String language, String memorySummary, List<String> snippets) {
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
        if (isMalay) {
            return "Ringkasan konteks: " + memorySummary + "\nCadangan teras: " + String.join(" ", snippets);
        }
        return "Context snapshot: " + memorySummary + "\nCore suggestions: " + String.join(" ", snippets);
    }

    private String refineResponseQuality(String aiResponse, String emotion, int intensity, String language) {
        String refined = aiResponse == null ? "" : aiResponse.trim();
        if (refined.isBlank()) {
            return FALLBACK_RESPONSE;
        }

        if (!refined.matches(".*(I hear|I understand|That sounds|I can see|Thank you for sharing).*")) {
            boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
            String prefix = isMalay ? "Terima kasih kerana berkongsi. " : "Thank you for sharing that. ";
            refined = prefix + refined;
        }

        if (!refined.contains("?") && intensity >= 6) {
            boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
            refined += isMalay
                ? " Apakah satu langkah kecil yang paling mungkin anda boleh cuba sekarang?"
                : " What is one small step that feels doable right now?";
        }

        if (refined.length() > 900) {
            refined = refined.substring(0, 900).trim();
        }
        return refined;
    }

    private String applySafetyPostCheck(String response, RiskAssessment risk, String language) {
        if (risk.level().equals("high") || risk.level().equals("critical")) {
            boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
            String guard = isMalay
                ? " Keselamatan anda paling penting. Jika anda berasa berisiko, sila hubungi bantuan kecemasan sekarang."
                : " Your safety is the most important thing. If you feel at risk, please contact emergency support now.";
            return response + guard;
        }
        return response;
    }

    private String buildAdaptiveStrategies(MonitoringContext context, List<String> snippets, String language, UUID userId) {
        StringBuilder strategies = new StringBuilder();
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
        String intro = isMalay ? "Langkah yang boleh anda cuba sekarang:" : "Try these practical steps now:";
        strategies.append(intro).append("\n");
        for (String snippet : snippets) {
            strategies.append("- ").append(snippet).append("\n");
        }

        List<String> goals = getGoals(userId);
        if (!goals.isEmpty()) {
            strategies.append(isMalay ? "- Selaraskan langkah dengan matlamat: " : "- Align this with your goal: ")
                .append(goals.get(goals.size() - 1)).append("\n");
        }

        // Replace template-based strategies with AI-generated suggestions for consistency
        try {
            String aiStrategiesPrompt = "Provide 3 concise, practical steps tailored to the user's current situation. "
                + "Keep each step short and actionable. Context: emotion=" + context.getCurrentEmotion()
                + ", stressor=" + (context.getDominantStressor() == null ? "none" : context.getDominantStressor())
                + ".";
            String aiStrategies = aiEnhancer.generateHumanLikeResponse(aiStrategiesPrompt, null, ApproachType.EMPATHY, language);
            if (aiStrategies != null && !aiStrategies.isBlank()) {
                strategies.append("\n").append(aiStrategies.trim());
            }
        } catch (Exception e) {
            logger.warn("AI strategies generation failed, keeping existing strategies: {}", e.getMessage());
        }

        return strategies.toString().trim();
    }

    private PersonalizationState decidePersonalizationState(ConversationState state, int intensity, RiskAssessment risk) {
        if (intensity >= 8 || "high".equals(risk.level()) || "critical".equals(risk.level())) {
            state.highStateStreak++;
            state.lowStateStreak = 0;
            return PersonalizationState.STABILIZE;
        }

        if (intensity <= 4) {
            state.lowStateStreak++;
            state.highStateStreak = 0;
            return PersonalizationState.COACH;
        }

        state.lowStateStreak = 0;
        state.highStateStreak = 0;
        return PersonalizationState.SUPPORT;
    }

    private String generatePersonalizedFollowUp(MonitoringContext context, String emotion, String language,
                                                PersonalizationState state, UUID userId, int cycle) {
        List<String> goals = getGoals(userId);
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");

        // Prefer AI-generated, context-aware follow-up questions
        try {
            StringBuilder followPrompt = new StringBuilder();
            followPrompt.append("Write one concise, empathetic follow-up question tailored to the user's context. ");
            followPrompt.append("Keep it natural and avoid canned phrasing. Context: emotion=").append(emotion)
                        .append(", cycle=").append(cycle).append(", personalization=").append(state.name()).append(".");
            if (!goals.isEmpty() && cycle % 4 == 0) {
                followPrompt.append(" The user has a goal: ").append(goals.get(goals.size() - 1)).append('.');
            }

            String aiFollow = aiEnhancer.generateHumanLikeResponse(followPrompt.toString(), null, ApproachType.EMPATHY, language);
            if (aiFollow != null && !aiFollow.isBlank()) {
                return aiFollow.trim();
            }
        } catch (Exception e) {
            logger.warn("AI follow-up generation failed, falling back to existing logic: {}", e.getMessage());
        }

        // Fallback to previous logic if AI fails
        if (!goals.isEmpty() && cycle % 4 == 0) {
            return isMalay
                ? "Bagaimana perkembangan kecil anda terhadap matlamat ini: " + goals.get(goals.size() - 1) + "?"
                : "How has your progress been on this goal: " + goals.get(goals.size() - 1) + "?";
        }

        return switch (state) {
            case STABILIZE -> isMalay
                ? "Bolehkah kita fokus pada satu langkah keselamatan yang boleh anda lakukan dalam 5 minit akan datang?"
                : "Can we focus on one safety step you can do in the next five minutes?";
            case COACH -> isMalay
                ? "Apakah tindakan kecil yang anda mahu komited selepas perbualan ini?"
                : "What small action do you want to commit to after this chat?";
            case SUPPORT -> generateContextualFollowUp(context, emotion, language);
        };
    }

    private void captureGoal(UUID userId, String userMessage) {
        if (userMessage == null) {
            return;
        }

        String lower = userMessage.toLowerCase();
        for (String trigger : GOAL_TRIGGERS) {
            if (lower.contains(trigger)) {
                String goal = userMessage;
                int idx = lower.indexOf(trigger);
                if (idx >= 0 && userMessage.length() > idx + trigger.length()) {
                    goal = userMessage.substring(idx + trigger.length()).trim();
                }
                addGoal(userId, goal);
                return;
            }
        }
    }

    private boolean hasPositiveSignal(String userMessage) {
        String lower = userMessage == null ? "" : userMessage.toLowerCase();
        for (String phrase : POSITIVE_OUTCOME_TRIGGERS) {
            if (lower.contains(phrase)) {
                return true;
            }
        }
        return false;
    }
}

