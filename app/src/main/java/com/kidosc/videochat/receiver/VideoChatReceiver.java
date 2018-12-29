package com.kidosc.videochat.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kidosc.videochat.constants.Constant;
import com.kidosc.videochat.service.VideoChatService;

/**
 * Desc:
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/28 17:28
 */

public class VideoChatReceiver extends BroadcastReceiver {
    private static final String TAG = VideoChatReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "action : " + intent.getAction());
        if (Constant.IS_C4) {
            Intent intent1 = new Intent(context, VideoChatService.class);
            context.startService(intent1);
        }
    }
}
