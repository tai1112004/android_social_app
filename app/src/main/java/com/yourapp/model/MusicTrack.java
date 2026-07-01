package com.yourapp.model;

public class MusicTrack {
    private String deezerTrackId;
    private String title;
    private String artist;
    private String previewUrl;
    private String albumCover;

    public MusicTrack(String deezerTrackId, String title, String artist, String previewUrl, String albumCover) {
        this.deezerTrackId = deezerTrackId;
        this.title = title;
        this.artist = artist;
        this.previewUrl = previewUrl;
        this.albumCover = albumCover;
    }

    public String getDeezerTrackId() { return deezerTrackId; }
    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getPreviewUrl() { return previewUrl; }
    public String getAlbumCover() { return albumCover; }
}
