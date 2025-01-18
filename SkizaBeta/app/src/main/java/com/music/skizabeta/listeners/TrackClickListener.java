package com.music.skizabeta.listeners;

import com.music.skizabeta.models.Track;

public interface TrackClickListener {
    void onTrackClick(Track track);
    void onTrackLongClick(Track track);

}
