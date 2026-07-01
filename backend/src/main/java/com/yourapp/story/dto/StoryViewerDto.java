package com.yourapp.story.dto;

import java.time.LocalDateTime;

public class StoryViewerDto {
    private Long userId;
    private String userName;
    private String displayName;
    private String avatarUrl;
    private LocalDateTime viewedAt;

    public StoryViewerDto() {
    }

    public StoryViewerDto(Long userId, String userName, String displayName, String avatarUrl, LocalDateTime viewedAt) {
        this.userId = userId;
        this.userName = userName;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.viewedAt = viewedAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public LocalDateTime getViewedAt() { return viewedAt; }
    public void setViewedAt(LocalDateTime viewedAt) { this.viewedAt = viewedAt; }
}
