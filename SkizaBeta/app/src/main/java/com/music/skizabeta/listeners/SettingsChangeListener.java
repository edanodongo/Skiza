package com.music.skizabeta.listeners;

public interface SettingsChangeListener {
    void onSettingsChanged(String audioQuality, boolean isShuffle);
}