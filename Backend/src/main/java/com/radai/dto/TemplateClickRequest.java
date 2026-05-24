package com.radai.dto;

public class TemplateClickRequest {
    private String scenarioId;
    private int value;
    private String messageText;

    public String getScenarioId() { return scenarioId; }
    public void setScenarioId(String scenarioId) { this.scenarioId = scenarioId; }
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }
}

