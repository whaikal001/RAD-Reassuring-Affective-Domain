package com.radai.dto;

import java.util.List;
import java.util.Map;

public class TemplateDTO {
    public static class Option {
        public String label;
        public int value;
    }

    private String id;
    private boolean anonymous;
    private String intro;
    private String prompt;
    private List<Option> options;
    private Map<Integer, String> followUp;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public boolean isAnonymous() { return anonymous; }
    public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public List<Option> getOptions() { return options; }
    public void setOptions(List<Option> options) { this.options = options; }
    public Map<Integer, String> getFollowUp() { return followUp; }
    public void setFollowUp(Map<Integer, String> followUp) { this.followUp = followUp; }
}

