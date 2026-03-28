package com.messaging.model;

import jakarta.validation.constraints.NotBlank;

public class SendMessageRequest {
    @NotBlank(message = "senderId is required")
    private String senderId;

    @NotBlank(message = "content is required")
    private String content;

    public SendMessageRequest() {
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
