package com.kidosc.videochat;

import android.content.Context;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

/**
 * Desc:    全局捕获异常类
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/10/22 14:32
 */

public class CustomCrash implements Thread.UncaughtExceptionHandler {

    Context context;

    public CustomCrash(Context context) {
        this.context = context;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.e("VideoChatManager", e.getMessage());
        Settings.Global.putInt(context.getContentResolver(), "video_chat", 0);
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    void setCrashInfo() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
}
