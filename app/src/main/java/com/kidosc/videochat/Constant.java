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
    public static final boolean IS_QINIU = Build.MODEL.equals(Constant.AUDE);

    /**
     * 协议地址
     */
    public static final String PROTOCOL_CALL_OUT = "/Service/VideoCallService.asmx/WVideoCallOut";
    public static final String PROTOCOL_CALL_REFUSAL = "/Service/VideoCallService.asmx/WRefusalVideoCall";
    public static final String PROTOCOL_CALL_HANGUP = "/Service/VideoCallService.asmx/WHangUpVideoCall";
    public static final int INCALL = 0;
    public static final int CALLOUT = 2;
}