package com.yourapp.model;

import java.util.ArrayList;
import java.util.List;

public class StoryGroup {
    private Long userId;
    private String userName;
    private String userAvatarUrl;
    private boolean hasUnviewed;
    private List<Story> stories = new ArrayList<>();
    private String latestStoryTime;
    private int currentIndex;

    public StoryGroup() {
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
    public boolean isHasUnviewed() { return hasUnviewed; }
    public void setHasUnviewed(boolean hasUnviewed) { this.hasUnviewed = hasUnviewed; }
    public List<Story> getStories() { return stories; }
    public void setStories(List<Story> stories) { this.stories = stories != null ? stories : new ArrayList<>(); }
    public String getLatestStoryTime() { return latestStoryTime; }
    public void setLatestStoryTime(String latestStoryTime) { this.latestStoryTime = latestStoryTime; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }
}
