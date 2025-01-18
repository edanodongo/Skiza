package com.music.skizabeta.models;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.music.skiza.models.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackViewModel extends AndroidViewModel {
    private final MutableLiveData<Track> currentTrack = new MutableLiveData<>();
    private final MutableLiveData<List<Track>> selectedTracks = new MutableLiveData<>();
    private final MutableLiveData<Playlist> currentPlaylist = new MutableLiveData<>();

    private final MutableLiveData<List<Track>> currentTracklist = new MutableLiveData<>();
    private final List<Track> defaultTracklist;

    public TrackViewModel(@NonNull Application application) {
        super(application);

        // Initialize the default track list
        defaultTracklist = MusicLibrary.getInstance().getTracks();
        currentTracklist.setValue(defaultTracklist);
    }



    public void setTrack(Track track) {
        currentTrack.setValue(track);
    }

    public LiveData<Track> getTrack() {
        return currentTrack;
    }

    public LiveData<List<Track>> getSelectedTracks() {
        return selectedTracks;
    }

    public void setSelectedTracks(List<Track> tracks) {
        selectedTracks.setValue(tracks);
    }

    public void setCurrentPlaylist(Playlist playlist) {
        currentPlaylist.setValue(playlist);
    }

    public LiveData<Playlist> getCurrentPlaylist() {
        return currentPlaylist;
    }


    public LiveData<List<Track>> getCurrentTracklist() {
        return currentTracklist;
    }

    public void setCurrentTrackList(List<Track> tracklist) {
        currentTracklist.setValue(tracklist);  // Use postValue if called from background thread
    }

    /*
    public void updateTracklistByArtist(Artist artist) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Track> artistTracks = new ArrayList<>();
            for (Album album : artist.getAlbums()) {
                artistTracks.addAll(album.getTracks());
            }
            currentTracklist.postValue(artistTracks);
        });
        executor.shutdown();
    }
    */

    public void updateTracklistByAlbum(Album album) {
        currentTracklist.postValue(album.getTracks());
    }

    public void updateTracklistByPlaylist(Playlist playlist) {
        currentTracklist.postValue(playlist.getTracks());
    }






    public void resetToDefaultTracklist() {
        currentTracklist.setValue(defaultTracklist);
    }

    public void updateTracklistByArtist(Artist artist) {
        List<Track> artistTracks = MusicLibrary.getInstance().getTracksByArtist(artist);
        currentTracklist.postValue(artistTracks);
    }

}

