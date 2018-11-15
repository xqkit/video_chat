package com.kidosc.videochat;

import android.net.Uri;
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
     * settings数据库
     */
    private static final String SETTING_TABLE_NAME = "watch_setting";
    private static final String AUTHORITIES = "com.zeusis.zscontactsprovider";
    public static final Uri SETTING_URI = Uri.parse("content://" + AUTHORITIES + "/" + SETTING_TABLE_NAME);
    public static final String WATCH_CALL = "watchCall";

    /**
     * 协议地址
     */
    public static final String PROTOCOL_CALL_OUT = "/Service/VideoCallService.asmx/WVideoCallOut";
    public static final String PROTOCOL_CALL_REFUSAL = "/Service/VideoCallService.asmx/WRefusalVideoCall";
    public static final String PROTOCOL_CALL_HANGUP = "/Service/VideoCallService.asmx/WHangUpVideoCall";
    public static final int INCALL = 0;
    public static final int CALLOUT = 2;
}