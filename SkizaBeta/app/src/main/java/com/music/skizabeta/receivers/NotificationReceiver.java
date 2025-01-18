package com.music.skizabeta.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.music.skizabeta.R;
import com.music.skizabeta.models.Track;
import com.music.skizabeta.services.MusicPlayerService;


public class NotificationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        MusicPlayerService service = new MusicPlayerService(); // Assuming you have a singleton instance

        if (service != null) {
            switch (action) {
                case MusicPlayerService.ACTION_NEXT:
                    service.skipToNextTrack();
                    break;
                case MusicPlayerService.ACTION_PREV:
                    service.skipToPreviousTrack();
                    break;
                case MusicPlayerService.ACTION_PLAY:
                    service.playMusic();
                    break;
                case MusicPlayerService.ACTION_PAUSE:
                    service.pauseMusic();
                    break;
            }
            // Update notification after handling the action
            Track currentTrack = service.getCurrentTrack();
            service.showNotification(currentTrack.getTitle(), currentTrack.getArtist().getName(),
                    R.drawable.album_art, service.isPlaying());
        }
    }
}
