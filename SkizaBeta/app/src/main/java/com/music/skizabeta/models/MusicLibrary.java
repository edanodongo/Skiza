package com.music.skizabeta.models;


import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicLibrary {

    private final List<Track> tracks;
    private final List<Album> albums;
    private final List<Artist> artists;
    private final List<Playlist> playlists;

    // Caches for faster lookup
    private final Map<String, List<Track>> artistTrackCache;
    private final Map<String, List<Track>> albumTrackCache;

    // Singleton instance
    private static MusicLibrary instance;

    public MusicLibrary() {
        tracks = new ArrayList<>();
        albums = new ArrayList<>();
        artists = new ArrayList<>();
        playlists = new ArrayList<>();
        artistTrackCache = new HashMap<>();
        albumTrackCache = new HashMap<>();
    }

    public static MusicLibrary getInstance() {
        if (instance == null) {
            instance = new MusicLibrary();
        }
        return instance;
    }


    /*** Add Methods ***/
    public void addTrack(Track track) {
        tracks.add(track);

        // Cache the track for its artist
        String artistName = track.getArtist().getName();
        artistTrackCache.computeIfAbsent(artistName, k -> new ArrayList<>()).add(track);

        // Cache the track for its album
        String albumTitle = track.getAlbum().getTitle();
        albumTrackCache.computeIfAbsent(albumTitle, k -> new ArrayList<>()).add(track);
    }

    public void addAlbum(Album album) {
        albums.add(album);
    }

    public void addArtist(Artist artist) {
        artists.add(artist);
    }

    public void addPlaylist(Playlist playlist) {
        playlists.add(playlist);
    }

    public void addAllTracks(List<Track> trackList) {
        for (Track track : trackList) {
            addTrack(track);
        }
    }


    /*** Get Methods ***/
    // Method to get all tracks
    public List<Track> getTracks() {
        return new ArrayList<>(tracks); // Return a copy to avoid modification
    }

    // Method to get track by ID
    public Track getTrackById(String id) {
        for (Track track : tracks) {
            if (track.getId().equals(id)) {
                return track;
            }
        }
        return null;
    }

    // Method to get all albums
    public List<Album> getAlbums() {
        return new ArrayList<>(albums);
    }

    // Method to get all artists
    public List<Artist> getArtists() {
        return new ArrayList<>(artists);
    }

    // Method to get favorite tracks
    public List<Track> getFavoriteTracks() {
        List<Track> favoriteTracks = new ArrayList<>();
        for (Track track : tracks) {
            if (track.isFavorite()) {
                favoriteTracks.add(track);
            }
        }
        return favoriteTracks;
    }

    // Method to get all playlists
    public List<Playlist> getPlaylists() {
        return new ArrayList<>(playlists);
    }

    public List<Track> getTracksByArtist(Artist artist) {
        return artistTrackCache.getOrDefault(artist.getName(), new ArrayList<>());
    }

    public List<Track> getTracksByAlbum(Album album) {
        return albumTrackCache.getOrDefault(album.getTitle(), new ArrayList<>());
    }

    public Playlist getPlaylistById(String playlistId) {
        for (Playlist playlist : playlists) {
            if (playlist.getId().equals(playlistId)) {
                return playlist;
            }
        }
        return null; // Playlist not found
    }


    /*** Utility Methods ***/
    public void removeTrackFromPlaylist(String playlistId, Track track) {
        Playlist playlist = getPlaylistById(playlistId);
        if (playlist != null) {
            playlist.getTracks().remove(track);
        }
    }

    public void deletePlaylist(String playlistId) {
        playlists.removeIf(playlist -> playlist.getId().equals(playlistId));
    }

    public void validateTracks(List<Playlist> playlistsToValidate) {
        for (Playlist playlist : playlistsToValidate) {
            List<Track> validTracks = new ArrayList<>();
            for (Track track : playlist.getTracks()) {
                if (tracks.contains(track)) {
                    validTracks.add(track);
                }
            }
            playlist.setTracks(validTracks); // Update with valid tracks only
        }
    }

    public Track findTrackById(String trackId) {
        for (Track track : tracks) {
            if (track.getId().equals(trackId)) {
                return track;
            }
        }
        return null; // Track not found
    }

    public Playlist createAllTracksPlaylist() {
        Playlist allTracksPlaylist = new Playlist("all_tracks", "All Tracks", new ArrayList<>(tracks));
        addPlaylist(allTracksPlaylist);
        return allTracksPlaylist;
    }

    /*** Load Tracks from Storage ***/
    public static List<Track> loadSongsFromStorage(Context context) {
        List<Track> trackList = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            int albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int albumArtColumn = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);

            do {
                String id = cursor.getString(idColumn);
                String title = cursor.getString(titleColumn);
                String artistName = cursor.getString(artistColumn);
                String albumName = cursor.getString(albumColumn);
                String path = cursor.getString(pathColumn);
                long duration = cursor.getLong(durationColumn);
                String albumId = cursor.getString(albumIdColumn);

                Artist artist = new Artist(artistName);
                Uri albumArtUri;

                if (albumId != null && !albumId.isEmpty()) {
                    albumArtUri = Uri.withAppendedPath(Uri.parse("content://media/external/audio/albumart"), albumId);
                    //Log.d("AlbumArt", "Album art URI: " + albumArtUri);
                } else {
                    Log.d("AlbumArt", "No album ID for track. Using default album art.");
                    albumArtUri = null; // Use placeholder later
                }

                Track track = new Track(
                        id,
                        title,
                        new Artist(artistName),
                        new Album(albumName, new Artist(artistName)),
                        path,
                        albumArtUri.toString(), // Album art path
                        false,
                        duration
                );

                trackList.add(track);
                //Log.d("MusicLibrary", "Loaded track: " + title + " by " + artist);
            } while (cursor.moveToNext());

            cursor.close();
        } else {
            Log.e("MusicLibrary", "No tracks found or failed to query.");
        }

        return trackList;
    }
}
