package com.music.skizabeta.helpers;


import static androidx.core.app.ServiceCompat.startForeground;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import androidx.core.app.NotificationCompat;

import com.music.skiza.R;
import com.music.skiza.activities.MainActivity;
import com.music.skiza.models.Track;
import com.music.skiza.receivers.NotificationReceiver;

public class MusicNotificationHelper {


    private Context context;

    public MusicNotificationHelper(Context context) {
        this.context = context;
    }

    public Notification buildNotification(Track track, boolean isPlaying) {
        // Pending intent to open the app when the notification is clicked
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Play/Pause action
        Intent playPauseIntent = new Intent(context, NotificationReceiver.class);
        playPauseIntent.setAction(isPlaying ? "ACTION_PAUSE" : "ACTION_PLAY");
        PendingIntent playPausePendingIntent = PendingIntent.getBroadcast(context, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play,
                isPlaying ? "Pause" : "Play",
                playPausePendingIntent
        );

        // Build notification
        return new NotificationCompat.Builder(context, "MUSIC_CHANNEL")
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist().getName())
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.album_art)) // Placeholder image
                .setContentIntent(pendingIntent)
                .addAction(playPauseAction)
                //.setStyle(new androidx.media.app.NotificationCompat.MediaStyle())
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1))
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
        //.build();

                /*
                .setSmallIcon(R.drawable.ic_music_note)
                .setContentTitle("Now Playing")
                //.addAction(R.drawable.ic_previous, "Previous", getPendingIntent("PREVIOUS"))
                //.addAction(R.drawable.ic_play, "Play", playPendingIntent)
                //.addAction(R.drawable.ic_pause, "Pause", pausePendingIntent)
                //.addAction(R.drawable.ic_next, "Next", getPendingIntent("NEXT"))
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(1))
                .setOngoing(true);

        startForeground(1, builder.build());
        */

        // Build notification

    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(context, NotificationReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


}

