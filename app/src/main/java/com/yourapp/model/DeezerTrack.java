package com.yourapp.model;

public class DeezerTrack {
    private long id;
    private String title;           // tên bài
    private String preview;         // URL MP3 30 giây — QUAN TRỌNG
    private DeezerArtist artist;    // { id, name }
    private DeezerAlbum album;      // { id, title, cover_medium } 
    private int duration;           // giây

    public DeezerTrack() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public DeezerArtist getArtist() { return artist; }
    public void setArtist(DeezerArtist artist) { this.artist = artist; }

    public DeezerAlbum getAlbum() { return album; }
    public void setAlbum(DeezerAlbum album) { this.album = album; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public static class DeezerArtist {
        private long id;
        private String name;

        public DeezerArtist() {}

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class DeezerAlbum {
        private long id;
        private String title;
        private String cover_medium;

        public DeezerAlbum() {}

        public long getId() { return id; }
        public void setId(long id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getCover_medium() { return cover_medium; }
        public void setCover_medium(String cover_medium) { this.cover_medium = cover_medium; }
    }
}
