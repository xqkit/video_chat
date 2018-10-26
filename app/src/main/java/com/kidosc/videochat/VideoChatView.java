package com.kidosc.videochat;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Desc:    视频通话专属view
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/10/25 15:26
 */

public class VideoChatView extends FrameLayout {

    private VolumeProgress mVolumeProgress;
    private Handler uiHandler;

    public VideoChatView(Context context) {
        super(context);
        initView();
    }

    public VideoChatView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public VideoChatView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.video_chat_view, this, false);
        mVolumeProgress = view.findViewById(R.id.volume_progress);
        addView(view);
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mVolumeProgress.setVisibility(INVISIBLE);
        }
    };

    public void setVolumeView(final int volume) {
        mVolumeProgress.setProgress(volume);
        if (mVolumeProgress.getVisibility() != VISIBLE) {
            mVolumeProgress.setVisibility(VISIBLE);
        }
        uiHandler.removeCallbacks(runnable);
        uiHandler.postDelayed(runnable, 1000);
    }

    public void setHandler(Handler handler) {
        uiHandler = handler;
    }
}
