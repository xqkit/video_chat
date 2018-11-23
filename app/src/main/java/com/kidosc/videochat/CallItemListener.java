package com.kidosc.videochat;

import android.view.SurfaceView;

/**
 * Desc:
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/11 10:44
 */

public interface CallItemListener {
    /**
     * run on main thread
     *
     * @param surfaceView surfaceView
     */
    void onCallUpdate(SurfaceView surfaceView, int status);

    /**
     * remove page
     */
    void onCallRemove(String reason);

    void onCallInAdd();

    void onLoginJc();

    void onCallOutAdd();

    void onLoginFailed();

    void onCallWait();
}