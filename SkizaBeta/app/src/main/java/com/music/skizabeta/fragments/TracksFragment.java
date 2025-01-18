package com.music.skizabeta.fragments;

import static com.music.skizabeta.activities.HomeActivity.getPlaylistById;
import static com.music.skizabeta.activities.HomeActivity.isBound;
import static com.music.skizabeta.fragments.PlaylistFragment.allTracks;
import static com.music.skizabeta.activities.HomeActivity.selectedPlaylist;
import static com.music.skizabeta.services.MusicPlayerService.currentPlaylist;
import static com.music.skizabeta.services.MusicPlayerService.playingTrack;
import static com.music.skizabeta.services.MusicPlayerService.playingTrack;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.music.skizabeta.MyApp;
import com.music.skizabeta.R;
import com.music.skizabeta.adapters.TrackAdapter;
import com.music.skizabeta.adapters.TracksSelectionAdapter;
import com.music.skizabeta.listeners.TrackClickListener;
import com.music.skizabeta.listeners.TrackFinished;
import com.music.skizabeta.listeners.TrackSelected;
import com.music.skizabeta.models.MusicLibrary;
import com.music.skizabeta.models.Playlist;
import com.music.skizabeta.models.Track;
import com.music.skizabeta.models.TrackViewModel;
import com.music.skizabeta.services.MusicPlayerService;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public class TracksFragment extends Fragment implements TrackClickListener, TrackFinished {

    TrackAdapter adapter;
    TracksSelectionAdapter selectionAdapter;

    private RecyclerView trackRecyclerView;
    private static final int REQUEST_CODE = 1;
    private String trackId;

    private List<Track> trackList;
    private List<Track> mainTrackList;

    private Track currentTrack;

    private MusicPlayerService musicService;

    public TracksFragment() {}


    private TextView trackTitle, trackArtist, miniPlayerTitle;
    private ImageButton playPauseButton, previousButton, nextButton, shuffleButton, miniPlayPause;

    private static BottomSheetBehavior<View> bottomSheetBehavior;
    private static LinearLayout miniPlayer;
    private static LinearLayout expandedPlayer;

    private boolean isPlaying = false;

    // For seekBar
    private SeekBar seekBar;
    private TextView currentTimeTextView, totalDurationTextView;
    private boolean isUserSeeking = false;
    private Handler handler;


    private ImageView albumArtImageView, miniAlbumArtImageView;
    private TrackViewModel trackViewModel;


    private RecyclerView tracksRecyclerView;
    private Button addSelectedTracksBtn;

    private Consumer<List<Track>> onTracksSelected; // Callback to pass selected tracks to PlaylistFragment
    private boolean isMultiSelectMode = false; // Flag to check if multi-selection is active

    private List<Playlist> playlist;

    /* Works Fine can be used to bind to MusicPlayerService
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Intent intent = new Intent(context, MusicPlayerService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    */

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracks, container, false);

        // Define a Handler to manage periodic updates fir seekBar
        handler = new Handler();

        // Retrieve MusicPlayerService
        musicService = MyApp.getMusicPlayerService();

        selectedPlaylist = getPlaylistById("all_tracks");

        //Initializing play/mini fragment the views
        trackTitle = view.findViewById(R.id.tvTrackTitle);
        trackArtist = view.findViewById(R.id.tvArtistName);
        playPauseButton = view.findViewById(R.id.btnPlayPause);

        miniPlayer = view.findViewById(R.id.mini_player);
        expandedPlayer = view.findViewById(R.id.expanded_player);

        previousButton = view.findViewById(R.id.btnPrevious);
        nextButton = view.findViewById(R.id.btnNext);
        shuffleButton = view.findViewById(R.id.btnShuffle);

        miniPlayerTitle = view.findViewById(R.id.mini_player_title);
        miniPlayPause = view.findViewById(R.id.mini_player_play_pause);

        addSelectedTracksBtn = view.findViewById(R.id.addSelectedTracksButton);


        // Initialize the SeekBar
        seekBar = view.findViewById(R.id.seekBar);
        currentTimeTextView = view.findViewById(R.id.current_time);
        totalDurationTextView = view.findViewById(R.id.total_duration);

        View bottomSheet = getActivity().findViewById(R.id.bottom_sheet_play_fragment);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);

        setupBottomSheetBehavior();

        Log.d("TracksFragment", "Started TracksFragment");

        // Initialize RecyclerView
        trackRecyclerView = view.findViewById(R.id.rvTracks);
        tracksRecyclerView = view.findViewById(R.id.tracksRecyclerView);
        trackRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        miniAlbumArtImageView = view.findViewById(R.id.mini_player_cover);
        albumArtImageView = view.findViewById(R.id.expanded_cover);

        // Load tracks from storage and add to MusicLibrary
        //loadTracks();

        /*
        Playlist allTracksPlaylist = MyApp.getMusicLibrary().getPlaylists()
                .stream()
                .filter(p -> p.getId().equals("all_tracks"))
                .findFirst()
                .orElse(null);

        // Obtain the ViewModel
        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.setCurrentPlaylist(allTracksPlaylist);

        if (allTracksPlaylist != null) {
            mainTrackList = allTracksPlaylist.getTracks();
            currentPlaylist = mainTrackList;
            // Use the tracks as needed
        }
        */


        currentPlaylist = selectedPlaylist.getTracks();

        selectedPlaylist = getPlaylistById("all_tracks");
        trackList = selectedPlaylist.getTracks(); //MyApp.getMusicLibrary().getTracks();

        // Initialize the adapter and set it to the RecyclerView
        //adapter = new TrackAdapter(trackList, this);
        adapter = new TrackAdapter(trackList, this, this::onTrackLongClick);
        trackRecyclerView.setAdapter(adapter);



        // Add Selected Tracks Button
        addSelectedTracksBtn.setOnClickListener(v -> {
            if (isMultiSelectMode) {
                if (onTracksSelected != null) {
                    List<Track> selectedTracks = selectionAdapter.getSelectedTracks();
                    if (!selectedTracks.isEmpty()) {
                        onTracksSelected.accept(selectedTracks); // Pass selected tracks
                    }
                    Log.d("TracksFragment", "Selected Tracks: " + selectedTracks.size());
                }
                exitMultiSelectMode();
                getParentFragmentManager().popBackStack(); // Return to PlaylistFragment
            }
        });


        playPauseButton.setOnClickListener(v -> {
            // Retrieve the selected track from the arguments
            togglePlayback();
        });

        miniPlayPause.setOnClickListener(v -> {
            togglePlayback();
        });

        previousButton.setOnClickListener(v -> {
            SkipToPreviousTrack();
        });

        nextButton.setOnClickListener(v -> {
            SkipToNextTrack();
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




        return view;
    }

    public static void setupBottomSheetBehavior() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        // Toggle between mini and expanded views based on the bottom sheet state
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {

                    miniPlayer.setVisibility(View.GONE);
                    expandedPlayer.setVisibility(View.VISIBLE);

                    //Update SeekBar
                    //updateSeekBarUI(track);

                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    miniPlayer.setVisibility(View.VISIBLE);
                    expandedPlayer.setVisibility(View.GONE);
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
    public void onTrackClick(Track track) {

        // Retrieve MusicPlayerService
        musicService = MyApp.getMusicPlayerService();

        currentTrack = track;
        playTrack();

        updateTrackInfo(currentTrack);

        //Update SeekBar
        updateSeekBarUI(track);


        // Obtain the ViewModel (Working ViewModel)
        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.setTrack(currentTrack);


        /*
        MusicLibrary musicLibrary = MyApp.getMusicLibrary();
        Playlist allTracksPlaylist = musicLibrary.getPlaylists()
                .stream()
                .filter(p -> p.getId().equals("all_tracks"))
                .findFirst()
                .orElse(null);

        // Obtain the ViewModel
        trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        trackViewModel.setCurrentPlaylist(allTracksPlaylist);

        if (allTracksPlaylist != null) {
            List<Track> allTracks = allTracksPlaylist.getTracks();
            // Use the tracks as needed
        }
        */

        /*
        // Display track details or initiate playback
        trackTitle.setText(track.getTitle());
        trackArtist.setText(track.getArtist().getName());
        miniPlayerTitle.setText(track.getTitle());

        // Load album art
        if (track.getAlbumArtPath() != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(track.getAlbumArtPath());
            albumArtImageView.setImageBitmap(bitmap);
            miniAlbumArtImageView.setImageBitmap(bitmap);
        } else {
            albumArtImageView.setImageResource(R.drawable.placeholder_cover); // Default placeholder
        }

         */

        // Observe current track
        //trackViewModel = new ViewModelProvider(requireActivity()).get(TrackViewModel.class);
        //trackViewModel.getTrack().observe(getViewLifecycleOwner(), this::updateTrackInfo);

        playPauseButton.setImageResource(R.drawable.ic_pause);// Set the initial play/pause button state

        miniPlayPause.setImageResource(R.drawable.ic_pause);// Set the initial play/pause button state

        /*
        // Create a bundle to pass the selected track
        Bundle bundle = new Bundle();
        bundle.putString("trackId", track.getId());

        // Play the selected track
        if (musicService != null) {
            if (MusicPlayerService.isPlaying) {

                //pauseMusic
                musicService.stopTrack();
                MusicPlayerService.isPlaying = false;
                musicService.playTrack(track);
                MusicPlayerService.isPlaying = true;
                musicService.showNotification(track.getTitle(), track.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);

            } else if (MusicPlayerService.isPause && !MusicPlayerService.isPlaying) {

                //resumeMusic
                musicService.stopTrack();
                MusicPlayerService.isPlaying = false;
                musicService.playTrack(track);
                MusicPlayerService.isPlaying = true;
                musicService.showNotification(track.getTitle(), track.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
            } else {

                //playMusic
                MusicPlayerService.isPlaying = true;
                musicService.playTrack(track);
                musicService.showNotification(track.getTitle(), track.getArtist().getName(), R.drawable.album_art, MusicPlayerService.isPlaying);
            }

        } else {
            Log.e("TracksFragment", "MusicPlayerService is null");

        }
         */


        /*
        PlayFragment playFragment = new PlayFragment();
        playFragment.setArguments(bundle);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, playFragment)
                .addToBackStack(null)
                .commit();
        */


        /*
        FragmentTransaction transection=getFragmentManager().beginTransaction();
        SecondFragment mfragment=new SecondFragment();
        //using Bundle to send data
        Bundle bundle=new Bundle();
        bundle.putString("email",memail);
        bundle.putString("user_name",muser_name);
        bundle.putString("phone",mphone_number);
        mfragment.setArguments(bundle); //data being send to SecondFragment
        transection.replace(R.id.main_fragment, mfragment);
        transection.commit();



        trackList = MyApp.getMusicLibrary().getTracks();
        int position = trackList.indexOf(track);
        // Get the clicked track
        Track selectedTrack = MyApp.getMusicLibrary().getTracks().get(position);

        // Play the selected track
        MusicPlayerService musicService = MyApp.getMusicPlayerService();
        if (musicService != null) {
            musicService.playTrack(selectedTrack);
        }

        trackId = selectedTrack.getId();
        // Retrieve MusicPlayerService
        musicService = MyApp.getMusicPlayerService();

        // Retrieve the MusicLibrary and find the track
        track = MyApp.getMusicLibrary().findTrackById(trackId);
        if (track != null) {

            //MusicPlayerService binder = (MusicPlayerService) musicService;
            //musicService = MusicPlayerService.LocalBinder.getService();
            if (musicService != null) {

                musicService.playTrack(track);
            } else {
                Log.e("PlayActivity", "MusicPlayerService is null");

            }
        }
        */
    }

    @Override
    public void onTrackLongClick(Track track) {
        if (!isMultiSelectMode) {

            addSelectedTracksBtn.setVisibility(View.VISIBLE);

            // Switch to multi-selection mode
            isMultiSelectMode = true;

            trackList = MyApp.getMusicLibrary().getTracks();
            selectionAdapter = new TracksSelectionAdapter(trackList);
            trackRecyclerView.setAdapter(selectionAdapter);
            selectionAdapter.selectTrack(track);

        }


        // Initialize Adapter with multi-selection
        // selectionAdapter = new TracksSelectionAdapter(trackList);
        // trackRecyclerView.setAdapter(adapter);
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

        MusicPlayerService.isPlaying = true;
        musicService.playTrack(currentTrack);
        miniPlayerTitle.setText(currentTrack.getTitle());

        // Retrieve the MusicLibrary and find the track
        //Track track = MyApp.getMusicLibrary().findTrackById(currentTrack);

        /*
        // Retrieve the MusicLibrary and find the track
        Track track = MyApp.getMusicLibrary().findTrackById(trackId);
        try {
            Uri albumArtUri = Uri.parse(track.getAlbumArtPath());
            ContentResolver contentResolver = context.getContentResolver();// requireContext().getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(albumArtUri);

            if (inputStream != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                albumArtImageView.setImageBitmap(bitmap);
                miniAlbumArtImageView.setImageBitmap(bitmap);
                inputStream.close();
            } else {
                albumArtImageView.setImageResource(R.drawable.placeholder_cover);

                miniAlbumArtImageView.setImageResource(R.drawable.album_art);
                Log.e("PlayFragment", "Failed to open album art URI.");
            }
        } catch (Exception e) {
            albumArtImageView.setImageResource(R.drawable.placeholder_cover);
            miniAlbumArtImageView.setImageResource(R.drawable.placeholder_cover);
            Log.e("PlayFragment", "Error loading album art: " + e.getMessage(), e);
        }
         */

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

    private void SkipToPreviousTrack(){
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

    private void SkipToNextTrack(){
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

    private void updateTrackInfo(Track currentTrack) {

        // Display track title and artist name
        trackTitle.setText(currentTrack.getTitle());
        trackArtist.setText(currentTrack.getArtist().getName());

        Log.e("TracksFragment", "Failed to open album art URI." + currentTrack.getAlbumArtPath());
        try {
            Uri albumArtUri = Uri.parse(currentTrack.getAlbumArtPath());
            ContentResolver contentResolver = requireContext().getContentResolver();// requireContext().getContentResolver();
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

    /*
    private void updateTrackInfo(Track track) {

        if (track == null) return;

        trackTitle.setText(track.getTitle());
        trackArtist.setText(track.getArtist().getName());
        miniPlayerTitle.setText(track.getTitle());

        String albumArtPath = track.getAlbumArtPath();
        if (albumArtPath != null) {
            File file = new File(albumArtPath);
            if (file.exists()) {
                Log.d("PlayFragment", "Album art path is valid: " + albumArtPath);
            } else {
                Log.e("PlayFragment", "Album art path is invalid or does not exist: " + albumArtPath);
            }
        } else {
            Log.e("PlayFragment", "Album art path is null.");
        }

        // Load album art
        if (track.getAlbumArtPath() != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(track.getAlbumArtPath());
            albumArtImageView.setImageBitmap(bitmap);
            miniAlbumArtImageView.setImageBitmap(bitmap);
        } else {
            albumArtImageView.setImageResource(R.drawable.placeholder_cover); // Default placeholder
        }
    }
    */

    /*
    private void updateTrackInfo(Track track) {
        if (track == null) return;

        trackTitle.setText(track.getTitle());
        trackArtist.setText(track.getArtist().getName());
        miniPlayerTitle.setText(track.getTitle());

        try {
            if (track.getAlbumArtPath() != null) {
                Uri albumArtUri = Uri.parse(track.getAlbumArtPath());
                ContentResolver resolver = requireContext().getContentResolver();
                InputStream inputStream = resolver.openInputStream(albumArtUri);

                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    albumArtImageView.setImageBitmap(bitmap);
                    inputStream.close();
                } else {
                    albumArtImageView.setImageResource(R.drawable.placeholder_cover);
                    Log.e("PlayFragment", "Failed to load album art.");
                }
            } else {
                albumArtImageView.setImageResource(R.drawable.placeholder_cover);
                Log.e("PlayFragment", "Album art URI is null.");
            }
        } catch (Exception e) {
            albumArtImageView.setImageResource(R.drawable.placeholder_cover);
            Log.e("PlayFragment", "Error loading album art: " + e.getMessage(), e);
        }
    }



    private void updateAlbumArt(Track track){

        if (track.getAlbumArtPath() != null && !track.getAlbumArtPath().isEmpty()) {
            Log.d("AlbumArt", "Album Art URI: " + track.getAlbumArtPath());
            Bitmap bitmap = BitmapFactory.decodeFile(track.getAlbumArtPath());
            if (bitmap != null) {
                albumArtImageView.setImageBitmap(bitmap);
                miniAlbumArtImageView.setImageBitmap(bitmap);
            } else {
                albumArtImageView.setImageResource(R.drawable.placeholder_cover);
                miniAlbumArtImageView.setImageResource(R.drawable.placeholder_cover);
                Log.e("PlayFragment", "Failed to decode album art.");
            }
        } else {
            albumArtImageView.setImageResource(R.drawable.placeholder_cover);
            miniAlbumArtImageView.setImageResource(R.drawable.placeholder_cover);
            Log.e("PlayFragment", "Album art path is null or empty.");
        }
    }

     */


    // Updating SeekBar

    private void updateSeekBarUI(Track track) {

        // Reset SeekBar
        seekBar.setProgress(0);
        seekBar.setMax(100); // SeekBar max is a percentage

        // Update total duration TextView
        //totalDurationTextView.setText(formatTime(musicService.getDuration()));

        // Start updating the SeekBar
        handler.post(updateSeekBar);
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
                } else if (formatTime(currentPosition).equals(formatTime(duration))){
                    onTrackFinished();
                    currentPosition = musicService.getCurrentPosition();
                    duration = musicService.getDuration();

                    seekBar.setProgress(0);
                    seekBar.setProgress((int) ((currentPosition / (float) duration) * 100));
                    currentTimeTextView.setText(formatTime(currentPosition));
                    totalDurationTextView.setText(formatTime(duration));
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




    private void exitMultiSelectMode() {
        isMultiSelectMode = false;
        tracksRecyclerView.setAdapter(adapter);  // Switch back to single selection mode
    }

    public void setOnTracksSelectedListener(Consumer<List<Track>> listener) {
        this.onTracksSelected = listener;
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
}