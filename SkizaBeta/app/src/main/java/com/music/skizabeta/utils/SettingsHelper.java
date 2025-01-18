package com.music.skizabeta.utils;

import android.content.SharedPreferences;
import android.content.Context;

public class SettingsHelper {
    private static final String PREFS_NAME = "MusicAppSettings";
    private static final String KEY_AUDIO_QUALITY = "audio_quality";
    private static final String KEY_THEME = "theme";
    private static final String KEY_SHUFFLE = "shuffle";

    private SharedPreferences sharedPreferences;

    public SettingsHelper(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Save audio quality
    public void saveAudioQuality(String audioQuality) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_AUDIO_QUALITY, audioQuality);
        editor.apply();
    }

    // Get audio quality
    public String getAudioQuality() {
        return sharedPreferences.getString(KEY_AUDIO_QUALITY, "default_quality"); // Default value
    }

    // Save theme preference
    public void saveTheme(String theme) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_THEME, theme);
        editor.apply();
    }

    // Get theme preference
    public String getTheme() {
        return sharedPreferences.getString(KEY_THEME, "light"); // Default value
    }

    // Save shuffle preference
    public void saveShufflePreference(boolean isShuffle) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_SHUFFLE, isShuffle);
        editor.apply();
    }

    // Get shuffle preference
    public boolean isShuffleEnabled() {
        return sharedPreferences.getBoolean(KEY_SHUFFLE, false); // Default value
    }
}
