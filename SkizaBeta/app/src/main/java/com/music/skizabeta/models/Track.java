package com.music.skizabeta.models;


public class Track {
    private String id;
    private String title;
    private Artist artist;
    private Album album;
    private String filePath;
    private boolean isFavorite;
    private long duration;
    private String albumArtPath; // Path to the album art

    public Track(String id, String title, Artist artist, Album album, String filePath, String albumArtPath, boolean isFavorite, long duration) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.filePath = filePath;
        this.albumArtPath = albumArtPath;
        this.isFavorite = isFavorite;
        this.duration = duration;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Artist getArtist() {
        return artist;
    }

    public Album getAlbum() {
        return album;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public long getDuration() {
        return duration;
    }

    public String getAlbumArtPath() {
        return albumArtPath;
    }
}

