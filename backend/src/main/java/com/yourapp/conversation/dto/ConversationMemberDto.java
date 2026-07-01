package com.yourapp.conversation.dto;

import java.time.LocalDateTime;

public class ConversationMemberDto {

    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String role;
    private LocalDateTime joinedAt;
    private boolean online;

    public ConversationMemberDto() {
    }

    public ConversationMemberDto(Long userId, String username, String displayName, String avatarUrl,
                                 String role, LocalDateTime joinedAt, boolean online) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.joinedAt = joinedAt;
        this.online = online;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }
}
