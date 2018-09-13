package com.kidosc.videochat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;

/**
 * Desc:    视频聊天界面
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/5 18:01
 */

public class VideoChatActivity extends Activity implements View.OnClickListener, CallItemListener {

    private static final String TAG = "VideoChatManager";
    private static final int CALL_UPDATE = 0;
    private String mContactPhoto;
    private String mChatName;
    private ImageView mIvEndCall;
    private int mType;
    private String mChatId;
    private RelativeLayout mInCallRl;
    private RelativeLayout mCallOutRl;
    private RelativeLayout mVideoChatingView;
    private VideoChatManager mVideoChatManager;
    private TextView mTvTime;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CALL_UPDATE:
                    updateRemoteView((SurfaceView) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initIntent(getIntent());
        mVideoChatManager = VideoChatManager.getInstance();
        Log.d(TAG, "mType : " + mType);
        mVideoChatingView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.video_chat_ing, null);
        QNRemoteSurfaceView qnRemoteSurfaceView = (QNRemoteSurfaceView) mVideoChatingView.findViewById(R.id.qnr_remote);
        mVideoChatManager.setRemoteView(qnRemoteSurfaceView);
        QNLocalSurfaceView qnLocalSurfaceView = (QNLocalSurfaceView) mVideoChatingView.findViewById(R.id.qnr_local);
        mVideoChatManager.setLocalView(qnLocalSurfaceView);
        if (mType == 0) {
            //来电
            setContentView(R.layout.video_chat_incall);
            mInCallRl = (RelativeLayout) findViewById(R.id.rl_video_bg);
            findViewById(R.id.iv_incall_answer_call).setOnClickListener(this);
            findViewById(R.id.iv_incall_end_call).setOnClickListener(this);
            Utils.setDefaultBackgroundPhoto(mContactPhoto, mInCallRl);
            TextView tvName = (TextView) findViewById(R.id.tv_incall_name);
            tvName.setText(mChatName);
        } else if (mType == 2) {
            //拨号
            setContentView(R.layout.video_chat_call_out);
            mCallOutRl = (RelativeLayout) findViewById(R.id.rl_call_out_bg);
            findViewById(R.id.iv_out_end_call).setOnClickListener(this);
            TextView tvName = (TextView) findViewById(R.id.tv_out_name);
            tvName.setText(mChatName);
            mVideoChatManager.call(Constant.ROOM_TOKEN);
        }
        mVideoChatManager.setCallListener(this);
    }

    private void initIntent(Intent intent) {
        mContactPhoto = intent.getStringExtra(Constant.EXTRA_CHAT_PHOTO);
        mChatName = intent.getStringExtra(Constant.EXTRA_CHAT_NAME);
        mType = intent.getIntExtra(Constant.EXTRA_CHAT_TYPE, -1);
        mChatId = intent.getStringExtra(Constant.EXTRA_CHAT_ID);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent");
        initIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_incall_answer_call:
                mVideoChatManager.onAnswerCall();
                break;
            case R.id.iv_incall_end_call:
            case R.id.iv_out_end_call:
            case R.id.iv_ing_end_call:
                mVideoChatManager.onEndCall();
                finish();
                break;
            case R.id.qnr_remote:
                if (mIvEndCall.getVisibility() != View.VISIBLE) {
                    mIvEndCall.setVisibility(View.VISIBLE);
                } else {
                    mIvEndCall.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.iv_change_camera:
                mVideoChatManager.switchCamera();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCallUpdate(SurfaceView surfaceView) {
        Log.d(TAG, "onCallUpdate");
        if (Constant.IS_AUDE) {
            mHandler.sendEmptyMessage(CALL_UPDATE);
            return;
        }
        updateRemoteView(surfaceView);
    }

    private void updateRemoteView(SurfaceView remoteView) {
        Log.d(TAG, "updateRemoteView");
        mVideoChatingView.findViewById(R.id.video_chating).setOnClickListener(this);
        ImageView ivChangeCamera = (ImageView) mVideoChatingView.findViewById(R.id.iv_change_camera);
        TextView tvTime = (TextView) mVideoChatingView.findViewById(R.id.tv_ing_time);
        if (!Constant.AUDE.equals(Build.MODEL)) {
            ivChangeCamera.setVisibility(View.INVISIBLE);
        } else {
            ivChangeCamera.setOnClickListener(this);
        }
        if (mIvEndCall == null) {
            mIvEndCall = (ImageView) mVideoChatingView.findViewById(R.id.iv_ing_end_call);
            mIvEndCall.setOnClickListener(this);
        }
        if (mType == 0) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mInCallRl.getWidth(), mInCallRl.getHeight());
            if (!Constant.IS_AUDE) {
                mInCallRl.addView(remoteView, params);
            }
            mInCallRl.addView(mVideoChatingView, params);
        } else if (mType == 2) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCallOutRl.getWidth(), mCallOutRl.getHeight());
            if (!Constant.IS_AUDE) {
                mCallOutRl.addView(remoteView, params);
            }
            mCallOutRl.addView(mVideoChatingView, params);
        }
        mVideoChatManager.startCallInfoTimer(tvTime);
    }

    @Override
    public void onCallRemove(SurfaceView surfaceView) {
        finish();
    }
}
