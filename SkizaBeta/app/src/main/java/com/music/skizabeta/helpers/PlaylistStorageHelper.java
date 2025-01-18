package com.music.skizabeta.helpers;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.music.skizabeta.models.Playlist;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class PlaylistStorageHelper {
    private static final String PREF_NAME = "playlists_pref";
    private static final String PLAYLIST_KEY = "playlists";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public PlaylistStorageHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<Playlist> getPlaylists() {
        String json = sharedPreferences.getString(PLAYLIST_KEY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<List<Playlist>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void savePlaylists(List<Playlist> playlists) {
        String json = gson.toJson(playlists);
        sharedPreferences.edit().putString(PLAYLIST_KEY, json).apply();
    }

    public void addPlaylist(Playlist playlist) {
        List<Playlist> playlists = getPlaylists();
        playlists.add(playlist);
        savePlaylists(playlists);
    }

    public void deletePlaylist(Playlist playlist) {
        List<Playlist> playlists = getPlaylists();
        playlists.remove(playlist);
        savePlaylists(playlists);
    }
}
