package com.kidosc.videochat;

import android.app.Application;
import android.util.Log;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.util.NIMUtil;

/**
 * Desc:    global application
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/12 17:13
 */

public class MyApplication extends Application {
    private static final String TAG = MyApplication.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        CustomCrash customCrash = new CustomCrash(this);
        customCrash.setCrashInfo();
        if (Constant.IS_C4) {
            // SDK初始化（启动后台服务，若已经存在用户登录信息， SDK 将完成自动登录）
//            LoginInfo info = loginInfo();
            NIMClient.init(this, null, null);
            // ... your codes
            if (NIMUtil.isMainProcess(this)) {
                Log.d(TAG, "init success");
                // 注意：以下操作必须在主进程中进行
                // 1、UI相关初始化操作
                // 2、相关Service调用
            }
        }
    }


    // 如果已经存在用户登录信息，返回LoginInfo，否则返回null即可
    /*private LoginInfo loginInfo() {
        // 从本地读取上次登录成功时保存的用户登录信息
        String account = "kidosc";
        String token = videoChatInfo.roomToken;
        Log.d(TAG,"token " + token);
        if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
            return new LoginInfo(account, token);
        } else {
            return null;
        }
    }*/

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}