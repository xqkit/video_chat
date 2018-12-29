package com.kidosc.videochat.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kidosc.videochat.event.VideoChatEvent;
import com.kidosc.videochat.utils.SPUtil;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.model.AVChatCommonEvent;
import com.netease.nimlib.sdk.avchat.model.AVChatData;

import org.greenrobot.eventbus.EventBus;

/**
 * Desc:    视频聊天service
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/12/27 15:21
 */

public class VideoChatService extends Service {

    private static final String TAG = VideoChatService.class.getSimpleName();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        //打开Rtc模块
        if (AVChatManager.getInstance().enableRtc(true)) {
            Log.d(TAG, "初始化Rtc成功");
        } else {
            Log.d(TAG, "初始化Rtc失败");
        }
        AVChatManager.getInstance().observeIncomingCall(incomingCallObserver, true);
        AVChatManager.getInstance().observeHangUpNotification(callHangUpObserver, true);
        return Service.START_STICKY;
    }

    private Observer<AVChatData> incomingCallObserver = new Observer<AVChatData>() {
        @Override
        public void onEvent(AVChatData data) {
            SPUtil.putLong(getBaseContext(), "chat_id", data.getChatId());
            Log.d(TAG, "Extra Message->" + data.getChatId());
        }
    };

    private Observer<AVChatCommonEvent> callHangUpObserver = new Observer<AVChatCommonEvent>() {
        @Override
        public void onEvent(AVChatCommonEvent avChatCommonEvent) {
            Log.d(TAG, "callHangUp : " + avChatCommonEvent.getChatId());
            EventBus.getDefault().post(new VideoChatEvent(0, avChatCommonEvent.getChatId()));
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        AVChatManager.getInstance().observeIncomingCall(incomingCallObserver, false);
        AVChatManager.getInstance().observeHangUpNotification(callHangUpObserver, false);
        super.onDestroy();
    }
}
