package com.yourapp.model;

public class Story {
    private Long id;
    private Long authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorAvatarUrl;
    private String mediaUrl;
    private String content;
    private String musicTitle;
    private String musicArtist;
    private String musicPreviewUrl;
    private String musicAlbumCover;
    private String musicDeezerTrackId;
    private String createdAt;
    private String expiresAt;

    public Story() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }
    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }
    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMusicTitle() { return musicTitle; }
    public void setMusicTitle(String musicTitle) { this.musicTitle = musicTitle; }
    public String getMusicArtist() { return musicArtist; }
    public void setMusicArtist(String musicArtist) { this.musicArtist = musicArtist; }
    public String getMusicPreviewUrl() { return musicPreviewUrl; }
    public void setMusicPreviewUrl(String musicPreviewUrl) { this.musicPreviewUrl = musicPreviewUrl; }
    public String getMusicAlbumCover() { return musicAlbumCover; }
    public void setMusicAlbumCover(String musicAlbumCover) { this.musicAlbumCover = musicAlbumCover; }
    public String getMusicDeezerTrackId() { return musicDeezerTrackId; }
    public void setMusicDeezerTrackId(String musicDeezerTrackId) { this.musicDeezerTrackId = musicDeezerTrackId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}
