package com.radai.controller;

import com.radai.dto.TemplateClickRequest;
import com.radai.dto.TemplateDTO;
import com.radai.service.EmotionTrackingService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/templates")
public class TemplatesController {

    private final EmotionTrackingService emotionTrackingService;

    public TemplatesController(EmotionTrackingService emotionTrackingService) {
        this.emotionTrackingService = emotionTrackingService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TemplateDTO> getTemplates() {
        List<TemplateDTO> list = new ArrayList<>();

        // Build 4 scenarios matching frontend wording
        list.add(build("stress_wind_down",
            "Hey, welcome. This is a safe space — no pressure at all. I just want to gently check in with you first.",
            "Over the past week, did you find it hard to calm yourself down after something stressful happened?",
            new String[]{"😔 Not at all","😐 Sometimes","😟 Yes, often","😰 Almost always"},
            new String[]{
                "That's really wonderful to hear. Being able to find your calm even after tough moments is something to be proud of. I'm glad you're doing well.",
                "I see — it happens to a lot of us. Some days are harder to shake off than others, and that's okay. I'm here with you.",
                "That sounds really draining. When your mind keeps holding on even after the moment has passed, it takes so much out of you. Thank you for being honest with me.",
                "I hear you, and I want you to know — what you're carrying right now sounds really heavy. You don't have to face that alone. I'm truly glad you're here today."
            }
        ));

        list.add(build("stress_nervous_energy",
            "Thank you for being here. Let me check in on how you've truly been feeling.",
            "This past week, did you feel like you were running on nervous energy — restless, tense, or unable to find stillness?",
            new String[]{"😔 Not at all","😐 Sometimes","😟 Yes, often","😰 Almost always"},
            new String[]{
                "That's so good to hear. Finding stillness isn't always easy, so it means a lot that you've been able to feel grounded this week.",
                "A little restlessness here and there is something many of us feel. You're not alone in that. I'm here, and we'll work through this together.",
                "Feeling tense and restless that often sounds truly exhausting — especially when you just want to feel at ease. Thank you for trusting me with that.",
                "Living in that constant state of tension must be so overwhelming. I really appreciate you sharing that with me. You deserve to feel at peace, and I want to help you get there."
            }
        ));

        list.add(build("stress_upset_reactive",
            "I'm really glad you stopped by today. Before we begin, I'd love to gently check in with you.",
            "Over the past week, did you feel like things got to you more deeply than usual — like you were more easily upset than you expected?",
            new String[]{"😔 Not at all","😐 Sometimes","😟 Yes, often","😰 Almost always"},
            new String[]{
                "That's really reassuring to hear. Feeling emotionally steady is something to hold onto. I hope that sense of balance stays with you.",
                "That's completely understandable. Our emotions can surprise us sometimes, and it's okay to not always have it figured out. I'm right here with you.",
                "When things keep touching a nerve more than they used to, it can feel really confusing and tiring. What you're feeling is real, and it truly matters.",
                "That sounds like a really painful place to be — feeling raw and on edge almost every day. I'm so glad you showed up today. You took a brave step just by being here."
            }
        ));

        list.add(build("stress_difficulty_relaxing",
            "Hi, I'm so glad you're here. Let me take a gentle moment to see how you're doing.",
            "Over the past week, even when you had free time — did you find it hard to truly relax, like your mind just kept racing?",
            new String[]{"😔 Not at all","😐 Sometimes","😟 Yes, often","😰 Almost always"},
            new String[]{
                "Oh, that's really lovely. Being able to rest and actually feel restored is such a gift. I'm glad your mind has been giving you some peace.",
                "A racing mind every now and then is something so many people experience. You're not alone in this, and I'm glad you're here to talk about it.",
                "When rest doesn't feel restful, it can leave you feeling empty even on your quietest days. That's really hard, and I want you to know I see that.",
                "Not being able to truly rest — even when you so desperately need it — sounds absolutely exhausting. Thank you for telling me. You matter, and how you feel matters."
            }
        ));

        return list;
    }

        private TemplateDTO build(String id, String intro, String prompt, String[] optionLabels, String[] followUps) {
        TemplateDTO t = new TemplateDTO();
        t.setId(id);
        t.setAnonymous(true);
        t.setIntro(intro);
        t.setPrompt(prompt);
        List<TemplateDTO.Option> opts = new ArrayList<>();
        for (int i = 0; i < optionLabels.length; i++) {
            TemplateDTO.Option o = new TemplateDTO.Option();
            o.label = optionLabels[i];
            o.value = i;
            opts.add(o);
        }
        t.setOptions(opts);
        Map<Integer, String> f = new HashMap<>();
        for (int i = 0; i < followUps.length; i++) f.put(i, followUps[i]);
        t.setFollowUp(f);
        return t;
        }

    @PostMapping(path = "/track", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> trackClick(@RequestBody TemplateClickRequest req) {
        try {
            // Map 0-3 to 1-10 intensity scale: (value * 3) + 1 => 0->1,1->4,2->7,3->10
            int scaled = Math.max(1, Math.min(10, req.getValue() * 3 + 1));
            // Use scenario id as emotionalState tag
            // For anonymous template clicks, avoid inserting NULL userId which violates DB constraints.
            // Use a placeholder UUID to indicate anonymous/aggregate entries.
            java.util.UUID anonUuid = java.util.UUID.fromString("00000000-0000-0000-0000-000000000000");
            emotionTrackingService.recordEmotion(anonUuid, req.getScenarioId(), scaled, req.getMessageText(), "dass_template");
        } catch (Exception e) {
            return Collections.singletonMap("status", "error");
        }
        return Collections.singletonMap("status", "ok");
    }
}

