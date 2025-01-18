package com.music.skizabeta.helpers;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.music.skizabeta.models.Playlist;
import com.music.skizabeta.models.Track;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class StorageHelper {

    private static final String PLAYLISTS_FILE = "playlists.json";
    private static final String FAVORITES_FILE = "favorites.json";
    private static final String TAG = "StorageHelper";

    private Context context;
    private Gson gson;

    public StorageHelper(Context context) {
        this.context = context;
        this.gson = new Gson();
    }

    // Save playlists to file
    public void savePlaylists(List<Playlist> playlists) {
        writeFile(PLAYLISTS_FILE, playlists);
    }

    // Load playlists from file
    public List<Playlist> loadPlaylists() {
        Type listType = new TypeToken<ArrayList<Playlist>>() {}.getType();
        return readFile(PLAYLISTS_FILE, listType);
    }

    // Save favorites to file
    public void saveFavorites(List<Track> favorites) {
        writeFile(FAVORITES_FILE, favorites);
    }

    // Load favorites from file
    public List<Track> loadFavorites() {
        Type listType = new TypeToken<ArrayList<Track>>() {}.getType();
        return readFile(FAVORITES_FILE, listType);
    }

    // General method to write a list to a JSON file
    private <T> void writeFile(String filename, List<T> data) {
        File file = new File(context.getFilesDir(), filename);
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(data, writer);
            Log.d(TAG, filename + " saved successfully.");
        } catch (IOException e) {
            Log.e(TAG, "Error saving " + filename, e);
        }
    }

    // General method to read a list from a JSON file
    private <T> List<T> readFile(String filename, Type typeOfT) {
        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            return new ArrayList<>(); // Return an empty list if the file doesn't exist
        }

        try (FileReader reader = new FileReader(file)) {
            return gson.fromJson(reader, typeOfT);
        } catch (IOException e) {
            Log.e(TAG, "Error loading " + filename, e);
            return new ArrayList<>(); // Return an empty list if an error occurs
        }
    }
}
