package com.SocializerAI.controller;

import com.SocializerAI.dto.ScreeningRequest;
import com.SocializerAI.dto.ScreeningResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/screening")
public class PreLLMScreeningController {

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ScreeningResponse screen(@RequestBody ScreeningRequest req) {
        List<Integer> answers = req.getDass21_answers() == null ? new ArrayList<>() : req.getDass21_answers();

        int score = answers.stream().mapToInt(Integer::intValue).sum();

        String band;
        if (score >= 17) {
            band = "Extremely severe";
        } else if (score >= 13) {
            band = "Severe";
        } else if (score >= 10) {
            band = "Moderate";
        } else if (score >= 8) {
            band = "Mild";
        } else {
            band = "Normal";
        }

        String action;
        String message;
        List<String> resources = new ArrayList<>();

        switch (band) {
            case "Extremely severe":
            case "Severe":
                action = "intervention";
                message = "High stress detected. LLM access is blocked; please review and escalate to human operator.";
                resources.add("If you are in immediate danger call your local emergency number.");
                resources.add("Crisis hotline: 1-800-273-8255 (example)");
                break;
            case "Mild":
                action = "prevention";
                message = "Mild stress detected. Provide preventive guidance and resources.";
                resources.add("Self-help resources and contacts.");
                break;
            default:
                action = "allow";
                message = "No elevated stress detected for the Stress subscale.";
        }

        ScreeningResponse resp = new ScreeningResponse();
        resp.setAction(action);
        resp.setScore(score);
        resp.setBand(band);
        resp.setMessage(message);
        resp.setResources(resources);

        return resp;
    }
}
