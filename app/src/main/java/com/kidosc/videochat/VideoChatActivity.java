package com.kidosc.videochat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.Process;
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
    private static final int FINISH = 4;
    private static final int MONITOR_CHECK = 5;

    private RelativeLayout mInCallRl;
    private RelativeLayout mCallOutRl;
    private ImageView mIvEndCall;
    private ImageView mIvIncallEnd;
    private ImageView mAnswerIv;
    private ImageView mIvChangeCamera;
    private RelativeLayout mVideoChatingView;
    private VideoChatView mPlayerView;
    private RelativeLayout.LayoutParams volumeParams;

    private VideoChatManager mVideoChatManager;
    private RefusalReceiver mRefusalReceiver;
    private AudioManager mAudioManager;
    private SettingsObserver mObserver;

    private String mContactPhoto;
    private String mChatName;
    private String mChatId;
    private String mRoomToken;
    private String mImei;
    private int mMyId;
    private int mType;

    private boolean isAnswer = false;
    private volatile boolean isFinish = false;

    private int maxVolume;
    private int currentVolume;
    private VideoChatInfo videoChatInfo;

    /**
     * 电话音量等级
     */
    private int[] mVoiceCallLevelArray = {1, 2, 3, 4, 5};

    private PowerManager.WakeLock mWakelock;
    private SoundPool mSoundPool;
    private int mInCallId;
    private int mEndCallId;

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
                    Toast.makeText(getApplicationContext(), getString(R.string.switch_camera_success), Toast.LENGTH_SHORT).show();
                    break;
                case CHECK_IS_ANSWER:
                    //检查对方是否答应
                    if (!isAnswer) {
                        mVideoChatManager.onEndCall(mMyId, mChatId);
                        finishThePage(FINISH);
                    }
                    break;
                case MONITOR_CHECK:
                    finishThePage(FINISH);
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate 10fps");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initIntent(getIntent());
        initVideoSettings();
        initAudio();
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
            mWakelock.acquire(60 * 1000);
        }
        mObserver = new SettingsObserver(mHandler);
        getContentResolver().registerContentObserver(Constant.SETTING_URI, true, mObserver);
        Log.d(TAG, "onCreate end");
    }

    /**
     * 解析intent的参数
     *
     * @param intent intent
     */
    private void initIntent(Intent intent) {
        videoChatInfo = intent.getParcelableExtra(Constant.EXTRA_VIDEO_INFO);
        mContactPhoto = videoChatInfo.photo;
        mChatName = videoChatInfo.name;
        mType = videoChatInfo.chatType;
        mChatId = videoChatInfo.senderId;
        String myId = videoChatInfo.myId;
        if (!TextUtils.isEmpty(myId)) {
            mMyId = Integer.parseInt(myId);
        }
        mRoomToken = videoChatInfo.roomToken;
        mImei = videoChatInfo.imei;
        Log.d(TAG, videoChatInfo.toString());
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
        mSoundPool = new SoundPool(2, AudioManager.STREAM_SYSTEM, 0);
        mInCallId = mSoundPool.load(VideoChatActivity.this, R.raw.video_chat, 0);
        //play in call sound
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (sampleId == mInCallId) {
                    int result = mSoundPool.play(mInCallId, 1f, 1f, 0, -1, 1);
                    Log.d(TAG, "play incall :" + result + "  mInCallId :  " + mInCallId);
                }
            }
        });
    }

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
        if (Constant.IS_QINIU) {
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
        mIvIncallEnd = (ImageView) findViewById(R.id.iv_incall_end_call);
        mAnswerIv.setOnClickListener(this);
        mIvIncallEnd.setOnClickListener(this);
        Utils.setDefaultBackgroundPhoto(mContactPhoto, bgIncall);
        TextView tvName = (TextView) findViewById(R.id.tv_incall_name);
        tvName.setText(mChatName);
        Log.d(TAG, "call in : " + mChatName);
        if (!Constant.IS_QINIU) {
            //一开始 接听界面是假的 ，按钮先屏蔽
            mAnswerIv.setEnabled(false);
            mAnswerIv.setClickable(false);
            mIvIncallEnd.setEnabled(false);
            mIvIncallEnd.setClickable(false);
        }
        mHandler.sendEmptyMessageDelayed(CHECK_IS_ANSWER, 60 * 1000);
    }

    /**
     * 初始化后台配置
     */
    private void initVideoSettings() {
        mVideoChatManager = new VideoChatManager();
        mVideoChatManager.init(this, videoChatInfo);
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
        if (!Constant.IS_QINIU) {
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
        //Settings.Global.putInt(getContentResolver(), "video_status", 1);
        mVideoChatManager.resume();
        Intent intent = new Intent("com.kidosc.videochat.ischating");
        sendBroadcast(intent);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_incall_answer_call:
                //来电 接听
                Log.d(TAG, "click inCall answer");
                mAnswerIv.setEnabled(false);
                mAnswerIv.setClickable(false);
                mVideoChatManager.onAnswerCall(mRoomToken);
                break;
            case R.id.iv_incall_end_call:
                //来电 挂断
                mVideoChatManager.onEndCall(mMyId, mChatId);
                finishThePage(FINISH);
                break;
            case R.id.iv_out_end_call:
                //拨出 挂断
                mVideoChatManager.onHangUp(mMyId, mChatId);
                finishThePage(FINISH);
                break;
            case R.id.iv_ing_end_call:
                //正在通话中 挂断
                mVideoChatManager.onEndCall();
                finishThePage(FINISH);
                break;
            case R.id.video_chating:
                //正在通话
                if (mIvEndCall.getVisibility() != View.VISIBLE) {
                    mIvEndCall.setVisibility(View.VISIBLE);
                } else {
                    mIvEndCall.setVisibility(View.INVISIBLE);
                }
                break;
            case R.id.iv_change_camera:
                //切换camera
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
        if (Constant.IS_QINIU) {
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
        //停止 声音
        mSoundPool.stop(mInCallId);
        boolean unload = mSoundPool.unload(mInCallId);
        Log.d(TAG, "unload : " + unload);
        Log.d(TAG, "updateRemoteView");
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
            if (!Constant.IS_QINIU) {
                mInCallRl.addView(remoteView, params);
            }
            mInCallRl.addView(mVideoChatingView, params);
            mInCallRl.addView(mPlayerView, volumeParams);
        } else if (mType == Constant.CALLOUT) {
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(mCallOutRl.getWidth(), mCallOutRl.getHeight());
            if (!Constant.IS_QINIU) {
                mCallOutRl.addView(remoteView, params);
            }
            mCallOutRl.addView(mVideoChatingView, params);
            mCallOutRl.addView(mPlayerView, volumeParams);
        }
        mVideoChatManager.startCallInfoTimer(tvTime);
    }

    @Override
    public void onCallRemove(SurfaceView surfaceView) {
        finishThePage(FINISH);
    }

    @Override
    public void onCallInAdd() {
        mAnswerIv.setClickable(true);
        mAnswerIv.setEnabled(true);
        mIvIncallEnd.setClickable(true);
        mIvIncallEnd.setEnabled(true);
    }

    @Override
    public void onCallOutAdd() {
    }

    @Override
    public void onLoginFailed() {
        if (mType == Constant.INCALL) {
            mIvIncallEnd.setClickable(true);
            mIvIncallEnd.setEnabled(true);
        }
    }

    /**
     * 结束页面
     *
     * @param finishType 结束类型
     */
    private void finishThePage(final int finishType) {
        Log.d(TAG, "finishThePage");
        if (isFinish) {
            return;
        }
        isFinish = true;
        //结束页面
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //finish 页面
                if (finishType == FINISH) {
                    VideoChatActivity.this.finish();
                } else if (finishType == -1) {
                    VideoChatActivity.this.onDestroy();
                }
            }
        }, 2000);
        //播放挂断音乐
        mSoundPool.stop(mInCallId);
        mEndCallId = mSoundPool.load(this, R.raw.end_call, 0);
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (sampleId == mEndCallId) {
                    int result = mSoundPool.play(mEndCallId, 0.5f, 0.5f, 0, 0, 1);
                    Log.d(TAG, "play end call : " + result);
                }
            }
        });
    }

    @Override
    public void onLoginJc() {
        if (mType == Constant.CALLOUT) {
            mVideoChatManager.call(mMyId, mChatId);
        }
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
            if (vol < mVoiceCallLevelArray[1]) {
                vol = mVoiceCallLevelArray[1];
            } else if (vol >= mVoiceCallLevelArray[1] && vol < mVoiceCallLevelArray[2]) {
                vol = mVoiceCallLevelArray[2];
            } else if (vol >= mVoiceCallLevelArray[2] && vol < mVoiceCallLevelArray[3]) {
                vol = mVoiceCallLevelArray[3];
            } else if (vol >= mVoiceCallLevelArray[3]) {
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

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        mVideoChatManager.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (mVideoChatManager != null) {
            mVideoChatManager.destroy();
            mVideoChatManager = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        if (mRefusalReceiver != null) {
            unregisterReceiver(mRefusalReceiver);
        }
        if (mWakelock != null) {
            mWakelock.release();
            mWakelock = null;
        }
        mSoundPool.release();
        if (mObserver != null) {
            getContentResolver().unregisterContentObserver(mObserver);
        }
        Process.killProcess(Process.myPid());
        System.exit(10);
    }

    class RefusalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive action:" + action);
            VideoChatInfo videoChatInfo = intent.getParcelableExtra(Constant.EXTRA_VIDEO_INFO);
            if (videoChatInfo == null) {
                return;
            }
            if (!videoChatInfo.senderId.equals(mChatId)) {
                return;
            }
            Log.d(TAG, "info : " + videoChatInfo.toString() + "\n currentID:" + mChatId);
            if (Constant.REFUSAL_CHAT_ACTION.equals(action)) {
                finishThePage(FINISH);
            } else if (Constant.ACTION_CLASS_BAGIN.equals(action)) {
                finishThePage(-1);
            }
        }
    }

    /**
     * 监听settings数据库
     */
    class SettingsObserver extends ContentObserver {

        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            int monitor = -1;
            Cursor cursor = getContentResolver().query(Constant.SETTING_URI, null, null
                    , null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    monitor = cursor.getInt(cursor.getColumnIndex(Constant.WATCH_CALL));
                    Log.d(TAG, "monitor = " + monitor);
                }
                cursor.close();
            }
            //正在监听
            if (monitor == 1) {
                Utils.showDialog(VideoChatActivity.this, mHandler);
            }
        }
    }
}
