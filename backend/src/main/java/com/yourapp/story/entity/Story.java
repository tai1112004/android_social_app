package com.yourapp.story.entity;

import com.yourapp.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stories")
public class Story {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "media_url", length = 500)
    private String mediaUrl;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "music_title", length = 255)
    private String musicTitle;

    @Column(name = "music_artist", length = 255)
    private String musicArtist;

    @Column(name = "music_preview_url", length = 700)
    private String musicPreviewUrl;

    @Column(name = "music_album_cover", length = 700)
    private String musicAlbumCover;

    @Column(name = "music_deezer_track_id", length = 80)
    private String musicDeezerTrackId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(24);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
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
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
