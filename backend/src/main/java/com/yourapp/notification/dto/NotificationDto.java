package com.yourapp.notification.dto;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDto {
    private String id;
    private String type;
    private String title;
    private String message;
    private Long actorId;
    private String actorUsername;
    private String actorDisplayName;
    private String actorAvatarUrl;
    private LocalDateTime createdAt;
    private Map<String, Object> data;

    public NotificationDto() {
    }

    public NotificationDto(String id, String type, String title, String message,
                           Long actorId, String actorUsername, String actorDisplayName,
                           String actorAvatarUrl, LocalDateTime createdAt,
                           Map<String, Object> data) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.message = message;
        this.actorId = actorId;
        this.actorUsername = actorUsername;
        this.actorDisplayName = actorDisplayName;
        this.actorAvatarUrl = actorAvatarUrl;
        this.createdAt = createdAt;
        this.data = data;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long actorId) { this.actorId = actorId; }
    public String getActorUsername() { return actorUsername; }
    public void setActorUsername(String actorUsername) { this.actorUsername = actorUsername; }
    public String getActorDisplayName() { return actorDisplayName; }
    public void setActorDisplayName(String actorDisplayName) { this.actorDisplayName = actorDisplayName; }
    public String getActorAvatarUrl() { return actorAvatarUrl; }
    public void setActorAvatarUrl(String actorAvatarUrl) { this.actorAvatarUrl = actorAvatarUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }
}