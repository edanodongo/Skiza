package com.music.skizabeta.services;

import static com.music.skiza.activities.HomeActivity.allTracksPlaylist;
import static com.music.skiza.fragments.PlaylistFragment.selectedPlaylist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.music.skiza.MyApp;
import com.music.skiza.R;
import com.music.skiza.activities.HomeActivity;
import com.music.skiza.listeners.PlaylistClickListener;
import com.music.skiza.models.MusicLibrary;
import com.music.skiza.models.Playlist;
import com.music.skiza.models.Track;
import com.music.skiza.models.TrackViewModel;
import com.music.skiza.receivers.NotificationReceiver;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.widget.RemoteViews;


import android.graphics.BitmapFactory;
import android.widget.Toast;

public class MusicPlayerService extends Service implements PlaylistClickListener{
    public static MediaPlayer mediaPlayer;
    private final IBinder binder = new LocalBinder();

    private Track currentTrack;

    public static Track playingTrack;


    private AudioManager audioManager;
    private MediaSessionCompat mediaSession;
    public static boolean isPlaying = false;
    public static boolean isPause = false;

    public static List<Track> currentPlaylist;
    private int currentTrackIndex = 0;
    public static boolean isShuffleEnabled = false;


    private static final String CHANNEL_ID = "MusicPlayerChannel";
    public static final String ACTION_PLAY = "ACTION_PLAY";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_NEXT = "ACTION_NEXT";
    public static final String ACTION_PREV = "ACTION_PREV";

    public boolean isPlaying() {
        return false;
    }


    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            // Permanent loss of audio focus (e.g., another app started playing music)
                            //stopPlayback();  // Stop playback
                            currentTrack = currentPlaylist.get(currentTrackIndex);
                            pauseTrack(currentTrack);
                            pauseMusic();
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            // Temporary loss of audio focus (e.g., short notification)
                            //pausePlayback();  // Pause playback
                            break;

                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            // Temporary loss but we can lower volume
                            //lowerVolume();  // Lower playback volume
                            break;

                        case AudioManager.AUDIOFOCUS_GAIN:
                            // Regain audio focus after loss
                            if (!isPlaying) {
                                // Resume playback if it was playing before
                                //resumePlayback();
                                resumeTrack(currentTrack);
                            }
                            break;
                    }
                }
            };

    @Override
    public void onPlaylistClick(Playlist playlist) {

        currentPlaylist = playlist.getTracks();
        //playTrack(currentPlaylist.get(0));
    }



    public class LocalBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }



    private TrackViewModel trackViewModel;

    @Override
    public void onCreate() {
        super.onCreate();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mediaPlayer = new MediaPlayer();
        //MyApp myApp = new MyApp();
        com.music.skiza.MyApp.setMusicPlayerService(this);

        // Notifications to be updated
        //createNotificationChannel();

        //onPlaylistClick();


        // Pres

        // Observe tracklist changes
        TrackViewModel trackViewModel = ((MyApp) getApplication()).getApplicationViewModelProvider().get(TrackViewModel.class);
        trackViewModel.getCurrentTracklist().observeForever(tracklist -> {
            if (tracklist == null || tracklist.isEmpty()) {
                // Fall back to default track list
                tracklist = MusicLibrary.getInstance().getTracks();
            }
            currentPlaylist = tracklist;

            // Automatically play the first track if a new list is loaded
            if (currentPlaylist != null && !currentPlaylist.isEmpty()) {
                //playTrack(currentPlaylist.get(0));
            }
        });


        // Access the application-scoped TrackViewModel
        MyApp app = (MyApp) getApplication();
        trackViewModel = app.getApplicationViewModelProvider().get(TrackViewModel.class);

        // Observe changes in the current tracklist
        trackViewModel.getCurrentPlaylist().observeForever(tracklist -> {
            currentPlaylist = tracklist.getTracks();
            Toast.makeText(getApplicationContext(), "Playlist Changed", Toast.LENGTH_SHORT).show();
        });

        if (selectedPlaylist == null) {
            currentPlaylist = MyApp.getMusicLibrary().getTracks();
        } else {
            currentPlaylist = selectedPlaylist.getTracks();
        }


        if (allTracksPlaylist == null) {
            currentPlaylist = MyApp.getMusicLibrary().getTracks();
            Log.d("MusicPlayerService", "Default Playlist: " + currentPlaylist);
        }


        // Listener for Autoplay
        TrackViewModel finalTrackViewModel = trackViewModel;
        mediaPlayer.setOnCompletionListener(mp -> {
            if (currentTrackIndex < currentPlaylist.size()) {
                playNextTrack();
                finalTrackViewModel.getTrack().getValue();
            } else {
                currentTrackIndex = 0;  // Reset to the first track or handle differently
            }
        });


        // For Notifications
        // Initialize the MediaSession
        mediaSession = new MediaSessionCompat(this, "MusicPlayerService");

        // Set the flags to handle media buttons and transport controls
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        // Set an initial PlaybackState
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_STOP |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1f)
                .build();
        mediaSession.setPlaybackState(playbackState);

        // Set the callback to handle media button actions on notifications
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                super.onPlay();
                playMusic();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseMusic();
            }

            @Override
            public void onStop() {
                super.onStop();
                stopMusic();
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNextSong();
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPreviousSong();
            }
        });

        // Set the session as active
        mediaSession.setActive(true);
    }


    public void playPlaylist(List<Track> tracks) {
        if (tracks != null && !tracks.isEmpty()) {
            currentPlaylist = tracks;
            currentTrackIndex = 0;
            playTrack(currentPlaylist.get(currentTrackIndex));
        }else {

        }
    }


    public void playTrack(Track track) {
        if (requestAudioFocus()) {  // To only play if audio focus is granted

            // initializing the media player
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(track.getFilePath());
                mediaPlayer.prepare();
                mediaPlayer.start();

                currentTrack = track;

                currentPlaylist = selectedPlaylist.getTracks();
                int index = currentPlaylist.indexOf(track);
                setCurrentTrackIndex(index);


                Log.d("MusicPlayerService", "Playing track: " + track.getTitle());
                // Start foreground notification (handled in the next section)
                //startForegroundService(track);

                //startForegroundServiceWithNotification();
            } catch (Exception e) {
                Log.e("MusicPlayerService", "Error playing track", e);
            }


            showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);


            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .build());

        }
    }

    public void pauseTrack(Track track) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            Log.d("MusicPlayerService", "Track paused: " + track.getTitle());
        }

        showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .build());
    }

    public void resumeTrack(Track track) {
        if (requestAudioFocus()){

            mediaPlayer.start();
            currentTrack = track;

            Log.d("MusicPlayerService", "Playing track: " + track.getTitle());

            showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                    .build());
        }
    }

    public void skipToNextTrack() {

        //List<Track> tracks = MyApp.getMusicLibrary().getTracks();

        currentPlaylist = selectedPlaylist.getTracks();

        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        if (isShuffleEnabled) {
            currentTrackIndex = new Random().nextInt(currentPlaylist.size());
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size();
        }
        playTrack(currentPlaylist.get(currentTrackIndex));


        playingTrack = currentPlaylist.get(currentTrackIndex);
        //For tests
        //Toast.makeText(getApplicationContext(), "Playing next track: " + playingNext.getTitle(), Toast.LENGTH_SHORT).show();

        showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

    }

    public void playNextTrack() {

        //List<Track> tracks = MyApp.getMusicLibrary().getTracks();
        currentPlaylist = selectedPlaylist.getTracks();

        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        if (isShuffleEnabled) {
            currentTrackIndex = new Random().nextInt(currentPlaylist.size());
        } else {
            currentTrackIndex = (currentTrackIndex + 1) % currentPlaylist.size();
        }
        playTrack(currentPlaylist.get(currentTrackIndex));


        playingTrack = currentPlaylist.get(currentTrackIndex);
        //For tests
        //Toast.makeText(getApplicationContext(), "Playing next track: " + playingNext.getTitle(), Toast.LENGTH_SHORT).show();


    }

    public void skipToPreviousTrack() {

        currentPlaylist = selectedPlaylist.getTracks();

        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        if (isShuffleEnabled) {
            currentTrackIndex = new Random().nextInt(currentPlaylist.size());
        } else {
            currentTrackIndex = (currentTrackIndex - 1 + currentPlaylist.size()) % currentPlaylist.size();
        }
        playTrack(currentPlaylist.get(currentTrackIndex));

        playingTrack = currentPlaylist.get(currentTrackIndex);

        showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

    }

    public void shuffleTracks(boolean enableShuffle) {
        isShuffleEnabled = enableShuffle;
        if (isShuffleEnabled) {
            Collections.shuffle(currentPlaylist);
            Toast.makeText(getApplicationContext(), "Shuffle enabled", Toast.LENGTH_SHORT).show();
        } else {
            // Optionally, reset playlist order to the original order in MusicLibrary
            currentPlaylist = MyApp.getMusicLibrary().getTracks();  // or specific playlist
            Toast.makeText(getApplicationContext(), "Shuffle disabled", Toast.LENGTH_SHORT).show();
        }

        showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

    }

    // Method to update the currently playing track index
    public void setCurrentTrackIndex(int index) {
        if (index >= 0 && index < currentPlaylist.size()) {
            currentTrackIndex = index;
        }
    }

    private void repeatTrack() {
    }

    public void stopTrack() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            releaseAudioFocus();
            Log.d("MusicPlayerService", "Track stopped");
        }
    }


    // Handle audio focus for the service
    // Request audio focus
    private boolean requestAudioFocus() {
        int result = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    // Release audio focus
    private void releaseAudioFocus() {
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }



    // For fade in and out during autoplay
    private Handler fadeHandler = new Handler();

    private void fadeVolume(final MediaPlayer mediaPlayer, final boolean fadeOut, final int durationMs) {
        final int fadeSteps = 20; // Number of steps to fade
        final int delayMs = durationMs / fadeSteps; // Delay between steps
        final float maxVolume = 1.0f;
        final float minVolume = 0.0f;

        fadeHandler.post(new Runnable() {
            int step = fadeOut ? fadeSteps : 0;

            @Override
            public void run() {
                float volume = fadeOut
                        ? maxVolume * (step / (float) fadeSteps)
                        : minVolume + (maxVolume * (step / (float) fadeSteps));

                mediaPlayer.setVolume(volume, volume);

                if (fadeOut) {
                    step--;
                } else {
                    step++;
                }

                if ((fadeOut && step >= 0) || (!fadeOut && step <= fadeSteps)) {
                    fadeHandler.postDelayed(this, delayMs);
                } else if (fadeOut && step < 0) {
                    mediaPlayer.pause(); // Pause when fade out is complete
                }
            }
        });
    }






    //For notifications
    public void playMusic() {
        if (requestAudioFocus()) {  // Only play if audio focus is granted

            Track track;
            mediaPlayer.start();
            isPlaying = true;

            // Update notification (if you have one)
            //showNotification("Paused: Sample Audio", "Artist Name", R.drawable.album_art ,isPlaying);

            // Update MediaSession state to playing
            mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition(), 1f)
                    .setActions(
                            PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                    PlaybackStateCompat.ACTION_PAUSE |
                                    PlaybackStateCompat.ACTION_STOP)
                    .build());
        }
    }

    public void pauseMusic() {
        mediaPlayer.pause();
        isPlaying = false;

        // Update notification (if you have one)
        //showNotification("Paused: Sample Audio", "Artist Name", R.drawable.album_art, isPlaying);

        // Update MediaSession state to paused
        mediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition(), 1f)
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                                PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_STOP)
                .build());
    }

    private void stopMusic() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            releaseAudioFocus();
            Log.d("MusicPlayerService", "Track stopped");
        }
    }

    private void skipToNextSong() {
    }

    private void skipToPreviousSong() {
    }


    /*
    // Working notification function
    public void showNotification(String title, String artist, int albumArtResource, boolean isPlaying) {
        // Call updateMetadata to set the title, artist, and album art
        updateMetadata(title, artist, albumArtResource);

        // Create the custom notification layout
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
        notificationLayout.setTextViewText(R.id.notification_title, title);
        notificationLayout.setTextViewText(R.id.notification_text, artist);

        // Set the play/pause icon based on the state
        int icon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_play;
        notificationLayout.setImageViewResource(R.id.play_pause_button, icon);

        // Working notification
        // Pending intent to open the app when the notification is clicked
        Intent intent = new Intent(this, HomeActivity.class);

        if (isPlaying){
            currentTrack = currentPlaylist.get(currentTrackIndex);
        }

        intent.putExtra("data_key", "Service notification clicked"); // Add data to the intent
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        // Set up the play/pause action
        Intent playIntent = new Intent(this, NotificationReceiver.class);
        playIntent.setAction("ACTION_PLAY_PAUSE");
        PendingIntent playPendingIntent = PendingIntent.getBroadcast(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE);
        notificationLayout.setOnClickPendingIntent(R.id.play_pause_button, playPendingIntent);

        // Create and configure the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music) // Small icon to show in the status bar
                .setContentTitle(title)  // Song title
                .setContentText(artist)  // Artist name
                .setCustomContentView(notificationLayout) // Set the custom layout
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle()) // Use custom view style
                .setOnlyAlertOnce(true) // Update notification without alerting
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Set priority
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())  // Link to the MediaSession
                        .setShowActionsInCompactView(0)  // Show play/pause button in compact view
                        .setShowCancelButton(true))      // Show a cancel button on the notification
                .setOnlyAlertOnce(true)
                .addAction(new NotificationCompat.Action(
                        icon, isPlaying ? "Pause" : "Play", playPendingIntent)); // Add play/pause action

        // Display the notification
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());

        startForeground(1, builder.build()); //notificationHelper.buildNotification(track, true));
    }
    */



    public void showNotification(String title, String artist, int albumArtResource, boolean isPlaying) {
        updateMetadata(title, artist, albumArtResource);

        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), albumArtResource);

        // Create custom notification layout
        RemoteViews notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_layout);
        notificationLayout.setTextViewText(R.id.notification_title, title);
        notificationLayout.setTextViewText(R.id.notification_text, artist);

        // Set album art
        notificationLayout.setImageViewBitmap(R.id.notification_album_art, albumArt);

        // Set play/pause icon based on state
        int playPauseIcon = isPlaying ? R.drawable.ic_pause : R.drawable.ic_plays;
        notificationLayout.setImageViewResource(R.id.play_pause_button, playPauseIcon);

        notificationLayout.setImageViewResource(R.id.next_button, R.drawable.ic_next_ml);
        notificationLayout.setImageViewResource(R.id.previous_button, R.drawable.ic_previous_ml);


        // Intents for next, previous, and play/pause actions
        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent prevIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, NotificationReceiver.class).setAction(ACTION_PREV), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPauseIntent = PendingIntent.getBroadcast(this, 0,
                new Intent(this, NotificationReceiver.class).setAction(isPlaying ? ACTION_PAUSE : ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE);

        // Assign button actions
        notificationLayout.setOnClickPendingIntent(R.id.next_button, nextIntent);
        notificationLayout.setOnClickPendingIntent(R.id.previous_button, prevIntent);
        notificationLayout.setOnClickPendingIntent(R.id.play_pause_button, playPauseIntent);


        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music) // Icon in status bar
                .setCustomContentView(notificationLayout) // Custom notification layout
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle()) // Custom style
                .setOnlyAlertOnce(true) // Avoid repeated alerts
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Notification priority
                .setOngoing(isPlaying) // Sticky notification if playing
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2)); // Show actions in compact view

        // Display the notification
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
        startForeground(1, builder.build());
    }


    // Working notification
    private Notification createNotification() {
        Intent mainIntent = new Intent(this, HomeActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE);

        PendingIntent playIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class).setAction(ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pauseIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT), PendingIntent.FLAG_IMMUTABLE);
        PendingIntent prevIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, NotificationReceiver.class).setAction(ACTION_PREV), PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("Now Playing")
                .setContentText("Track name - Artist name")
                .setContentIntent(mainPendingIntent)
                .setOnlyAlertOnce(true)
                .addAction(R.drawable.ic_previous, "Previous", prevIntent)
                .addAction(R.drawable.ic_play, "Play", playIntent)
                .addAction(R.drawable.ic_pause, "Pause", pauseIntent)
                .addAction(R.drawable.ic_next, "Next", nextIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .build();
    }

    private void updateNotification() {
        Notification notification = createNotification();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, notification);
        } else {
            Log.e("Notification Error", "NotificationManager is null.");
        }

    }

    private void createNotificationChannel() {
        NotificationChannel channel = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            channel = new NotificationChannel(CHANNEL_ID, "MUSIC_CHANNEL", NotificationManager.IMPORTANCE_LOW);
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(channel);
        }
    }

    // Define album art and other metadata information for notification
    /*
    private void updateMetadata(String title, String artist, int albumArtResource) {
        // Load the album art as a Bitmap
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), albumArtResource);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .build());
    }
    */


    public void updateMetadata(String title, String artist, int albumArtResource) {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), albumArtResource);

        if (albumArt == null) {
            albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder_cover); // Fallback
        }

        // Update media metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .build());

        // Define actions for playback state
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY |
                                PlaybackStateCompat.ACTION_PAUSE |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                        isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                        mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0,
                        1f
                )
                .build();

        // Update playback state in MediaSession
        mediaSession.setPlaybackState(playbackState);

        // Set MediaSession callbacks to handle actions
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                playMusic();
            }

            @Override
            public void onPause() {
                pauseMusic();
                showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
            }

            @Override
            public void onSkipToNext() {
                skipToNextTrack();

                showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
            }

            @Override
            public void onSkipToPrevious() {
                skipToPreviousTrack();
                showNotification(playingTrack.getTitle(), playingTrack.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
            }
        });
    }





    public Track getCurrentTrack() {
        return currentTrack;
    }


    private void handleAction(String action) {
        switch (action) {
            case ACTION_PLAY:
                // Play logic here
                break;
            case ACTION_PAUSE:
                // Pause logic here
                break;
            case ACTION_NEXT:
                // Next track logic here
                break;
            case ACTION_PREV:
                // Previous track logic here
                break;
        }

        updateNotification();
    }



    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (mediaPlayer != null) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            mediaSession.setActive(false);
        }

        if (fadeHandler != null) {
            fadeHandler.removeCallbacksAndMessages(null);
        }

        // Remove observers to avoid memory leaks
        if (trackViewModel != null) {
            trackViewModel.getCurrentTracklist().removeObserver(tracklist -> {});
            //trackViewModel.getCurrentTrack().removeObserver(track -> {});
        }
    }


}
