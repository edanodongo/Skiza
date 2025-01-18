package com.music.skizabeta.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.music.skizabeta.R;
import com.music.skizabeta.helpers.StorageHelper;
import com.music.skizabeta.listeners.TrackClickListener;
import com.music.skizabeta.models.Track;

import java.util.ArrayList;
import java.util.List;

public class TracksSelectionAdapter extends RecyclerView.Adapter<TracksSelectionAdapter.TrackViewHolder> {

    private List<Track> tracks;
    private List<Track> selectedTracks = new ArrayList<>();
    private StorageHelper storageHelper;

    public TracksSelectionAdapter(List<Track> tracks) {
        this.tracks = tracks;
    }

    public List<Track> getSelectedTracks() {
        return selectedTracks;
    }

    // Updated selectTrack method to prevent duplicates and trigger UI update
    public void selectTrack(Track track) {
        if (!selectedTracks.contains(track)) {
            selectedTracks.add(track);

            //selectedPlaylist.getTracks().addAll(selectedTracks);
            //storageHelper.savePlaylists(playlists);

            notifyItemChanged(tracks.indexOf(track)); // Update specific track UI
        }
    }

    public void deselectTrack(Track track) {
        if (selectedTracks.contains(track)) {
            selectedTracks.remove(track);
            notifyItemChanged(tracks.indexOf(track)); // Update specific track UI
        }
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track_selectable, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {

        TextView trackTitle;
        CheckBox selectCheckBox;

        TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            trackTitle = itemView.findViewById(R.id.trackTitle);
            selectCheckBox = itemView.findViewById(R.id.selectCheckBox);
        }

        void bind(Track track) {
            trackTitle.setText(track.getTitle());
            selectCheckBox.setOnCheckedChangeListener(null); // Prevent issues with recycled views
            selectCheckBox.setChecked(selectedTracks.contains(track));

            selectCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedTracks.add(track);
                    selectTrack(track);
                } else {
                    selectedTracks.remove(track);
                    deselectTrack(track);
                }

            });


            // Handle click outside the checkbox for selection
            itemView.setOnClickListener(v -> {
                boolean newCheckedState = !selectCheckBox.isChecked();
                selectCheckBox.setChecked(newCheckedState);
            });
        }
    }


}