package com.kidosc.videochat;

import android.app.Application;
import android.os.Handler;

/**
 * Desc:
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/12 17:13
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        VideoChatManager videoChatManager = VideoChatManager.getInstance();
        videoChatManager.init(getApplicationContext());
        videoChatManager.initVideoChat();
    }
}
