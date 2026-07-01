package com.yourapp.friendship.dto;

public class FriendDto {
    private Long friendshipId;
    private Long userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String status;       // PENDING / ACCEPTED / BLOCKED
    private boolean isOnline;    // last_seen_at within 5 minutes
    private String direction;    // SENT / RECEIVED (for pending)

    public FriendDto() {}

    public FriendDto(Long friendshipId, Long userId, String username,
                     String displayName, String avatarUrl,
                     String status, boolean isOnline, String direction) {
        this.friendshipId = friendshipId;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.isOnline = isOnline;
        this.direction = direction;
    }

    public Long getFriendshipId() { return friendshipId; }
    public void setFriendshipId(Long friendshipId) { this.friendshipId = friendshipId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
}
