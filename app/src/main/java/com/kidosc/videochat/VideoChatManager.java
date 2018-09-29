package com.kidosc.videochat;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.juphoon.cloud.JCCall;
import com.juphoon.cloud.JCCallCallback;
import com.juphoon.cloud.JCCallItem;
import com.juphoon.cloud.JCClient;
import com.juphoon.cloud.JCClientCallback;
import com.juphoon.cloud.JCMediaDevice;
import com.juphoon.cloud.JCMediaDeviceCallback;
import com.juphoon.cloud.JCMediaDeviceVideoCanvas;
import com.qiniu.droid.rtc.QNCameraSwitchResultCallback;
import com.qiniu.droid.rtc.QNLocalSurfaceView;
import com.qiniu.droid.rtc.QNLogLevel;
import com.qiniu.droid.rtc.QNRTCEnv;
import com.qiniu.droid.rtc.QNRTCManager;
import com.qiniu.droid.rtc.QNRTCSetting;
import com.qiniu.droid.rtc.QNRemoteAudioCallback;
import com.qiniu.droid.rtc.QNRemoteSurfaceView;
import com.qiniu.droid.rtc.QNRoomEventListener;
import com.qiniu.droid.rtc.QNRoomState;
import com.qiniu.droid.rtc.QNStatisticsReport;
import com.qiniu.droid.rtc.QNVideoFormat;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Desc:    视频聊天管理
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/9/11 11:27
 */

public class VideoChatManager implements JCMediaDeviceCallback, JCCallCallback, JCClientCallback, QNRoomEventListener {

    private static final String TAG = VideoChatManager.class.getSimpleName();
    private JCClient client;
    private JCMediaDevice mediaDevice;
    private JCCall call;
    private JCMediaDeviceVideoCanvas mLocal;
    private JCMediaDeviceVideoCanvas mRemote;
    private Timer mCallInfoTimer;
    private JCCallItem mCallItem;
    private Context mContext;
    private QNRTCManager mRTCManager;
    private QNRTCSetting mRTCSetting;
    public CallItemListener mCallListener;

    /**
     * 是否使用10帧
     */
    private boolean mCheck10 = false;
    /**
     * 是否展示本地预览图像
     */
    private boolean mIsShowLocal = false;
    /**
     * 是否50度自动挂断
     */
    private boolean mICheck50d = false;
    private boolean mIsAnswered;
    private boolean mIsJoinedRoom = false;

    private static final String CALL_OUT = Constant.PROTOCOL + Constant.PROTOCOL_CALL_OUT;
    private static final String CALL_TEMPERATURE = Constant.PROTOCOL + Constant.PROTOCOL_CALL_TEMPERATURE;
    private static final String CALL_REFUSAL = Constant.PROTOCOL + Constant.PROTOCOL_CALL_REFUSAL;

    private static final String CHILD_ID = "childId";
    private static final String RECEIVER_ID = "receiverId";
    private static final String SENDER_ID = "senderId";
    /**
     * 0 : APP , 1 : Watch
     */
    private static final String C_TYPE = "cType";
    /**
     * 0 : 七牛 , 1 : 菊风
     */
    private static final String P_TYPE = "pType";
    private static final String JSON = "json";

    /**
     * 0: 成功,
     * 否则 失败
     */
    private static final String CODE = "code";
    private static final String MESSAGE = "Message";
    /**
     * roomToken
     */
    private static final String DATA = "Data";
    private String mChatId = "";

    public volatile boolean mLoginVideoChat;
    private QNRemoteSurfaceView mQnRemoteSurfaceView;
    private QNLocalSurfaceView mQnLocalSurfaceView;
    private Handler mHandler = new Handler();
    private long mBeginTime = 0;
    private RequestQueue mRequestQueue;
    private boolean mhasSetRemote = false;

    /*private VideoChatManager() {
    }

    private static VideoChatManager videoChatManager = null;

    public static VideoChatManager getInstance() {
        if (videoChatManager == null) {
            synchronized (VideoChatManager.class) {
                if (videoChatManager == null) {
                    videoChatManager = new VideoChatManager();
                }
            }
        }
        return videoChatManager;
    }*/

    /**
     * 设置监听回调
     *
     * @param listener listener
     */
    public void setCallListener(CallItemListener listener) {
        mCallListener = listener;
    }

    /**
     * 设置七牛 的远程view
     */
    public void setRemoteView(QNRemoteSurfaceView surfaceView) {
        mQnRemoteSurfaceView = surfaceView;
    }

    /**
     * 设置七牛 的本地view
     */
    public void setLocalView(QNLocalSurfaceView surfaceView) {
        mQnLocalSurfaceView = surfaceView;
    }

    /**
     * 初始化chatManager类
     *
     * @param context context
     */
    public void init(Context context) {
        mContext = context;
        mRequestQueue = Volley.newRequestQueue(context);
    }

    /**
     * 初始化视频聊天的资源sdk
     */
    public void initVideoChat() {
        if (Constant.IS_AUDE) {
            //qi niu
            Log.d(TAG, "IS_AUDE initVideoChat");
            QNRTCEnv.setLogLevel(QNLogLevel.INFO);
            QNRTCEnv.init(mContext);
            mRTCManager = new QNRTCManager();
            mRTCSetting = new QNRTCSetting();
            return;
        }
        client = JCClient.create(mContext, Constant.JC_APP_KEY, this, null);
        mediaDevice = JCMediaDevice.create(client, this);
        call = JCCall.create(client, mediaDevice, this);
        // 设置手表模式
        mediaDevice.setWatchMode(true);
    }

    /**
     * 七牛视频通话前设置参数
     */
    private void initQnSetting() {
        Log.d(TAG, "initQnSetting");
        mRTCSetting.setAudioBitrate(100 * 1000).setVideoBitrate(Constant.VIDEO_BITRATE).setBitrateRange(0
                , Constant.VIDEO_MAX_BITRATE).setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT)
                .setHWCodecEnabled(Constant.IS_HW_CODEC).setVideoPreviewFormat(new QNVideoFormat(640
                , 480, 20))
                .setVideoEncodeFormat(new QNVideoFormat(Constant.VIDEO_CHAT_WIDTH, Constant.VIDEO_CHAT_HEIGHT, QNRTCSetting.DEFAULT_FPS));
        mRTCSetting.setVideoPreviewFormat(new QNVideoFormat(Constant.VIDEO_CHAT_WIDTH
                , Constant.VIDEO_CHAT_HEIGHT, QNRTCSetting.DEFAULT_FPS));
        mRTCManager.addRemoteWindow(mQnRemoteSurfaceView);
        mQnRemoteSurfaceView.setZOrderMediaOverlay(true);
        mRTCManager.setRoomEventListener(this);
        //don't show local view
        mRTCManager.initialize(mContext, mRTCSetting, mQnLocalSurfaceView);
        mRTCManager.setPreviewEnabled(false);
    }

    /**
     * 登录视频聊天的账号体系
     */
    public void loginVideoChat(String account) {
        if (Constant.IS_AUDE) {
            //add remote view after set remote view
            //TODO for now just goto answer
            return;
        }
        mLoginVideoChat = true;
        Log.d(TAG, "account : " + account);
        if (TextUtils.isEmpty(account)) {
            return;
        }
        client.login(account, "1");
    }

    /**
     * 退出账号体系
     */
    public void logoutVideoChat() {
        if (client.getState() == JCClient.STATE_LOGINED) {
            client.logout();
        }
    }

    /**
     * 主动拨号，发起视频聊天
     *
     * @param myId 手表的id
     * @param uid  对方的id
     */
    public void call(int myId, String uid) {
        Log.d(TAG, "myId " + myId + "  uid " + uid);
        if (!Constant.IS_AUDE) {
            boolean isCallOk = call.call(uid, true, "kido");
            Log.d(TAG, "isCallOk : " + isCallOk);
            return;
        }
        JSONObject jsonObject = getJsonObject(myId, uid, 0, 0);
        JsonObjectRequest objectRequest = new JsonObjectRequest(CALL_OUT, jsonObject
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.d(TAG, "onResponse : " + jsonObject.toString());
                jsonObject = jsonObject.optJSONObject("d");
                String data = jsonObject.optString(DATA);
                initQnSetting();
                //uid is room token
                Log.d(TAG, "roomToken : " + data);
                mRTCManager.joinRoom(data);
                mIsJoinedRoom = true;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "onErrorResponse:" + volleyError.getMessage());
            }
        });
        mRequestQueue.add(objectRequest);
    }

    /**
     * 接听按钮响应事件
     */
    public void onAnswerCall(String mChatId, String token) {
        Log.d(TAG, "onAnswerCall");
        this.mChatId = mChatId;
        if (!Constant.IS_AUDE && mCallItem.getDirection() == JCCall.DIRECTION_IN) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mCheck10) {
                        mediaDevice.setCameraProperty(352, 282, 10);
                    } else {
                        mediaDevice.setCameraProperty(352, 282, 15);
                    }
                    mIsAnswered = call.answer(mCallItem, true);
                    Log.d(TAG, "answer : " + mIsAnswered);
                }
            }, 500);
            return;
        }
        if (Constant.IS_AUDE) {
            initQnSetting();
            mRTCManager.joinRoom(token);
            mIsAnswered = true;
            mBeginTime = System.currentTimeMillis() / 1000;
        }
    }

    /**
     * 获取json
     *
     * @param myId     myId
     * @param uid      uid
     * @param cType    cType 0 : app , 1: watch
     * @param jsonType 0: call out , 1: refusal
     * @return json
     */
    private JSONObject getJsonObject(int myId, String uid, int cType, int jsonType) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CHILD_ID, myId);
            if (jsonType == 0) {
                jsonObject.put(RECEIVER_ID, uid);
            } else if (jsonType == 1) {
                jsonObject.put(SENDER_ID, uid);
            }
            jsonObject.put(C_TYPE, cType);
            jsonObject.put(P_TYPE, 0);
            jsonObject.put(JSON, "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 退出应用时，释放资源
     */
    public void destroy() {
        if (Constant.IS_AUDE && mRTCManager != null) {
            mRTCManager.destroy();
            return;
        }
        mediaDevice.stopCamera();
    }

    /**
     * 挂断按钮响应事件
     */
    public void onEndCall() {
        stopCallInfoTimer();
        if (!Constant.IS_AUDE && mCallItem != null) {
            call.term(mCallItem, 0, "");
            return;
        }
        if (Constant.IS_AUDE) {
            Log.d(TAG, "onEndCall");
            mRTCManager.leaveRoom();
        }
    }

    /**
     * 主动挂断 只针对 七牛
     */
    public void onEndCall(int myId, String uid) {
        Log.d(TAG, "onEndCall myId : " + myId + " , uid : " + uid);
        JSONObject jsonObject = getJsonObject(myId, uid, 0, 1);
        JsonObjectRequest objectRequest = new JsonObjectRequest(CALL_REFUSAL, jsonObject
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.d(TAG, "onResponse : " + jsonObject.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "onErrorResponse:" + volleyError.getMessage());
            }
        });
        mRequestQueue.add(objectRequest);
        mRTCManager.leaveRoom();
    }

    /**
     * 开始计时
     *
     * @param textView 显示时间的控件
     */
    public void startCallInfoTimer(final TextView textView) {
        Log.d(TAG, "startCallInfoTimer");
        if (mCallInfoTimer != null) {
            stopCallInfoTimer();
        }
        mCallInfoTimer = new Timer();
        mCallInfoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!Constant.IS_AUDE && mCallItem != null) {
                            mBeginTime = mCallItem.getTalkingBeginTime();
                        }
                        long seconds = System.currentTimeMillis() / 1000 - mBeginTime;
                        String time = String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
                        textView.setText(time);
                    }
                });

            }
        }, 0, 1000);
    }

    /**
     * 停止计时
     */
    private void stopCallInfoTimer() {
        Log.d(TAG, "stopCallInfoTimer");
        if (mCallInfoTimer != null) {
            mCallInfoTimer.cancel();
            mCallInfoTimer = null;
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        if (Constant.IS_AUDE) {
            mRTCManager.switchCamera(new QNCameraSwitchResultCallback() {
                @Override
                public void onCameraSwitchDone(boolean b) {
                    Log.d(TAG, "isFrontCamera : " + b);
                }

                @Override
                public void onCameraSwitchError(String s) {
                    Log.d(TAG, "errorMessage : " + s);
                }
            });
            return;
        }
        mediaDevice.switchCamera();
    }

    /**
     * -----------------------------------------------------------------------------------
     * 以下 菊风的各个回调方法
     * -----------------------------------------------------------------------------------
     */
    @Override
    public void onLogin(boolean b, int i) {
        Log.d(TAG, b ? "登录成功" : "登录失败");
    }

    @Override
    public void onLogout(int i) {
        Log.d(TAG, "登出");
    }

    @Override
    public void onClientStateChange(int i, int i1) {

    }

    @Override
    public void onCameraUpdate() {

    }

    @Override
    public void onAudioOutputTypeChange(boolean b) {

    }

    @Override
    public void onCallItemAdd(JCCallItem jcCallItem) {
        Log.d(TAG, "onCallItemAdd , DisplayName : " + jcCallItem.getDisplayName());
        mCallItem = jcCallItem;
        if (mCallItem.getDirection() == JCCall.DIRECTION_IN) {
            Log.d(TAG, "DIRECTION_IN");
            Intent intent = new Intent(mContext, VideoChatActivity.class);
            intent.putExtra(Constant.EXTRA_CHAT_NAME, jcCallItem.getDisplayName());
            intent.putExtra(Constant.EXTRA_CHAT_TYPE, 0);
            mContext.startActivity(intent);
        }
    }

    @Override
    public void onCallItemRemove(JCCallItem jcCallItem, int i, String s) {
        if (mLocal != null) {
            mediaDevice.stopVideo(mLocal);
            mLocal = null;
        }
        if (mRemote != null) {
            mediaDevice.stopVideo(mRemote);
            mCallListener.onCallRemove(mRemote.getVideoView());
            mRemote = null;
        }
        mediaDevice.stopCamera();
    }

    @Override
    public void onCallItemUpdate(JCCallItem jcCallItem) {
        if (jcCallItem.getState() == JCCall.STATE_TALKING) {
            mediaDevice.startCamera();
            if (mRemote == null) {
                Log.d(TAG, "show remote");
                mRemote = mediaDevice.startVideo(jcCallItem.getRenderId(), JCMediaDevice.RENDER_FULL_SCREEN);
                if (mCallListener != null) {
                    mCallListener.onCallUpdate(mRemote.getVideoView());
                }
            }
        }
    }

    @Override
    public void onMessageReceive(String s, String s1, JCCallItem jcCallItem) {

    }

    /**
     * -----------------------------------------------------------------------------------
     * 以下 七牛的room的各个回调方法
     * -----------------------------------------------------------------------------------
     */

    @Override
    public void onJoinedRoom() {
        Log.d(TAG, "onJoinedRoom");
        mRTCManager.publish();
    }

    @Override
    public void onLocalPublished() {
        Log.d(TAG, "onLocalPublished");
        if (TextUtils.isEmpty(mChatId)) {
            return;
        }
        mRTCManager.subscribe(mChatId);
    }

    @Override
    public void onSubscribed(String s) {
        Log.i(TAG, "onSubscribed: userId: " + s);
    }

    @Override
    public void onRemotePublished(String s, boolean b, boolean b1) {
        Log.i(TAG, "onRemotePublished: userId: " + s + " , isAudioEnabled : " + b + " , isVideoEnabled : " + b1);
        mRTCManager.subscribe(s);
        mRTCManager.addRemoteAudioCallback(s, new QNRemoteAudioCallback() {
            @Override
            public void onRemoteAudioAvailable(String userId, ByteBuffer audioData, int size, int bitsPerSample
                    , int sampleRate, int numberOfChannels) {
            }
        });
    }

    /**
     * 首次收到远端媒体流时会触发的回调接口
     *
     * @param s  远端用户的 userId
     * @param b  远端用户是否发布了音频
     * @param b1 远端用户是否发布了视频
     * @param b2 远端用户是否关闭了音频
     * @param b3 远端用户是否关闭了视频
     * @return 远程view
     */
    @Override
    public QNRemoteSurfaceView onRemoteStreamAdded(String s, boolean b, boolean b1, boolean b2, boolean b3) {
        Log.i(TAG, "onRemoteStreamAdded: user = " + s + ", hasAudio = " + b + ", hasVideo = " + b1
                + ", isAudioMuted = " + b2 + ", isVideoMuted = " + b3);
        mCallListener.onCallUpdate(null);
        return mQnRemoteSurfaceView;
    }

    @Override
    public void onRemoteStreamRemoved(String s) {
        Log.i(TAG, "onRemoteStreamRemoved : user = " + s);
        mCallListener.onCallRemove(mQnRemoteSurfaceView);
    }

    @Override
    public void onRemoteUserJoined(String s) {
        Log.d(TAG, "onRemoteUserJoined user: " + s);
//        mCallListener.onCallUpdate(null);
        mBeginTime = System.currentTimeMillis() / 1000;
    }

    @Override
    public void onRemoteUserLeaved(String s) {
        Log.d(TAG, "onRemoteUserLeaved user: " + s);
        mCallListener.onCallRemove(mQnRemoteSurfaceView);
    }

    @Override
    public void onRemoteUnpublished(String s) {
        Log.d(TAG, "onRemoteUnpublished uid : " + s);
    }

    @Override
    public void onRemoteMute(String s, boolean b, boolean b1) {

    }

    @Override
    public void onStateChanged(QNRoomState qnRoomState) {
        Log.d(TAG, "onStateChanged QNRoomState: " + qnRoomState);
    }

    @Override
    public void onError(int i, String s) {
        Log.i(TAG, "onError: " + i + " " + s);
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport qnStatisticsReport) {
        Log.d(TAG, "onStatisticsUpdated : " + qnStatisticsReport.toString());
    }

    @Override
    public void onUserKickedOut(String s) {

    }
}
