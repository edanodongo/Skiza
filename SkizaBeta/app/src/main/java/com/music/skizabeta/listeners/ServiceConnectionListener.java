package com.music.skizabeta.listeners;

import android.content.ComponentName;
import android.os.IBinder;

public interface ServiceConnectionListener {

    void onServiceConnected(ComponentName name, IBinder binder);

    void onServiceDisconnected(ComponentName name);
}

