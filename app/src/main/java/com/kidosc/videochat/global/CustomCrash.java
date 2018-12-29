package com.kidosc.videochat.global;

import android.content.Context;
import android.os.Process;
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
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    void setCrashInfo() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }
}
