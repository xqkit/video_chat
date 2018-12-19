package com.kidosc.videochat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class SPUtil {
    private static final String TAG = SPUtil.class.getSimpleName();
    private static String DEFAULT_SP_NAME = "video_chat";

    private static SharedPreferences getDefaultSharedPreferences(Context context) {
        return context.getSharedPreferences(DEFAULT_SP_NAME, Context.MODE_PRIVATE);
    }

    /**
     * 初始化标记的SP区域
     *
     * @param name 区域名字
     */
    public static void initASpName(String name) {
        if (name != null) {
            Log.i(TAG, "initASpName: " + name);
            DEFAULT_SP_NAME = name;
        }
    }

    public static String getDefaultSPName() {
        return DEFAULT_SP_NAME;
    }

    public static boolean putInt(Context context, String key, int value) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        Editor edit = sharedPreferences.edit();
        edit.putInt(key, value);
        return edit.commit();
    }

    public static int getInt(Context context, String key) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(key, 0);
    }

    public static int getInt(Context context, String key, int defValue) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getInt(key, defValue);
    }

    public static boolean putString(Context context, String key, String value) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        Editor edit = sharedPreferences.edit();
        edit.putString(key, value);
        return edit.commit();
    }

    public static String getString(Context context, String key) {
        return getString(context, key, null);
    }

    public static boolean putLong(Context context, String key, long value) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        Editor edit = sharedPreferences.edit();
        edit.putLong(key, value);
        return edit.commit();
    }

    public static Long getLong(Context context, String key) {
        return getLong(context, key, 0);
    }

    public static String getString(Context context, String key, String defValue) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getString(key, defValue);
    }

    public static Long getLong(Context context, String key, long defValue) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getLong(key, defValue);
    }

    public static boolean putBoolean(Context context, String key, boolean value) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        Editor edit = sharedPreferences.edit();
        edit.putBoolean(key, value);
        return edit.commit();
    }

    public static boolean getBoolean(Context context, String key, boolean defValue) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean(key, defValue);
    }

    public static boolean remove(Context context, String key) {
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(context);
        Editor edit = sharedPreferences.edit();
        edit.remove(key);
        return edit.commit();
    }

    private static final String RADIO_BACKGROUND = "radiofrequency_background";

    public static void saveRFIString(Context context, String key, String value) {
        SharedPreferences sp = context.getSharedPreferences(RADIO_BACKGROUND, Context.MODE_PRIVATE);
        sp.edit().putString(key, value).commit();
    }

    public static String getRFIString(Context context, String key, String defaultValue) {
        SharedPreferences sp = context.getSharedPreferences(RADIO_BACKGROUND, Context.MODE_PRIVATE);
        return sp.getString(key, defaultValue);
    }
    // RFI(射频干扰测试) ****end***


}
