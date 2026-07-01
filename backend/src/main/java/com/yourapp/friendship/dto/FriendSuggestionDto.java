package com.yourapp.friendship.dto;

public class FriendSuggestionDto extends FriendDto {

    private int mutualFriendCount;
    private String reason;

    public FriendSuggestionDto() {
    }

    public FriendSuggestionDto(Long userId, String username, String displayName, String avatarUrl,
                               boolean online, int mutualFriendCount, String reason) {
        super(null, userId, username, displayName, avatarUrl, null, online, "SUGGESTED");
        this.mutualFriendCount = mutualFriendCount;
        this.reason = reason;
    }

    public int getMutualFriendCount() {
        return mutualFriendCount;
    }

    public void setMutualFriendCount(int mutualFriendCount) {
        this.mutualFriendCount = mutualFriendCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
