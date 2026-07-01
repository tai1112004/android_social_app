package com.yourapp.conversation.dto;

import java.time.LocalDateTime;

public class ConversationDto {

    private Long id;
    private String type;
    private String name;
    private int memberCount;
    private LocalDateTime createdAt;
    private Boolean online;
    private LocalDateTime lastSeenAt;
    private long unreadCount;
    private String lastMessagePreview;

    public ConversationDto() {
    }

    public ConversationDto(Long id, String type, String name, int memberCount, LocalDateTime createdAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
    }

    public ConversationDto(Long id, String type, String name, int memberCount, LocalDateTime createdAt, Boolean online, LocalDateTime lastSeenAt) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.online = online;
        this.lastSeenAt = lastSeenAt;
    }

    public ConversationDto(Long id, String type, String name, int memberCount, LocalDateTime createdAt, Boolean online, LocalDateTime lastSeenAt, long unreadCount, String lastMessagePreview) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.memberCount = memberCount;
        this.createdAt = createdAt;
        this.online = online;
        this.lastSeenAt = lastSeenAt;
        this.unreadCount = unreadCount;
        this.lastMessagePreview = lastMessagePreview;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public long getUnreadCount() { return unreadCount; }
    public void setUnreadCount(long unreadCount) { this.unreadCount = unreadCount; }
    public String getLastMessagePreview() { return lastMessagePreview; }
    public void setLastMessagePreview(String lastMessagePreview) { this.lastMessagePreview = lastMessagePreview; }
}