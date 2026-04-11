package com.quickdelivery.abstarct.dto;

import com.quickdelivery.abstarct.parameters.NOTIFICATION_EVENT_TYPE;

import java.sql.Timestamp;

public class NotificationDTO {
    private Long id;
    private Long recipientUserId;
    private NOTIFICATION_EVENT_TYPE eventType;
    private String title;
    private String body;
    private String targetUrl;
    private String payloadJson;
    private Boolean read;
    private Timestamp createdAt;
    private Timestamp readAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public NOTIFICATION_EVENT_TYPE getEventType() {
        return eventType;
    }

    public void setEventType(NOTIFICATION_EVENT_TYPE eventType) {
        this.eventType = eventType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getReadAt() {
        return readAt;
    }

    public void setReadAt(Timestamp readAt) {
        this.readAt = readAt;
    }
}
