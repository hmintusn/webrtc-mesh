package com.SFU;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebSocketMessage {
    private String type;
    private String from;
    private String to;
    private Object offer;
    private Object answer;
    private Object candidate;
    private String peerId;

    // Getters and Setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public Object getOffer() { return offer; }
    public void setOffer(Object offer) { this.offer = offer; }

    public Object getAnswer() { return answer; }
    public void setAnswer(Object answer) { this.answer = answer; }

    public Object getCandidate() { return candidate; }
    public void setCandidate(Object candidate) { this.candidate = candidate; }

    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }
}