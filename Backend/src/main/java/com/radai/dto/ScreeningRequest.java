package com.radai.dto;

import java.util.List;

public class ScreeningRequest {
    private Boolean consent;
    private String language;
    private List<Integer> dass21_answers;

    public Boolean getConsent() {
        return consent;
    }

    public void setConsent(Boolean consent) {
        this.consent = consent;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Integer> getDass21_answers() {
        return dass21_answers;
    }

    public void setDass21_answers(List<Integer> dass21_answers) {
        this.dass21_answers = dass21_answers;
    }
}

