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
    public static final String EXTRA_CHAT_ACTION = "com.zeusis.action.CHAT";
    public final static String EXTRA_CHAT_TYPE = "ExtraChatType";
    public final static String EXTRA_CHAT_PRIMARYKEY = "ExtraChatPrimarykey";
    public final static String EXTRA_CHAT_ID = "ExtraChatId";
    public final static String EXTRA_CHAT_PHOTO = "ExtraChatPhoto";
    public final static String EXTRA_CHAT_NAME = "ExtraChatName";

    public static final String JC_APP_KEY = "75e06df8717cb0df36b35097";
    public static final String F1 = "F1";
    public static final String AUDE = "AUDE";
//    public static final boolean IS_AUDE = Build.MODEL.equals(Constant.AUDE);
    public static final boolean IS_AUDE = true;
    public static final int VIDEO_BITRATE = 15;
    public static final int VIDEO_MAX_BITRATE = 30;
    /**
     * 硬编码
     */
    public static final boolean IS_HWCodec = false;
    public static final int VIDEO_CHAT_WIDTH = 640;
    public static final int VIDEO_CHAT_HEIGHT = 480;
    public static final String ROOM_TOKEN = "ydkscVt7Soh1rNSEDSRetyYw5XKUHlmg40U9FG5M:BcP0OlrDbCU6GKWxtZDAN1ddMOE=:eyJhcHBJZCI6ImRvdG5qMXBqeCIsInJvb21OYW1lIjoiZnJhbmsiLCJ1c2VySWQiOiJ5b2xvIiwiZXhwaXJlQXQiOjE1MzY5MDUyODcsInBlcm1pc3Npb24iOiJ1c2VyIn0=";
    public static final String UID = "uid";
}
