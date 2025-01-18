package com.music.skizabeta.adapters;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.music.skizabeta.MyApp;
import com.music.skizabeta.R;
import com.music.skizabeta.listeners.TrackClickListener;
import com.music.skizabeta.models.Track;
import com.music.skizabeta.services.MusicPlayerService;

import java.util.List;
import java.util.function.Consumer;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    public static TrackClickListener listener;
    private final List<Track> tracks;
    private final Consumer<Track> onItemLongClick;

    public TrackAdapter(List<Track> tracks, TrackClickListener listener, Consumer<Track> onItemLongClick) {
        this.tracks = tracks;
        this.listener = listener;
        this.onItemLongClick = onItemLongClick;
    }


    @NonNull
    @Override
    public TrackAdapter.TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_track, parent, false);
        return new TrackAdapter.TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackAdapter.TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }


    //new
    public void setTracks(List<Track> tracks) {
    }
    //

    class TrackViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView artistTextView;
        private TextView trackDuration;

        private Button addButton, removeButton;

        public TrackViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.track_title);
            artistTextView = itemView.findViewById(R.id.track_artist);
            trackDuration = itemView.findViewById(R.id.track_duration);
            addButton = itemView.findViewById(R.id.addButton);
            removeButton = itemView.findViewById(R.id.removeButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onTrackClick(tracks.get(position));
                    trackDuration.setVisibility(View.VISIBLE);
                    removeButton.setVisibility(View.GONE);
                }
            });

            // Handle long press to enter multi-select mode
            itemView.setOnLongClickListener(v -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    onItemLongClick.accept(tracks.get(getAdapterPosition()));
                    //addButton.setVisibility(View.VISIBLE);
                    trackDuration.setVisibility(View.GONE);
                    removeButton.setVisibility(View.VISIBLE);
                } else{
                    trackDuration.setVisibility(View.VISIBLE);
                    removeButton.setVisibility(View.GONE);
                }
                return true;
            });
        }

        public void bind(Track track) {
            titleTextView.setText(track.getTitle());
            artistTextView.setText(track.getArtist().getName());
            trackDuration.setText(formatTime(track.getDuration()));
        }


        private String formatTime(long millis) {
            long minutes = millis / 1000 / 60;
            long seconds = (millis / 1000) % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }


    }
}
