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
    public final static String EXTRA_CHAT_TYPE = "ExtraChatType";
    public final static String EXTRA_CHAT_PRIMARYKEY = "ExtraChatPrimarykey";
    public final static String EXTRA_CHAT_ID = "ExtraChatId";
    public final static String EXTRA_CHAT_PHOTO = "ExtraChatPhoto";
    public final static String EXTRA_CHAT_NAME = "ExtraChatName";
    public static final String EXTRA_MY_ID = "ExtraMyId";
    public static final String EXTRA_ROOM_TOKEN = "roomToken";

    public static final String JC_APP_KEY = "75e06df8717cb0df36b35097";
    public static final String AUDE = "AUDE";
    public static final boolean IS_AUDE = Build.MODEL.equals(Constant.AUDE);
    public static final int VIDEO_BITRATE = 15;
    public static final int VIDEO_MAX_BITRATE = 30;
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
}