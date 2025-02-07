package com.music.skizabeta.activities;

//import static com.music.skizabeta.fragments.PlaylistFragment.allTracks;
//import static com.music.skizabeta.fragments.PlaylistFragment.playlists;
//import static com.music.skizabeta.fragments.PlaylistFragment.selectedPlaylist;
import static com.music.skizabeta.services.MusicPlayerService.currentPlaylist;
import static com.music.skizabeta.services.MusicPlayerService.mediaPlayer;
import static com.music.skizabeta.services.MusicPlayerService.playingTrack;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.music.skizabeta.MyApp;
import com.music.skizabeta.R;


// HomeActivity.java
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.music.skizabeta.fragments.AlbumFragment;
import com.music.skizabeta.fragments.ArtistFragment;
import com.music.skizabeta.fragments.PlayFragment;
import com.music.skizabeta.fragments.PlaylistFragment;
import com.music.skizabeta.fragments.TracksFragment;
import com.music.skizabeta.helpers.StorageHelper;
import com.music.skizabeta.listeners.TrackClickListener;
import com.music.skizabeta.listeners.TrackFinished;
import com.music.skizabeta.models.MusicLibrary;
import com.music.skizabeta.models.Playlist;
import com.music.skizabeta.models.Track;
import com.music.skizabeta.models.TrackViewModel;
import com.music.skizabeta.services.MusicPlayerService;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity implements TrackFinished {

    private Track currentTrack;

    private static BottomSheetBehavior<View> bottomSheetBehavior;
    private static LinearLayout miniPlayer;
    private static LinearLayout expandedPlayer;
    private static FrameLayout fragmentContainer;

    public static Playlist selectedPlaylist;

    BottomNavigationView bottomNavigationView;
    private MediaSessionCompat mediaSession;

    public Context context;
    public static Playlist allTracksPlaylist;

    AlbumFragment albumFragment = new AlbumFragment();
    TracksFragment tracksFragment = new TracksFragment();
    ArtistFragment artistFragment = new ArtistFragment();
    PlayFragment playFragment = new PlayFragment();
    PlaylistFragment playlistFragment = new PlaylistFragment();


    //private Playlist selectedPlaylist;
    private Playlist currentList;
    //public static List<Playlist> playlists;

    private static final String CHANNEL_ID = "MusicPlayerChannel";

    private MusicPlayerService musicService;
    public static boolean isBound = false;


    private TrackViewModel trackViewModel;


    // For seekBar
    private SeekBar seekBar;
    private TextView currentTimeTextView, totalDurationTextView;
    private boolean isUserSeeking = false;
    private Handler handler;

    private TextView trackTitle, trackArtist, miniPlayerTitle;
    private ImageButton playPauseButton, previousButton, nextButton, shuffleButton, miniPlayPause;

    private ImageView albumArtImageView, miniAlbumArtImageView;


    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            // Get the bound service instance from the binder
            MusicPlayerService.LocalBinder musicBinder = (MusicPlayerService.LocalBinder) service;
            musicService = musicBinder.getService(); // Get the MusicPlaybackService instance
            isBound = true; // Mark the service as bound

            // Optionally, you can initialize the media player or play a track here
            // musicService.play(); // Start playback if desired
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            isBound = false; // Mark the service as unbound
            musicService = null; // Nullify the service reference

            //PlayActivity.this.onServiceDisconnected(); // Notify listener
        }
    };


    public static void setupBottomSheetBehavior() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Toggle between mini and expanded views based on the bottom sheet state
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {

                    miniPlayer.setVisibility(View.GONE);
                    expandedPlayer.setVisibility(View.VISIBLE);
                    fragmentContainer.setVisibility(View.GONE);

                    //Update SeekBar
                    //updateSeekBarUI(track);

                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    miniPlayer.setVisibility(View.VISIBLE);
                    expandedPlayer.setVisibility(View.GONE);
                    fragmentContainer.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                // Optional: Add sliding animations or transitions if needed
            }
        });

        // Expand the bottom sheet when mini player is clicked
        miniPlayer.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED));

        expandedPlayer.setOnClickListener(v -> bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Now Playing

        // Initialize the MediaSession
        mediaSession = new MediaSessionCompat(this, "HomeActivity");

        // Initialize the SeekBar
        seekBar = findViewById(R.id.seekBar);
        currentTimeTextView = findViewById(R.id.current_time);
        totalDurationTextView = findViewById(R.id.total_duration);

        miniAlbumArtImageView = findViewById(R.id.mini_player_cover);
        albumArtImageView = findViewById(R.id.expanded_cover);

        previousButton = findViewById(R.id.btnPrevious);
        nextButton = findViewById(R.id.btnNext);
        shuffleButton = findViewById(R.id.btnShuffle);

        trackTitle = findViewById(R.id.tvTrackTitle);
        trackArtist = findViewById(R.id.tvArtistName);
        playPauseButton = findViewById(R.id.btnPlayPause);

        miniPlayPause = findViewById(R.id.mini_player_play_pause);

        // Create notification channel for Android 8.0+
        // createNotificationChannel();

        // Rest of your code...

        // Bind to the MusicPlayerService
        Intent intent = new Intent(this, MusicPlayerService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);


        selectedPlaylist = getPlaylistById("all_tracks");

        miniPlayerTitle = findViewById(R.id.mini_player_title);
        // Obtain the ViewModel
        trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);

        /*
        // Observe changes in the selected playlist
        trackViewModel.getCurrentPlaylist().observe(this, playlist -> {
            // Update the UI with the selected track details
            if (playlist != null) {

                //List<Track> currentTracks = selectedPlaylist.getTracks();
            }else {
                Toast.makeText(getApplicationContext(), "No Playlist Selected ", Toast.LENGTH_SHORT).show();
            }
            // Retrieve MusicPlayerService
            musicService = MyApp.getMusicPlayerService();
        });
        */

        // Observe changes in the selected track from TrackFragment and Playlist Fragment (Working)
        trackViewModel.getTrack().observe(this, track -> {
            // Update the UI with the selected track details
            if (track != null) {

                currentTrack = track;
                miniPlayerTitle.setText(track.getTitle());
                currentPlaylist = selectedPlaylist.getTracks();

                playingTrack = track;
                updateTrackInfo(track);
                updateSeekBarUI(track);
                updateUI();
            }
        });


        trackViewModel.getCurrentTracklist().observe(this, tracklist -> {
            if (tracklist.equals(MusicLibrary.getInstance().getTracks())) {
                Toast.makeText(getApplicationContext(), "Default Selected ", Toast.LENGTH_SHORT).show();
                // nowPlayingTextView.setText("Now Playing: All Tracks");
            } else {
                Toast.makeText(getApplicationContext(), "Another Playlist", Toast.LENGTH_SHORT).show();
                //nowPlayingTextView.setText("Now Playing: Custom List");
            }
        });


        currentTrack = trackViewModel.getTrack().getValue();
        if (currentTrack != null) {

            updateTrackInfo(currentTrack);

            miniPlayerTitle.setText(currentTrack.getTitle());
        } else {
            Toast.makeText(getApplicationContext(), "Default Selected ", Toast.LENGTH_SHORT).show();

            startTrack();
        }


        trackViewModel.getCurrentPlaylist().observe(this, currentPlaylist -> {
            // Update the UI with the selected track details
            if (currentPlaylist != null) {
                currentList = currentPlaylist;
            }
        });

        /*
        MusicLibrary musicLibrary = MyApp.getMusicLibrary();
        Playlist allTracksPlaylist = musicLibrary.getPlaylists()
                .stream()
                .filter(p -> p.getId().equals("all_tracks"))
                .findFirst()
                .orElse(null);
        */




        new ViewModelProvider(this).get(TrackViewModel.class)
                .getCurrentPlaylist().observe(this, currentPlaylist -> {
                    trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);
                    trackViewModel.setCurrentPlaylist(currentPlaylist);
                });



        // For selected when adding tracks to playlist
        trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);
        trackViewModel.getSelectedTracks();



        ContextCompat.startForegroundService(this, intent);  // Ensures it starts in the foreground

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        // Set the default fragment

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, tracksFragment)
                    .commit();
        }
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_album) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, albumFragment)
                        .commit();
                return true;
            } else if (itemId == R.id.nav_tracks) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, tracksFragment)
                        .commit();
                return true;
            } else if (itemId == R.id.nav_artists) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, artistFragment)
                        .commit();
                return true;

            } else if (itemId == R.id.nav_playlist) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, playlistFragment)
                        .commit();
                return true;
            }else {
                return super.onOptionsItemSelected(item);
            }
        });



        miniPlayer = findViewById(R.id.mini_player);
        expandedPlayer = findViewById(R.id.expanded_player);
        fragmentContainer = findViewById(R.id.fragment_container);


        View bottomSheet = findViewById(R.id.bottom_sheet_play_fragment);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        setupBottomSheetBehavior();

        // Define a Handler to manage periodic updates fir seekBar
        handler = new Handler();




        playPauseButton.setOnClickListener(v -> {
            // Retrieve the selected track from the arguments
            togglePlayback();

            Toast.makeText(getApplicationContext(), "Playlist size " + currentPlaylist.size(), Toast.LENGTH_SHORT).show();
        });

        miniPlayPause.setOnClickListener(v -> {

            if (currentPlaylist != null && currentTrack != null) {
                // Obtain the ViewModel (Working ViewModel)
                trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);
                trackViewModel.setTrack(currentTrack);
            }

            togglePlayback();
        });

        previousButton.setOnClickListener(v -> {
            skipToPreviousTrack();
        });

        nextButton.setOnClickListener(v -> {
            skipToNextTrack();
        });

        shuffleButton.setOnClickListener(v -> {

            ShuffleTrack();
        });


        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Optionally update a time preview if needed
                if (fromUser && musicService != null) {
                    int newPosition = (int) (progress / 100.0 * musicService.getDuration());
                    musicService.seekTo(newPosition); // Seek to the new position
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar); // Pause updates while the user is dragging
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                handler.post(updateSeekBar); // Resume updates
            }
        });


        Intent serviceIntent = getIntent(); // Get the intent that started this activity
        String receivedData = intent.getStringExtra("data_key"); // Retrieve data from the intent

        if ((Objects.equals(receivedData, "Service notification clicked"))){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        // Use the receivedData as needed in your activity
        // For example, you can set it to a TextView or perform other actions
        Toast.makeText(this, "Received data: " + receivedData, Toast.LENGTH_SHORT).show();





        // Set MediaSession callbacks to handle actions
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                playTrack();
            }

            @Override
            public void onPause() {
                pauseTrack();
            }

            @Override
            public void onSkipToNext() {
                skipToNextTrack();
            }

            @Override
            public void onSkipToPrevious() {
                skipToPreviousTrack();
            }
        });



        // Updated TrackViewModel
        trackViewModel.getCurrentTracklist().observe(this, tracklist -> {
            if (tracklist.equals(MusicLibrary.getInstance().getTracks())) {
                //nowPlayingTextView.setText("Now Playing: All Tracks");
            } else {
                //nowPlayingTextView.setText("Now Playing: Custom List");
            }
            //trackAdapter.updateTracks(tracklist);
        });
    }

    private void updateUI() {
        playPauseButton.setImageResource(R.drawable.ic_pause);
        miniPlayPause.setImageResource(R.drawable.ic_pause);
    }


    public static Playlist getPlaylistById(String playlistId) {
        MusicLibrary musicLibrary = MyApp.getMusicLibrary();

        return null;
        /*
        return musicLibrary.getPlaylists()
                .stream()
                .filter(p -> p.getId().equals(playlistId))
                .findFirst()
                .orElse(null);
        */
    }


    /*
    // MainActivity.java
    TrackViewModel trackViewModel = new ViewModelProvider(this).get(TrackViewModel.class);

    @Override
    public void onTrackSelected(Track track) {
        trackViewModel.setTrack(track);
        //playFragmentBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
    */

    // For notification
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Music Player",
                    NotificationManager.IMPORTANCE_DEFAULT // or IMPORTANCE_DEFAULT for higher priority
            );
            channel.setDescription("Channel for music player controls");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }


    public void updateTrackInfo(Track currentTrack) {

        // Display track title and artist name
        trackTitle.setText(currentTrack.getTitle());
        trackArtist.setText(currentTrack.getArtist().getName());

        Log.e("TracksFragment", "Failed to open album art URI." + currentTrack.getAlbumArtPath());
        try {
            Uri albumArtUri = Uri.parse(currentTrack.getAlbumArtPath());
            ContentResolver contentResolver = this.getContentResolver();// requireContext().getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(albumArtUri);

            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                albumArtImageView.setImageBitmap(bitmap);
                miniAlbumArtImageView.setImageBitmap(bitmap);
                inputStream.close();
            } else {
                albumArtImageView.setImageResource(R.drawable.placeholder_cover);

                miniAlbumArtImageView.setImageResource(R.drawable.placeholder_cover);
                Log.e("PlayFragment", "Failed to open album art URI.");
            }
        } catch (Exception e) {
            albumArtImageView.setImageResource(R.drawable.placeholder_cover);
            miniAlbumArtImageView.setImageResource(R.drawable.placeholder_cover);
            Log.e("PlayFragment", "Error loading album art: " + e.getMessage(), e);
        }
    }

    private void togglePlayback() {

        if (musicService != null) {
            if (MusicPlayerService.isPlaying) {
                //pauseMusic
                pauseTrack();

            } else if (MusicPlayerService.isPause) {
                //resumeMusic
                ResumeTrack();

            } else {
                //playMusic
                playTrack();
            }
        } else {
            Log.e("PlayActivity", "MusicPlayerService is null");
        }

    }

    private void playTrack() {
        // Code to start playback using MusicPlayerService

        currentPlaylist = selectedPlaylist.getTracks();

        currentTrack = trackViewModel.getTrack().getValue();

        Toast.makeText(getApplicationContext(), "Another Playlist", Toast.LENGTH_SHORT).show();


        MusicPlayerService.isPlaying = true;
        musicService.playTrack(currentTrack);
        miniPlayerTitle.setText(currentTrack.getTitle());

        musicService.showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

        playPauseButton.setImageResource(R.drawable.ic_pause);
        miniPlayPause.setImageResource(R.drawable.ic_pause);// Set the initial play/pause button state
    }

    private void pauseTrack() {
        // Code to pause playback

        musicService.pauseTrack(currentTrack);
        MusicPlayerService.isPause = true;
        MusicPlayerService.isPlaying = false;
        musicService.showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
        playPauseButton.setImageResource(R.drawable.ic_play);
        miniPlayPause.setImageResource(R.drawable.ic_play);// Set the initial play/pause button state
    }

    private void ResumeTrack() {
        // Code to pause playback

        musicService.resumeTrack(currentTrack);
        MusicPlayerService.isPause = false;
        MusicPlayerService.isPlaying = true;
        musicService.showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
        playPauseButton.setImageResource(R.drawable.ic_pause);
        miniPlayPause.setImageResource(R.drawable.ic_pause);// Set the initial play/pause button state
    }

    private void skipToPreviousTrack(){
        //Track playingNextTrack = playingNext;
        musicService.skipToPreviousTrack();
        musicService.showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

        // Updating albumArt
        updateTrackInfo(playingTrack);

        // Display track title and artist name
        trackTitle.setText(playingTrack.getTitle());
        trackArtist.setText(playingTrack.getArtist().getName());


        miniPlayerTitle.setText(playingTrack.getTitle());
    }

    private void skipToNextTrack(){
        musicService.skipToNextTrack();

        // Updating albumArt
        updateTrackInfo(playingTrack);

        // Display track title and artist name
        trackTitle.setText(playingTrack.getTitle());
        trackArtist.setText(playingTrack.getArtist().getName());
        miniPlayerTitle.setText(playingTrack.getTitle());


        musicService.showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
    }

    private void ShuffleTrack() {
        if (MusicPlayerService.isShuffleEnabled) {
            musicService.shuffleTracks(false);
            // Display track title and artist name
            trackTitle.setText(playingTrack.getTitle());
            trackArtist.setText(playingTrack.getArtist().getName());


        }else {
            musicService.shuffleTracks(true);
            // Display track title and artist name
            trackTitle.setText(playingTrack.getTitle());
            trackArtist.setText(playingTrack.getArtist().getName());

        }
        miniPlayerTitle.setText(playingTrack.getTitle());
        musicService.showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
    }


    private void startTrack() {
        // Code to set track on start of activity

        selectedPlaylist = getPlaylistById("all_tracks");
        currentPlaylist = selectedPlaylist.getTracks();
        currentTrack = currentPlaylist.get(0);

        updateTrackInfo(currentTrack);
        miniPlayerTitle.setText(currentTrack.getTitle());

        MusicPlayerService.isPause = true;
        MusicPlayerService.isPlaying = false;
        //musicService.showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
        playPauseButton.setImageResource(R.drawable.ic_play);
        miniPlayPause.setImageResource(R.drawable.ic_play);// Set the initial play/pause button state
    }

    private Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (isBound && musicService != null) {
                int currentPosition = musicService.getCurrentPosition();
                int duration = musicService.getDuration();


                if (duration > 0) {
                    seekBar.setProgress((int) ((currentPosition / (float) duration) * 100));
                    currentTimeTextView.setText(formatTime(currentPosition));
                    totalDurationTextView.setText(formatTime(duration));

                } else if (currentTimeTextView.getText().equals(totalDurationTextView.getText())){
                    Toast.makeText(getApplicationContext(), "Track Finished ", Toast.LENGTH_SHORT).show();
                    /* onTrackFinished();
                    currentPosition = musicService.getCurrentPosition();
                    duration = musicService.getDuration();

                    seekBar.setProgress(0);
                    seekBar.setProgress((int) ((currentPosition / (float) duration) * 100));
                    currentTimeTextView.setText(formatTime(currentPosition));
                    totalDurationTextView.setText(formatTime(duration));
                    */
                }

            }

            handler.postDelayed(this, 1000); // Update every second
        }
    };

    private String formatTime(int millis) {
        int minutes = millis / 1000 / 60;
        int seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onTrackFinished() {

        musicService.playNextTrack();

        // Updating albumArt
        updateTrackInfo(playingTrack);

        // Display track title and artist name
        trackTitle.setText(playingTrack.getTitle());
        trackArtist.setText(playingTrack.getArtist().getName());
        miniPlayerTitle.setText(playingTrack.getTitle());


        musicService.showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
    }


    private void updateSeekBarUI(Track track) {

        // Reset SeekBar
        seekBar.setProgress(0);
        seekBar.setMax(100); // SeekBar max is a percentage

        // Update total duration TextView
        //totalDurationTextView.setText(formatTime(musicService.getDuration()));

        // Start updating the SeekBar
        handler.post(updateSeekBar);
    }




}
