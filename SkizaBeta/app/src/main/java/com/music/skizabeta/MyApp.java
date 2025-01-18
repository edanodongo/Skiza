package com.music.skizabeta;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.os.Build;
import android.os.IBinder;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;

import com.music.skizabeta.helpers.StorageHelper;
import com.music.skizabeta.listeners.ServiceConnectionListener;
import com.music.skizabeta.models.MusicLibrary;
import com.music.skizabeta.models.Playlist;
import com.music.skizabeta.models.Track;
import com.music.skizabeta.services.MusicPlayerService;

import java.util.List;


public class MyApp extends Application implements ServiceConnectionListener {
    private static MusicPlayerService musicPlayerService;
    private static MusicLibrary musicLibrary;
    private static StorageHelper storageHelper;


    private ViewModelStore viewModelStore;
    private ViewModelProvider applicationViewModelProvider;


    @Override
    public void onCreate() {
        super.onCreate();
        musicLibrary = new MusicLibrary();
        // Load your music tracks into the music library here
        //
        loadMusicLibrary();
        storageHelper = new StorageHelper(this);

        loadMusicLibrary();
        loadPlaylists();
        ensureAllTracksPlaylist();

        createNotificationChannel();

        preloadMusicLibrary();

        // Initialize ViewModelStore
        viewModelStore = new ViewModelStore();

        // Initialize ViewModelProvider for application-scoped ViewModels
        applicationViewModelProvider = new ViewModelProvider(viewModelStore,
                ViewModelProvider.AndroidViewModelFactory.getInstance(this));

    }


    private void preloadMusicLibrary() {
        List<Track> tracks = MusicLibrary.loadSongsFromStorage(this);
        MusicLibrary.getInstance().addAllTracks(tracks);
    }

    public ViewModelProvider getApplicationViewModelProvider() {
        return applicationViewModelProvider;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Clear the ViewModelStore to avoid memory leaks
        viewModelStore.clear();
    }

    public static MusicPlayerService getMusicPlayerService() {
        return musicPlayerService;
    }

    public static void setMusicPlayerService(MusicPlayerService service) {
        musicPlayerService = service;
    }

    public static MusicLibrary getMusicLibrary() {
        return musicLibrary;
    }

    private void loadMusicLibrary() {
        // Implement your logic to load music tracks, albums, and artists
        // Example: musicLibrary.loadSongsFromStorage();

        List<Track> tracks = MusicLibrary.loadSongsFromStorage(this);
        for (Track track : tracks) {
            MyApp.getMusicLibrary().addTrack(track);
        }
    }

    private void loadPlaylists() {
        List<Playlist> playlists = storageHelper.loadPlaylists();
        for (Playlist playlist : playlists) {
            musicLibrary.addPlaylist(playlist);
        }
    }

    private void ensureAllTracksPlaylist() {
        boolean exists = musicLibrary.getPlaylists().stream()
                .anyMatch(p -> p.getId().equals("all_tracks"));
        if (!exists) {
            Playlist allTracksPlaylist = musicLibrary.createAllTracksPlaylist();
            storageHelper.savePlaylists(musicLibrary.getPlaylists());
        }
    }

    private boolean isServiceBound = false;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
        musicPlayerService = binder.getService();
        isServiceBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        musicPlayerService = null;
        isServiceBound = false;

    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "MUSIC_CHANNEL", "Music Playback", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

}
