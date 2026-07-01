package com.yourapp.story.dto;

import java.time.LocalDateTime;
import java.util.List;

public class StoryGroupDto {
    private Long userId;
    private String userName;
    private String userAvatarUrl;
    private boolean hasUnviewed;
    private List<StoryDto> stories;
    private LocalDateTime latestStoryTime;

    public StoryGroupDto() {
    }

    public StoryGroupDto(Long userId, String userName, String userAvatarUrl,
                         boolean hasUnviewed, List<StoryDto> stories,
                         LocalDateTime latestStoryTime) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.hasUnviewed = hasUnviewed;
        this.stories = stories;
        this.latestStoryTime = latestStoryTime;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }
    public boolean isHasUnviewed() { return hasUnviewed; }
    public void setHasUnviewed(boolean hasUnviewed) { this.hasUnviewed = hasUnviewed; }
    public List<StoryDto> getStories() { return stories; }
    public void setStories(List<StoryDto> stories) { this.stories = stories; }
    public LocalDateTime getLatestStoryTime() { return latestStoryTime; }
    public void setLatestStoryTime(LocalDateTime latestStoryTime) { this.latestStoryTime = latestStoryTime; }
}
