package com.yourapp.user.dto;

import java.time.LocalDateTime;

public class PublicProfileDto {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String coverUrl;
    private String bio;
    private String location;
    private String website;
    private String dateOfBirth;
    private String gender;
    private String phone;
    private LocalDateTime lastSeenAt;
    private String friendshipStatus;
    private int mutualFriendCount;
    private int friendCount;
    private int postCount;
    private int storyCount;
    private boolean owner;

    public PublicProfileDto() {
    }

    public PublicProfileDto(Long id, String username, String displayName, String avatarUrl,
                            String coverUrl, String bio, String location, String website,
                            String dateOfBirth, String gender, String phone,
                            LocalDateTime lastSeenAt, String friendshipStatus,
                            int mutualFriendCount, int friendCount, int postCount, int storyCount,
                            boolean owner) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.coverUrl = coverUrl;
        this.bio = bio;
        this.location = location;
        this.website = website;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phone = phone;
        this.lastSeenAt = lastSeenAt;
        this.friendshipStatus = friendshipStatus;
        this.mutualFriendCount = mutualFriendCount;
        this.friendCount = friendCount;
        this.postCount = postCount;
        this.storyCount = storyCount;
        this.owner = owner;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public String getFriendshipStatus() { return friendshipStatus; }
    public void setFriendshipStatus(String friendshipStatus) { this.friendshipStatus = friendshipStatus; }
    public int getMutualFriendCount() { return mutualFriendCount; }
    public void setMutualFriendCount(int mutualFriendCount) { this.mutualFriendCount = mutualFriendCount; }
    public int getFriendCount() { return friendCount; }
    public void setFriendCount(int friendCount) { this.friendCount = friendCount; }
    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }
    public int getStoryCount() { return storyCount; }
    public void setStoryCount(int storyCount) { this.storyCount = storyCount; }
    public boolean isOwner() { return owner; }
    public void setOwner(boolean owner) { this.owner = owner; }
}