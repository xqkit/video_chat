package com.kidosc.videochat;

import android.os.Build;

/**
 * Desc:
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/12 16:59
 */

public class Constant {
    /**
     * 列表传递的数据
     */
    public static final String REFUSAL_CHAT_ACTION = "com.kidosc.videochat.refusal";
    public static final String EXTRA_VIDEO_INFO = "ExtraVideoInfo";
    public static final String ACTION_CLASS_BAGIN = "com.zeusis.socket.class.ban.bagin";

    public static final String JC_APP_KEY = "75e06df8717cb0df36b35097";
    public static final String AUDE = "AUDE";
    public static final boolean IS_AUDE = Build.MODEL.equals(Constant.AUDE);
    public static final int VIDEO_BITRATE = 1000;
    public static final int VIDEO_MAX_BITRATE = 10 * 1000;
    /**
     * 硬编码
     */
    public static final boolean IS_HW_CODEC = false;
    public static final int VIDEO_CHAT_WIDTH = 640;
    public static final int VIDEO_CHAT_HEIGHT = 480;

    /**
     * 协议地址
     */
    public static final String PROTOCOL = "http://devcapp.artimen.cn:7001";
    public static final String PROTOCOL_CALL_OUT = "/Service/VideoCallService.asmx/WVideoCallOut";
    public static final String PROTOCOL_CALL_REFUSAL = "/Service/VideoCallService.asmx/WRefusalVideoCall";
    public static final String PROTOCOL_CALL_TEMPERATURE = "/Service/VideoCallService.asmx/WTemperatureNotify";
    public static final String PROTOCOL_CALL_HANGUP = "/Service/VideoCallService.asmx/WHangUpVideoCall";
    public static final int INCALL = 0;
    public static final int CALLOUT = 2;
}