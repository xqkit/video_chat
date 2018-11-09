package com.kidosc.videochat;

import android.app.Application;

/**
 * Desc:    global application
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/12 17:13
 */

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CustomCrash customCrash = new CustomCrash(this);
        customCrash.setCrashInfo();
    }
}