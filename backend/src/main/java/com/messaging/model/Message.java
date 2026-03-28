package com.messaging.model;

import java.time.Instant;

public class Message {
    private String id;
    private String senderId;
    private String content;
    private long lamportTimestamp;
    private Instant creationTime;

    public Message() {
    }

    public Message(String id, String senderId, String content, long lamportTimestamp, Instant creationTime) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.lamportTimestamp = lamportTimestamp;
        this.creationTime = creationTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getLamportTimestamp() {
        return lamportTimestamp;
    }

    public void setLamportTimestamp(long lamportTimestamp) {
        this.lamportTimestamp = lamportTimestamp;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }
}