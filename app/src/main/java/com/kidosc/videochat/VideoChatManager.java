package com.kidosc.videochat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
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
import com.juphoon.cloud.JCPush;
import com.juphoon.cloud.JCPushTemplate;
import com.justalk.cloud.zmf.ZmfVideo;
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
import com.qiniu.droid.rtc.model.QNAudioDevice;
import com.zeusis.videochat.VideoChatInfo;

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
    private static final int LOGIN_ACCOUNT = 0;
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
    private long mBeginTime = 0;
    private RequestQueue mRequestQueue;

    private VideoChatInfo videoChatInfo = null;
    private int mMyId;

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

    private String mProtocol = "";

    private int loginTimes = 0;

//    private String mProtocol = "http://app.enjoykido.com:8801";
//    private String mProtocol = "http://devcapp.artimen.cn:7001";

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
     * roomToken
     */
    private static final String DATA = "Data";
    private static final int QI_NIU = 0;
    private static final int JU_FENG = 1;

    private QNRemoteSurfaceView mQnRemoteSurfaceView;
    private QNLocalSurfaceView mQnLocalSurfaceView;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case LOGIN_ACCOUNT:
                    loginAccount();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * 设置监听回调
     *
     * @param listener listener
     */
    void setCallListener(CallItemListener listener) {
        mCallListener = listener;
    }

    /**
     * 设置七牛 的远程view
     */
    void setRemoteView(QNRemoteSurfaceView surfaceView) {
        mQnRemoteSurfaceView = surfaceView;
    }

    /**
     * 设置七牛 的本地view
     */
    void setLocalView(QNLocalSurfaceView surfaceView) {
        mQnLocalSurfaceView = surfaceView;
    }

    /**
     * 初始化chatManager类
     *
     * @param context context
     */
    void init(Context context, VideoChatInfo videoChatInfo) {
        mContext = context;
        mRequestQueue = Volley.newRequestQueue(context);
        mProtocol = Settings.Global.getString(context.getContentResolver(), "chataddress");
        this.videoChatInfo = videoChatInfo;
        String myId = videoChatInfo.myId;
        if (!TextUtils.isEmpty(myId)) {
            mMyId = Integer.parseInt(myId);
        }
        Log.d(TAG, "init context . mProtocol : " + mProtocol);
    }

    /**
     * 初始化视频聊天的资源sdk
     */
    void initVideoChat() {
        if (Constant.IS_AUDE) {
            //qi niu
            Log.d(TAG, "IS_AUDE initVideoChat");
            QNRTCEnv.setLogLevel(QNLogLevel.INFO);
            QNRTCEnv.init(mContext);
            mRTCManager = new QNRTCManager();
            mRTCSetting = new QNRTCSetting();
            initQnSetting();
            if (videoChatInfo.chatType == Constant.CALLOUT) {
                call();
            }
            return;
        }
        //ju phoon
        client = JCClient.create(mContext, Constant.JC_APP_KEY, this, null);
        mediaDevice = JCMediaDevice.create(client, this);
        call = JCCall.create(client, mediaDevice, this);
        mediaDevice.setWatchMode(true);
        ZmfVideo.captureListenRotation(0, 0);
        ZmfVideo.renderListenRotation(0, 0);
        if (mCheck10) {
            mediaDevice.setCameraProperty(352, 282, 10);
        } else {
            mediaDevice.setCameraProperty(352, 282, 15);
        }
        mediaDevice.enableSpeaker(true);
        mediaDevice.startCamera();
        if (TextUtils.isEmpty(videoChatInfo.imei)) {
            mCallListener.onCallRemove("imei is null");
            return;
        }
        loginAccount();
    }

    /**
     * 七牛视频通话前设置参数
     */
    private void initQnSetting() {
        Log.d(TAG, "initQnSetting");
        mRTCSetting.setAudioBitrate(14 * 1000)
                .setVideoBitrate(550 * 1000)
                .setBitrateRange(0, 600 * 1000)
                .setCameraID(QNRTCSetting.CAMERA_FACING_ID.FRONT)
                .setHWCodecEnabled(true)
                .setVideoPreviewFormat(new QNVideoFormat(320, 240, 15))
                .setVideoEncodeFormat(new QNVideoFormat(320, 240, 15));
        mRTCManager.addRemoteWindow(mQnRemoteSurfaceView);
        mQnRemoteSurfaceView.setZOrderMediaOverlay(true);
        mRTCManager.setRoomEventListener(this);
        //don't show local view
        mRTCManager.initialize(mContext, mRTCSetting, mQnLocalSurfaceView);
        mRTCManager.setPreviewEnabled(false);
    }

    /**
     * 登录账号
     * 菊风登录
     */
    private void loginAccount() {
        if (client.getState() != JCClient.STATE_LOGINED) {
            boolean isLoginSuccess = client.login(videoChatInfo.imei, "1");
            loginTimes++;
            Log.d(TAG, "isLogin : " + isLoginSuccess + " times : " + loginTimes);
            if (!isLoginSuccess && loginTimes > 2) {
                Toast.makeText(mContext, mContext.getString(R.string.login_failed), Toast.LENGTH_SHORT).show();
                if (videoChatInfo.chatType == Constant.CALLOUT) {
                    mCallListener.onCallRemove("jufeng login failed , rather 3 times");
                } else if (videoChatInfo.chatType == Constant.INCALL) {
                    sendServerRefusal();
                }
            }
        } else if (client.getState() == JCClient.STATE_LOGINED) {
            call();
        }
    }

    /**
     * 主动拨号，发起视频聊天
     * 先走服务器，再走接口
     */
    void call() {
        //走服务器
        sendServerCall();
        //走三方接口
        if (!Constant.IS_AUDE) {
            boolean isCallOk = call.call(videoChatInfo.senderId, true, "kido");
            Log.d(TAG, "isCallOk : " + isCallOk);
            if (!isCallOk) {
                Toast.makeText(mContext, mContext.getString(R.string.call_failed), Toast.LENGTH_SHORT).show();
                sendServerRefusal();
            }
        }
    }

    /**
     * 给服务器发送拒接消息
     * 然后通知activity finish界面
     */
    private void sendServerRefusal() {
        JSONObject jsonObject = getJsonObject(mMyId, videoChatInfo.senderId, 0, 1, Constant.IS_AUDE ? QI_NIU : JU_FENG);
        String callRefusalUrl = mProtocol + Constant.PROTOCOL_CALL_REFUSAL;
        Log.d(TAG, "sendServerRefusal : " + callRefusalUrl);
        JsonObjectRequest objectRequest = new JsonObjectRequest(callRefusalUrl, jsonObject
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.d(TAG, "sendServerRefusal onResponse : " + jsonObject.toString());
                mCallListener.onCallRemove("refusal,1");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "sendServerRefusal onErrorResponse:" + volleyError.getMessage());
                mCallListener.onCallRemove("refusal,0");
            }
        });
        mRequestQueue.add(objectRequest);
    }

    /**
     * 给服务器发送取消消息
     * 然后通知activity finish界面
     */
    private void sendServerHangUp() {
        JSONObject jsonObject = getJsonObject(mMyId, videoChatInfo.senderId, 0, 1, Constant.IS_AUDE ? QI_NIU : JU_FENG);
        String callHangUpUrl = mProtocol + Constant.PROTOCOL_CALL_HANGUP;
        Log.d(TAG, "sendServerHangUp , callHangUpUrl : " + callHangUpUrl);
        JsonObjectRequest objectRequest = new JsonObjectRequest(callHangUpUrl, jsonObject
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.d(TAG, "hang up onResponse : " + jsonObject.toString());
                mCallListener.onCallRemove("hangup,1");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "hang up onErrorResponse:" + volleyError.getMessage());
                mCallListener.onCallRemove("hangup,0");
            }
        });
        mRequestQueue.add(objectRequest);
    }

    /**
     * 给服务器发送拨叫消息
     * 然后通知activity finish界面
     */
    private void sendServerCall() {
        JSONObject jsonObject = getJsonObject(mMyId, videoChatInfo.senderId, 0, 0, Constant.IS_AUDE ? QI_NIU : JU_FENG);
        String callOutUrl = mProtocol + Constant.PROTOCOL_CALL_OUT;
        Log.d(TAG, "sendServerCall  callOutUrl : " + callOutUrl);
        JsonObjectRequest objectRequest = new JsonObjectRequest(callOutUrl, jsonObject
                , new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject jsonObject) {
                Log.d(TAG, "call out onResponse : " + jsonObject.toString());
                if (Constant.IS_AUDE) {
                    //七牛的 接收返回值 token 并加入房间
                    jsonObject = jsonObject.optJSONObject("d");
                    String data = jsonObject.optString(DATA);
                    //uid is room token
                    Log.d(TAG, "roomToken : " + data);
                    mRTCManager.joinRoom(data);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "call out onErrorResponse:" + volleyError.getMessage());
            }
        });
        mRequestQueue.add(objectRequest);
    }

    /**
     * 接听按钮响应事件
     */
    void onAnswerCall() {
        mBeginTime = System.currentTimeMillis() / 1000;
        if (!Constant.IS_AUDE && mCallItem.getDirection() == JCCall.DIRECTION_IN) {
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
            mIsAnswered = call.answer(mCallItem, true);
            mCallListener.onCallUpdate(null, Constant.WAITING_STREAM);
            Log.d(TAG, "onAnswerCall answer : " + mIsAnswered);
//                }
//            }, 500);
            return;
        }
        if (Constant.IS_AUDE) {
            mRTCManager.joinRoom(videoChatInfo.roomToken);
            mIsAnswered = true;
            //mBeginTime = System.currentTimeMillis() / 1000;
            mCallListener.onCallUpdate(null, Constant.WAITING_STREAM);
        }
    }

    /**
     * 获取json
     *
     * @param myId     myId
     * @param uid      uid
     * @param cType    cType 0 : app , 1: watch
     * @param jsonType 0: call out , 1: refusal
     * @param pType    0-qiniu 1-jufeng
     * @return json
     */
    private JSONObject getJsonObject(int myId, String uid, int cType, int jsonType, int pType) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CHILD_ID, myId);
            if (jsonType == 0) {
                jsonObject.put(RECEIVER_ID, uid);
            } else if (jsonType == 1) {
                jsonObject.put(SENDER_ID, uid);
            }
            jsonObject.put(C_TYPE, cType);
            jsonObject.put(P_TYPE, pType);
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
            mRTCManager.leaveRoom();
            mRTCManager.destroy();
            return;
        }
        if (!Constant.IS_AUDE && mCallItem != null) {
            boolean isTerm = call.term(mCallItem, 0, "");
            Log.d(TAG, "destroy isTerm : " + isTerm);
        }
        //不主动退出账号，如果主动退出，会清掉push参数
        //logoutVideoChat();
        mediaDevice.stopCamera();
    }

    /**
     * 挂断电话
     *
     * @param type 3 : 拒接 ; 4: 取消 ; 5 : 正在通话中取消
     */
    void endCall(int type) {
        if (type == Constant.ISCHATING_REFUSE) {
            stopCallInfoTimer();
        }
        if (Constant.IS_AUDE) {
            Log.d(TAG, "QN term type " + type);
            if (type == Constant.REFUSE || type == Constant.ISCHATING_REFUSE) {
                sendServerRefusal();
            } else if (type == Constant.HANGUP) {
                sendServerHangUp();
            }
            mRTCManager.leaveRoom();
            return;
        }
        if (mCallItem != null) {
            boolean isTerm = call.term(mCallItem, 0, "");
            Log.d(TAG, "JC term : " + isTerm);
        }
        mCallListener.onCallRemove("endcall,jufeng");
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
                        /*if (!Constant.IS_AUDE && mCallItem != null) {
                            mBeginTime = mCallItem.getTalkingBeginTime();
                        }*/
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
        if (b) {
            mCallListener.onLoginJc();
            // 向菊风平台添加推送参数
            JCPush push = JCPush.create(client);
            JCPushTemplate pushInfo = new JCPushTemplate();
            pushInfo.initWithMiPush(mContext.getPackageName(), "kido");
            push.addPushInfo(pushInfo);
            pushInfo.initWithCall(JCPushTemplate.XIAOMI, client.getUserId(), "呼叫", "0");
            push.addPushInfo(pushInfo);
            pushInfo.initWithText(JCPushTemplate.XIAOMI, client.getUserId(), "Text", "消息", "0");
            push.addPushInfo(pushInfo);
        } else {
            mHandler.sendEmptyMessageDelayed(LOGIN_ACCOUNT, 300);
            mCallListener.onLoginFailed();
        }
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
        Log.d(TAG, "onCameraUpdate");
    }

    @Override
    public void onAudioOutputTypeChange(boolean b) {
        Log.d(TAG, "onAudioOutputTypeChange " + b);
    }

    @Override
    public void onCallItemRemove(JCCallItem jcCallItem, int i, String s) {
        Log.d(TAG, "onCallItemRemove");
        if (mLocal != null) {
            mediaDevice.stopVideo(mLocal);
            mLocal = null;
        }
        if (mRemote != null) {
            mediaDevice.stopVideo(mRemote);
            mRemote = null;
        }
        mediaDevice.stopCamera();
        if (call.getCallItems().size() == 0) {
            mCallListener.onCallRemove("jufeng item remove");
        }
    }

    @Override
    public void onCallItemAdd(JCCallItem jcCallItem) {
        if (jcCallItem == null) {
            return;
        }
        Log.d(TAG, "onCallItemAdd , DisplayName : " + jcCallItem.getDisplayName());
        mCallItem = jcCallItem;
        if (jcCallItem.getDirection() == JCCall.DIRECTION_IN) {
            Log.d(TAG, "onCallItemAdd : DIRECTION_IN ");
            mCallListener.onCallInAdd();
        } else if (jcCallItem.getDirection() == JCCall.DIRECTION_OUT) {
            Log.d(TAG, "onCallItemAdd : DIRECTION_OUT");
            mCallListener.onCallOutAdd();
        }
        if (jcCallItem.getActive()) {
            mediaDevice.startCameraVideo(JCMediaDevice.RENDER_FULL_SCREEN);
        }
    }

    @Override
    public void onCallItemUpdate(JCCallItem jcCallItem) {
        if (jcCallItem.getState() == JCCall.STATE_TALKING) {
            if (jcCallItem.getVideo()) {
                if (mRemote == null) {
                    Log.d(TAG, "onCallItemUpdate remote");
                    mRemote = mediaDevice.startVideo(jcCallItem.getRenderId(), JCMediaDevice.RENDER_FULL_SCREEN);
                    mBeginTime = System.currentTimeMillis() / 1000;
                    mCallListener.onCallUpdate(mRemote.getVideoView(), videoChatInfo.chatType == Constant.CALLOUT
                            ? Constant.NO_NEED_WAITING : Constant.ON_STREAM);
                }
            }
        }
    }

    public void pause() {
        if (!Constant.IS_AUDE) {
            //mediaDevice.enableSpeaker(false);
        }
    }

    public void resume() {
        if (!Constant.IS_AUDE && mediaDevice != null) {
            //mediaDevice.enableSpeaker(true);
        }
    }

    @Override
    public void onMessageReceive(String s, String s1, JCCallItem jcCallItem) {
        Log.d(TAG, "onMessageReceive : " + s + "  " + s1);
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
        mRTCManager.setMirror(true);
    }

    @Override
    public void onLocalPublished() {
        Log.d(TAG, "onLocalPublished ");
        mRTCManager.subscribe(videoChatInfo.senderId);
    }

    @Override
    public void onSubscribed(String s) {
        Log.i(TAG, "onSubscribed: userId: " + s);
        mBeginTime = System.currentTimeMillis() / 1000;
        mCallListener.onCallUpdate(null, videoChatInfo.chatType == Constant.CALLOUT ? Constant.NO_NEED_WAITING : Constant.ON_STREAM);
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
        return mQnRemoteSurfaceView;
    }

    @Override
    public void onRemoteStreamRemoved(String s) {
        Log.i(TAG, "onRemoteStreamRemoved : user = " + s);
        //mCallListener.onCallRemove();
    }

    @Override
    public void onRemoteUserJoined(String s) {
        Log.d(TAG, "onRemoteUserJoined user: " + s);
    }

    @Override
    public void onRemoteUserLeaved(String s) {
        Log.d(TAG, "onRemoteUserLeaved user: " + s);
        mCallListener.onCallRemove(s + " leaved");
    }

    @Override
    public void onRemoteUnpublished(String s) {
        Log.d(TAG, "onRemoteUnpublished uid : " + s);
        mCallListener.onCallRemove(s + " unPublished");
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
        Log.e(TAG, "onError: " + i + " " + s);
        Toast.makeText(mContext, s, Toast.LENGTH_SHORT).show();
        if (videoChatInfo.chatType == Constant.INCALL) {
            sendServerRefusal();
        } else if (videoChatInfo.chatType == Constant.CALLOUT) {
            mCallListener.onCallRemove("jufeng error : " + s);
        }
    }

    @Override
    public void onStatisticsUpdated(QNStatisticsReport qnStatisticsReport) {
        Log.d(TAG, "onStatisticsUpdated : " + qnStatisticsReport.toString());
    }

    @Override
    public void onUserKickedOut(String s) {

    }

    @Override
    public void onAudioRouteChanged(QNAudioDevice qnAudioDevice) {

    }

    @Override
    public void onCreateMergeJobSuccess(String s) {

    }
}
