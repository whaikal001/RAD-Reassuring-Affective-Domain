package com.radai.service;

import com.radai.enums.ApproachType;
import com.radai.enums.PathwayType;
import com.radai.model.MonitoringContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates response templates based on approach type (Empathy/Sympathy) and pathway (Prevention/Intervention).
 * Uses Woebot-inspired templates for emotional support.
 */
public class ResponseTemplateService {

    /**
     * Generate opening based on approach type and emotion
     */
    public String generateOpening(MonitoringContext context, ApproachType approach) {
        String emotion = context.getCurrentEmotion();
        
        if (approach == ApproachType.SYMPATHY) {
            return generateSympathyOpening(emotion);
        } else {
            return generateEmpathyOpening(emotion);
        }
    }

    /**
     * Empathy opening: "I understand what you're going through..."
     */
    private String generateEmpathyOpening(String emotion) {
        Map<String, String> empathyOpenings = new HashMap<>();
        empathyOpenings.put("stress", "I can really sense that you're feeling overwhelmed right now. It's completely understandable given what you're experiencing.");
        empathyOpenings.put("anxiety", "I understand how anxiety can make everything feel impossible. Your worry is real and valid.");
        empathyOpenings.put("sadness", "I can feel the weight in what you're sharing. These feelings deserve space and compassion.");
        empathyOpenings.put("anger", "I hear your frustration. What you're feeling makes complete sense given the situation.");
        empathyOpenings.put("exhaustion", "I understand how depleted you must be feeling. Running on empty is incredibly hard.");
        empathyOpenings.put("loneliness", "I hear the loneliness in your words. Feeling alone is a painful experience.");
        empathyOpenings.put("joy", "I'm so glad to hear you're feeling good! That's wonderful.");
        empathyOpenings.put("neutral", "I understand where you're coming from. I'm here to listen and support you.");
        
        return empathyOpenings.getOrDefault(emotion, "I understand what you're sharing with me. You're not alone in this.");
    }

    /**
     * Sympathy opening: "I'm so sorry you're going through this..."
     */
    private String generateSympathyOpening(String emotion) {
        Map<String, String> sympathyOpenings = new HashMap<>();
        sympathyOpenings.put("stress", "I'm truly sorry you're dealing with so much stress. This must be incredibly hard on you.");
        sympathyOpenings.put("anxiety", "I'm so sorry you're experiencing this anxiety. You don't have to face this alone.");
        sympathyOpenings.put("sadness", "I'm deeply sorry you're going through this. Your pain matters, and I'm here for you.");
        sympathyOpenings.put("anger", "I'm sorry you're in this situation. Your anger is completely valid.");
        sympathyOpenings.put("exhaustion", "I'm sorry you're so exhausted. You deserve rest and support.");
        sympathyOpenings.put("loneliness", "I'm so sorry you're feeling this alone. Please know you matter, and I'm here.");
        sympathyOpenings.put("joy", "I'm so happy for you! That's wonderful news.");
        sympathyOpenings.put("neutral", "I'm here for you. Whatever you're feeling, I care about your wellbeing.");
        
        return sympathyOpenings.getOrDefault(emotion, "I'm truly sorry you're going through this. You don't have to face it alone.");
    }

    /**
     * Generate main content based on pathway and approach
     */
    public String generateMainContent(MonitoringContext context, String userInput, ApproachType approach) {
        return generateMainContent(context, userInput, approach, "en");
    }

    /**
     * Generate main content with language support
     */
    public String generateMainContent(MonitoringContext context, String userInput, ApproachType approach, String language) {
        PathwayType pathway = context.getCurrentPathway();
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
        
        if (pathway == PathwayType.PREVENTION) {
            return generatePreventionContent(context, approach, isMalay);
        } else {
            return generateInterventionContent(context, approach, isMalay);
        }
    }

    /**
     * Prevention content: Proactive coping strategies
     */
    private String generatePreventionContent(MonitoringContext context, ApproachType approach) {
        return generatePreventionContent(context, approach, false);
    }

    private String generatePreventionContent(MonitoringContext context, ApproachType approach, boolean isMalay) {
        StringBuilder content = new StringBuilder();
        String stressor = context.getDominantStressor();
        
        if (isMalay) {
            if (stressor != null && stressor.equals("academics")) {
                content.append("Nampaknya tekanan akademik sedang meningkat. Mari kita bekerjasama untuk mencari strategi yang boleh membantu anda merasa lebih tenang.\n\n");
                content.append("Membahagikan tugas kepada bahagian yang lebih kecil boleh membuat perbezaan besar.");
            } else if (stressor != null && stressor.equals("work")) {
                content.append("Tekanan kerja boleh sangat meletihkan. Mari kita terokai beberapa cara untuk menguruskan tekanan dan menjaga diri anda.\n\n");
                content.append("Ingat: anda tidak boleh mengawal segala-galanya, tetapi anda boleh mengawal tindak balas anda.");
            } else {
                content.append("Nampaknya tekanan sedang meningkat. Mari kita bekerjasama untuk mencari strategi yang boleh membantu anda merasa lebih tenang.");
            }
        } else {
            if (stressor != null && stressor.equals("academics")) {
                content.append("It sounds like academic pressure is building. Let's work together on some strategies that might help you feel more grounded and in control.\n\n");
                content.append("Breaking things down into smaller, manageable pieces can make a huge difference.");
            } else if (stressor != null && stressor.equals("work")) {
                content.append("Work stress can be overwhelming. Let's explore some ways to manage the pressure and take care of yourself.\n\n");
                content.append("Remember: you can't control everything, but you can control your response.");
            } else {
                content.append("It sounds like stress is building up. Let's work together on some strategies that might help you feel more grounded.");
            }
        }
        
        return content.toString();
    }

    /**
     * Intervention content: Active crisis support
     */
    private String generateInterventionContent(MonitoringContext context, ApproachType approach) {
        return generateInterventionContent(context, approach, false);
    }

    private String generateInterventionContent(MonitoringContext context, ApproachType approach, boolean isMalay) {
        StringBuilder content = new StringBuilder();
        
        if (isMalay) {
            if (context.isSuicidalIdeationDetected()) {
                content.append("Saya sangat prihatin dengan apa yang anda kongsikan. Keselamatan anda adalah perkara yang paling penting sekarang.\n\n");
                content.append("Sila hubungi profesional kesihatan mental atau talian krisis dengan segera.");
            } else {
                content.append("Saya dapat melihat anda sedang melalui masa yang sangat sukar sekarang. Tahap kesusahan ini memerlukan sokongan aktif.\n\n");
                content.append("Mari kita fokus pada apa yang anda perlukan paling banyak pada masa ini.");
            }
        } else {
            if (context.isSuicidalIdeationDetected()) {
                content.append("I'm really concerned about what you're sharing. Your safety is the most important thing right now.\n\n");
                content.append("Please reach out to a mental health professional or crisis line immediately.");
            } else {
                content.append("I can see you're going through a really difficult time right now. This level of distress needs some active support.\n\n");
                content.append("Let's focus on what you need most in this moment.");
            }
        }
        
        return content.toString();
    }

    /**
     * Generate coping strategies based on emotion and stressor
     */
    public String generateStrategies(MonitoringContext context) {
        return generateStrategies(context, "en");
    }

    public String generateStrategies(MonitoringContext context, String language) {
        StringBuilder strategies = new StringBuilder();
        String emotion = context.getCurrentEmotion();
        String stressor = context.getDominantStressor();
        boolean isMalay = language != null && language.toLowerCase().startsWith("ms");
        
        strategies.append(isMalay ? "**Perkara yang mungkin boleh membantu sekarang:**\n\n" : "**Things that might help right now:**\n\n");
        
        if (stressor != null && stressor.equals("academics")) {
            if (isMalay) {
                strategies.append("• Bahagikan tugasan kepada 3-4 tugas lebih kecil – mulakan dengan satu sahaja\n");
                strategies.append("• Cuba teknik Pomodoro: bekerja 25 minit, kemudian rehat 5 minit\n");
                strategies.append("• Minta bantuan atau lanjutan masa jika perlu\n");
                strategies.append("• Ingat: selesai lebih baik daripada sempurna\n");
            } else {
                strategies.append("• Break the assignment into 3-4 smaller tasks – just start with one\n");
                strategies.append("• Try the Pomodoro technique: work 25 minutes, then take a 5-minute break\n");
                strategies.append("• Ask for help or an extension if you need it\n");
                strategies.append("• Remember: done is better than perfect\n");
            }
        } else if (emotion.equals("anxiety")) {
            if (isMalay) {
                strategies.append("• Cuba latihan grounding 5-4-3-2-1: namakan 5 benda yang anda lihat, 4 yang boleh sentuh, 3 yang dengar, 2 yang hidu, 1 yang rasa\n");
                strategies.append("• Amalkan pernafasan kotak: nafas masuk 4 kiraan, tahan 4, keluar 4, tahan 4\n");
                strategies.append("• Gerakkan badan anda – berjalan sebentar pun boleh menenangkan sistem saraf\n");
            } else {
                strategies.append("• Try the 5-4-3-2-1 grounding exercise: name 5 things you see, 4 you can touch, 3 you hear, 2 you smell, 1 you taste\n");
                strategies.append("• Practice box breathing: breathe in for 4 counts, hold for 4, out for 4, hold for 4\n");
                strategies.append("• Move your body – even a short walk can help calm your nervous system\n");
            }
        } else if (emotion.equals("stress")) {
            if (isMalay) {
                strategies.append("• Brain dump: tulis semua yang ada dalam fikiran tanpa menapis\n");
                strategies.append("• Kenal pasti hanya SATU perkara paling mendesak – ketepikan yang lain\n");
                strategies.append("• Ambil 3 nafas dalam dan set semula fokus anda\n");
            } else {
                strategies.append("• Brain dump: write down everything on your mind without filtering\n");
                strategies.append("• Identify just ONE most urgent thing – set the rest aside for now\n");
                strategies.append("• Take 3 deep breaths and reset your focus\n");
            }
        } else if (emotion.equals("sadness")) {
            if (isMalay) {
                strategies.append("• Benarkan diri anda merasai apa yang anda rasai – tak apa untuk bersedih\n");
                strategies.append("• Hubungi seseorang yang anda percayai dan berbincang\n");
                strategies.append("• Lakukan sesuatu yang biasanya membawa keselesaan kepada anda\n");
            } else {
                strategies.append("• Allow yourself to feel what you're feeling – it's okay to be sad\n");
                strategies.append("• Reach out to someone you trust and talk about it\n");
                strategies.append("• Do something that usually brings you comfort\n");
            }
        } else {
            if (isMalay) {
                strategies.append("• Ambil jeda dan amalkan pernafasan dalam\n");
                strategies.append("• Lakukan satu perkara kecil yang biasanya membantu anda\n");
                strategies.append("• Hubungi seseorang yang anda percayai\n");
            } else {
                strategies.append("• Take a pause and practice deep breathing\n");
                strategies.append("• Do one small thing that usually helps you\n");
                strategies.append("• Reach out to someone you trust\n");
            }
        }
        
        return strategies.toString();
    }

    /**
     * Generate follow-up question based on cycle count and emotion
     */
    public String generateFollowUp(MonitoringContext context) {
        int cycleCount = context.getCycleCount();
        String emotion = context.getCurrentEmotion();
        
        if (cycleCount == 1) {
            return "What feels most urgent to address right now?";
        } else if (cycleCount >= 3 && cycleCount <= 5) {
            return "How are you feeling now compared to when we started talking?";
        } else if (cycleCount > 5) {
            return "What's one small thing we can focus on right now that might help?";
        } else {
            switch (emotion) {
                case "stress":
                    return "What's the one thing that, if handled first, would give you the most relief?";
                case "anxiety":
                    return "What's the main thing you're most worried about?";
                case "sadness":
                    return "What kind of support would feel most helpful for you right now?";
                case "anger":
                    return "What do you need to feel heard and validated right now?";
                default:
                    return "What would be most helpful for us to talk about?";
            }
        }
    }

    /**
     * Generate session closure message
     */
    public String generateSessionClosure(MonitoringContext context, String reason) {
        StringBuilder closure = new StringBuilder();
        
        if (reason.equals("improved")) {
            closure.append("\nI'm really proud of how you've worked through this. You showed real strength today. 💙\n");
            closure.append("Remember: you can come back anytime you need to talk. Take care of yourself.");
        } else if (reason.equals("stable")) {
            closure.append("\nYou're in a much better place now. That's great progress. 💙\n");
            closure.append("Try to use some of the strategies we talked about. I'm here whenever you need support.");
        } else if (reason.equals("escalated")) {
            closure.append("\nI'm concerned about your safety. Please reach out to a mental health professional or crisis line.\n");
            closure.append("You deserve professional support, and there's no shame in asking for help.");
        } else {
            closure.append("\nThank you for being open with me today. Remember, you're not alone. 💙\n");
            closure.append("Take care of yourself, and come back whenever you need to talk.");
        }
        
        return closure.toString();
    }

    /**
     * Generate message for approach switch
     */
    public String generateApproachSwitchMessage(ApproachType newApproach) {
        if (newApproach == ApproachType.SYMPATHY) {
            return "I want to try a different way to support you. I'm here for you, and I truly care about what you're going through. 💙";
        } else {
            return "Let me try to understand better what you're experiencing. What would help you feel more heard right now?";
        }
    }

    /**
     * Generate message for pathway switch (Prevention to Intervention)
     */
    public String generatePathwaySwitchMessage() {
        return "I can see things have shifted. Let me be more direct in supporting you through this difficult moment.";
    }
}

