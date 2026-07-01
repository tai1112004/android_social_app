package com.yourapp.model;

public class StoryViewer {
    private Long userId;
    private String userName;
    private String displayName;
    private String avatarUrl;
    private String viewedAt;

    public StoryViewer() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getViewedAt() { return viewedAt; }
    public void setViewedAt(String viewedAt) { this.viewedAt = viewedAt; }
}
