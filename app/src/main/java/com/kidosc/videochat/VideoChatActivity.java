package com.kidosc.videochat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.zeusis.videochat.VideoChatInfo;

/**
 * Desc:    视频聊天界面
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/5 18:01
 */

public class VideoChatActivity extends Activity implements View.OnClickListener, CallItemListener {

    private static final String TAG = "VideoChatManager";
    private static final String LOCK_TAG = "kido_video_chat";
    private static final int CALL_UPDATE = 0;
    private static final int SWITCH_CAMERA = 2;
    private static final int CHECK_IS_ANSWER = 3;

    private RelativeLayout mInCallRl;
    private RelativeLayout mCallOutRl;
    private ImageView mIvEndCall;
    private ImageView mAnswerIv;
    private ImageView mIvChangeCamera;
    private RelativeLayout mVideoChatingView;
    private VideoChatView mPlayerView;
    private RelativeLayout.LayoutParams volumeParams;

    private VideoChatManager mVideoChatManager;
    private RefusalReceiver mRefusalReceiver;
    private AudioManager mAudioManager;

    private String mContactPhoto;
    private String mChatName;
    private String mChatId;
    private String mRoomToken;
    private String mImei;

    private boolean isAnswer = false;

    private int mMyId;
    private int mType;
    private int maxVolume;
    private int currentVolume;
    /**
     * 媒体音量等级 musicVolume:11,systemVolume:5
     */
    private int[] mVoiceCallLevelArray = {0, 1, 3, 4, 5};
    /**
     * 0 1 2 3 4 系统音量等级
     */
    private int[] mSettingLevelArray = {0, 1, 3, 5, 7};

    private PowerManager.WakeLock mWakelock;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d(TAG, "msg.what : " + msg.what);
            switch (msg.what) {
                case CALL_UPDATE:
                    //更新远程视频
                    updateRemoteView((SurfaceView) msg.obj);
                    break;
                case SWITCH_CAMERA:
                    //切换camera
                    mIvChangeCamera.setClickable(true);
                    mIvChangeCamera.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "切换成功", Toast.LENGTH_SHORT).show();
                    break;
                case CHECK_IS_ANSWER:
                    //检查对方是否答应
                    if (!isAnswer) {
                        finish();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initIntent(getIntent());
        initVideoSettings();
        mPlayerView = new VideoChatView(this);
        mPlayerView.setHandler(mHandler);
        volumeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.MATCH_PARENT);
        volumeParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        if (mType == Constant.INCALL) {
            //初始化来电界面
            initIncallUi();
        } else if (mType == Constant.CALLOUT) {
            //初始化拨号界面
            initCallOutUi();
        }
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        //亮屏
        if (pm != null) {
            mWakelock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, LOCK_TAG);
            mWakelock.setReferenceCounted(false);
            mWakelock.acquire();
        }
        initAudio();
    }

    /**
     * 解析intent的参数
     *
     * @param intent intent
     */
    private void initIntent(Intent intent) {
        VideoChatInfo info = intent.getParcelableExtra(Constant.EXTRA_VIDEO_INFO);
        mContactPhoto = info.photo;
        mChatName = info.name;
        mType = info.chatType;
        mChatId = info.senderId;
        String myId = info.myId;
        if (!TextUtils.isEmpty(myId)) {
            mMyId = Integer.parseInt(myId);
        }
        mRoomToken = info.roomToken;
        mImei = info.imei;
        Log.d(TAG, info.toString());
    }

    /**
     * 初始化音频，将音频跳到最大
     */
    private void initAudio() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        }
        Log.d(TAG, "maxVolume:" + maxVolume + ",currentVolume:" + currentVolume);
        mAudioManager.requestAudioFocus(focusChangeListener, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
    }

    AudioManager.OnAudioFocusChangeListener focusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.d(TAG, "focusChange : " + focusChange);
        }
    };

    /**
     * 初始化拨号界面
     */
    private void initCallOutUi() {
        setContentView(R.layout.video_chat_call_out);
        mCallOutRl = (RelativeLayout) findViewById(R.id.rl_call_out_bg);
        findViewById(R.id.iv_out_end_call).setOnClickListener(this);
        TextView tvName = (TextView) findViewById(R.id.tv_out_name);
        ImageView bgCallOut = (ImageView) findViewById(R.id.iv_bg_call_out);
        tvName.setText(mChatName);
        if (Constant.IS_AUDE) {
            mVideoChatManager.call(mMyId, mChatId);
        }
        Log.d(TAG, "call out : " + mChatName);
        Utils.setDefaultBackgroundPhoto(mContactPhoto, bgCallOut);
        mHandler.sendEmptyMessageDelayed(CHECK_IS_ANSWER, 60 * 1000);
    }

    /**
     * 初始化来电界面
     */
    private void initIncallUi() {
        setContentView(R.layout.video_chat_incall);
        mInCallRl = (RelativeLayout) findViewById(R.id.rl_video_bg);
        mAnswerIv = (ImageView) findViewById(R.id.iv_incall_answer_call);
        ImageView bgIncall = (ImageView) findViewById(R.id.iv_bg_incall);
        mAnswerIv.setOnClickListener(this);
        findViewById(R.id.iv_incall_end_call).setOnClickListener(this);
        Utils.setDefaultBackgroundPhoto(mContactPhoto, bgIncall);
        TextView tvName = (TextView) findViewById(R.id.tv_incall_name);
        tvName.setText(mChatName);
        Log.d(TAG, "call in : " + mChatName);
        if (!Constant.IS_AUDE) {
            //一开始 接听界面是假的 ，接听按钮先屏蔽
            mAnswerIv.setEnabled(false);
            mAnswerIv.setClickable(false);
        }
    }

    /**
     * 初始化后台配置
     */
    private void initVideoSettings() {
        mVideoChatManager = new VideoChatManager();
        mVideoChatManager.init(this);
        mVideoChatManager.initVideoChat();
        mVideoChatingView = (RelativeLayout) LayoutInflater.from(this).inflate(R.layout.video_chat_ing, null);
        QNRemoteSurfaceView qnRemoteSurfaceView = (QNRemoteSurfaceView) mVideoChatingView.findViewById(R.id.qnr_remote);
        mVideoChatManager.setRemoteView(qnRemoteSurfaceView);
        QNLocalSurfaceView qnLocalSurfaceView = (QNLocalSurfaceView) mVideoChatingView.findViewById(R.id.qnr_local);
        mVideoChatManager.setLocalView(qnLocalSurfaceView);
        mVideoChatManager.setCallListener(this);
        //注册对方主动挂断广播
        mRefusalReceiver = new RefusalReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constant.REFUSAL_CHAT_ACTION);
        intentFilter.addAction(Constant.ACTION_CLASS_BAGIN);
        registerReceiver(mRefusalReceiver, intentFilter);
        //先登录账号
        if (!Constant.IS_AUDE) {
            mVideoChatManager.loginJcChat(mImei);
        }
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
        Log.d(TAG, "onResume");
        Settings.Global.putInt(getContentResolver(), "video_status", 1);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_incall_answer_call:
                Log.d(TAG, "click inCall answer");
                mAnswerIv.setEnabled(false);
                mAnswerIv.setClickable(false);
                mVideoChatManager.onAnswerCall(mChatId, mRoomToken);
                break;
            case R.id.iv_incall_end_call:
                mVideoChatManager.onEndCall(mMyId, mChatId);
                finish();
                break;
            case R.id.iv_out_end_call:
                mVideoChatManager.onHangUp(mMyId, mChatId);
                finish();
                break;
            case R.id.iv_ing_end_call:
                mVideoChatManager.onEndCall();
                finish();
                break;
            case R.id.video_chating:
                if (mIvEndCall.getVisibility() != View.VISIBLE) {
                    mIvEndCall.setVisibility(View.VISIBLE);
                } else {
                    mIvEndCall.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.iv_change_camera:
                Log.d(TAG, "click iv_change_camera");
                mIvChangeCamera.setClickable(false);
                mIvChangeCamera.setEnabled(false);
                mVideoChatManager.switchCamera();
                mHandler.sendEmptyMessageDelayed(SWITCH_CAMERA, 1500);
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

    /**
     * 更新远程view
     *
     * @param remoteView view
     */
    private void updateRemoteView(SurfaceView remoteView) {
        Log.d(TAG, "updateRemoteView");
        Settings.Global.putInt(getContentResolver(), "video_status", 2);
        isAnswer = true;
        mVideoChatingView.findViewById(R.id.video_chating).setOnClickListener(this);
        mIvChangeCamera = (ImageView) mVideoChatingView.findViewById(R.id.iv_change_camera);
        TextView tvTime = (TextView) mVideoChatingView.findViewById(R.id.tv_ing_time);
        if (!Constant.AUDE.equals(Build.MODEL)) {
            mIvChangeCamera.setVisibility(View.INVISIBLE);
        } else {
            mIvChangeCamera.setOnClickListener(this);
        }
        if (mIvEndCall == null) {
            mIvEndCall = (ImageView) mVideoChatingView.findViewById(R.id.iv_ing_end_call);
            mIvEndCall.setOnClickListener(this);
        }
        if (mType == Constant.INCALL) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mInCallRl.getWidth(), mInCallRl.getHeight());
            if (!Constant.IS_AUDE) {
                mInCallRl.addView(remoteView, params);
            }
            mInCallRl.addView(mVideoChatingView, params);
            mInCallRl.addView(mPlayerView, volumeParams);
        } else if (mType == Constant.CALLOUT) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCallOutRl.getWidth(), mCallOutRl.getHeight());
            if (!Constant.IS_AUDE) {
                mCallOutRl.addView(remoteView, params);
            }
            mCallOutRl.addView(mVideoChatingView, params);
            mCallOutRl.addView(mPlayerView, volumeParams);
        }
        mVideoChatManager.startCallInfoTimer(tvTime);
    }

    @Override
    public void onCallRemove(SurfaceView surfaceView) {
        finish();
    }

    @Override
    public void onCallAdd() {
        mAnswerIv.setClickable(true);
        mAnswerIv.setEnabled(true);
    }

    @Override
    public void onLoginJc() {
        if (mType == Constant.CALLOUT) {
            mVideoChatManager.call(mMyId, mChatId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        Settings.Global.putInt(getContentResolver(), "video_status", 0);
        mAudioManager.abandonAudioFocus(focusChangeListener);
        mVideoChatManager.destroy();
        mVideoChatManager = null;
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mHandler.removeCallbacksAndMessages(null);
        if (mRefusalReceiver != null) {
            unregisterReceiver(mRefusalReceiver);
        }
        if (mWakelock != null) {
            mWakelock.release();
            mWakelock = null;
        }
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        Settings.Global.putInt(getContentResolver(), "video_status", 0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //长按处理
        if (event.getRepeatCount() != 0) {
            return super.onKeyDown(keyCode, event);
        }
        int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        Log.d(TAG, "vol:" + vol + " , keyCode:" + keyCode);
        //back键下调音乐音量，power键上调音乐键
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (vol <= mVoiceCallLevelArray[1]) {
                vol = mVoiceCallLevelArray[0];
            } else if (vol > mVoiceCallLevelArray[1] && vol <= mVoiceCallLevelArray[2]) {
                vol = mVoiceCallLevelArray[1];
            } else if (vol > mVoiceCallLevelArray[2] && vol <= mVoiceCallLevelArray[3]) {
                vol = mVoiceCallLevelArray[2];
            } else if (vol > mVoiceCallLevelArray[3] && vol <= mVoiceCallLevelArray[4]) {
                vol = mVoiceCallLevelArray[3];
            }
        } else if (keyCode == KeyEvent.KEYCODE_POWER) {
            if (vol <= mVoiceCallLevelArray[1]) {
                vol = mVoiceCallLevelArray[2];
            } else if (vol > mVoiceCallLevelArray[1] && vol <= mVoiceCallLevelArray[2]) {
                vol = mVoiceCallLevelArray[3];
            } else if (vol > mVoiceCallLevelArray[2] && vol <= mVoiceCallLevelArray[3]) {
                vol = mVoiceCallLevelArray[4];
            }
        }
        Log.d(TAG, "setVolume vol : " + vol);
        mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, vol, AudioManager.FLAG_VIBRATE);
        if (mPlayerView != null) {
            mPlayerView.setVolumeView(vol);
        }
        return true;
    }

    class RefusalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "onReceive action:" + action);
            if (action != null) {
                if (action.equals(Constant.REFUSAL_CHAT_ACTION)) {
                    finish();
                } else if (action.equals(Constant.ACTION_CLASS_BAGIN)) {
                    onDestroy();
                }
            }
        }
    }
}
