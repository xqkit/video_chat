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
import com.kidosc.videochat.utils.SPUtil;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatEventType;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;
import com.netease.nimlib.sdk.avchat.model.AVChatCalleeAckEvent;
import com.netease.nimlib.sdk.avchat.model.AVChatCameraCapturer;
import com.netease.nimlib.sdk.avchat.model.AVChatCommonEvent;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.avchat.model.AVChatNotifyOption;
import com.netease.nimlib.sdk.avchat.model.AVChatParameters;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoCapturerFactory;
import com.netease.nrtc.video.render.IVideoRender;
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
import java.util.Map;
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
    private AVChatCameraCapturer mVideoCapturer;

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
    private IVideoRender mRemoteTv;
    private IVideoRender mLocalTv;
    private AVChatData avChatData;
    private AVChatNotifyOption mNotifyOption;
    private long dataChatId;

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

    void setIMRemoteView(IVideoRender textureViewRenderer) {
        this.mRemoteTv = textureViewRenderer;
    }

    void setIMLocalView(IVideoRender textureViewRenderer) {
        this.mLocalTv = textureViewRenderer;
    }

    /**
     * 初始化chatManager类
     *
     * @param context context
     */
    void init(Context context, VideoChatInfo videoChatInfo, String clientId, String clientToken) {
        mContext = context;
        mRequestQueue = Volley.newRequestQueue(context);
        mProtocol = Settings.Global.getString(context.getContentResolver(), "chataddress");
        this.videoChatInfo = videoChatInfo;
        String myId = videoChatInfo.myId;
        if (!TextUtils.isEmpty(myId)) {
            mMyId = Integer.parseInt(myId);
        }
        Log.d(TAG, "init context . mProtocol : " + mProtocol);
        if (Constant.IS_C4) {
            Log.d(TAG, "clientId " + clientId + " clientToken " + clientToken);
            NIMClient.getService(AuthService.class).login(new LoginInfo(clientId, clientToken)).setCallback
                    (new RequestCallback() {
                        @Override
                        public void onSuccess(Object o) {
                            Log.d(TAG, "login onSuccess");
                        }

                        @Override
                        public void onFailed(int i) {
                            Log.d(TAG, "login onFailed " + i);
                        }

                        @Override
                        public void onException(Throwable throwable) {
                            Log.d(TAG, "login onException " + throwable);
                        }
                    });
        }
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
        //net ease im
        if (Constant.IS_C4) {
            mNotifyOption = new AVChatNotifyOption();
            //附加字段
            mNotifyOption.extendMessage = "extra_data";
            //默认forceKeepCalling为true，开发者如果不需要离线持续呼叫功能可以将forceKeepCalling设为false
            mNotifyOption.forceKeepCalling = false;
            //打开Rtc模块
            if (AVChatManager.getInstance().enableRtc()) {
                Log.d(TAG, "初始化Rtc成功");
            } else {
                Log.d(TAG, "初始化Rtc失败");
            }
            //this.callingState = (callTypeEnum == AVChatType.VIDEO ? CallStateEnum.VIDEO : CallStateEnum.AUDIO);
            AVChatParameters avChatParameters = new AVChatParameters();
            avChatParameters.set(AVChatParameters.KEY_VIDEO_ROTATE_IN_RENDING, false);
            avChatParameters.set(AVChatParameters.KEY_DEVICE_DEFAULT_ROTATION, 0);
            avChatParameters.set(AVChatParameters.KEY_DEVICE_ROTATION_FIXED_OFFSET, 0);
            //设置自己需要的可选参数
            AVChatManager.getInstance().setParameters(avChatParameters);
            // state observer
            registerObserves(true);
            if (videoChatInfo.chatType == Constant.INCALL) {
                AVChatManager.getInstance().observeIncomingCall(incomingCallObserver, true);
            }
            //打开视频模块
            AVChatManager.getInstance().enableVideo();
            //创建视频采集模块并且设置到系统中
            if (mVideoCapturer == null) {
                mVideoCapturer = AVChatVideoCapturerFactory.createCameraCapturer();
                AVChatManager.getInstance().setupVideoCapturer(mVideoCapturer);
            }
            //设置本地预览画布
            AVChatManager.getInstance().setupLocalVideoRender(mLocalTv, false, AVChatVideoScalingType.SCALE_ASPECT_FIT);
            //开始视频预览
            AVChatManager.getInstance().startVideoPreview();

            if (videoChatInfo.chatType == Constant.CALLOUT) {
                outGoingCalling(videoChatInfo.senderId, AVChatType.VIDEO);
            }
            return;
        }
        //ju phoon
        client = JCClient.create(mContext, Constant.JC_APP_KEY, this, null);
        mediaDevice = JCMediaDevice.create(client, this);
        call = JCCall.create(client, mediaDevice, this);
        mediaDevice.setWatchMode(true);
        ZmfVideo.screenOrientation(0);
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

    private void registerObserves(boolean register) {
        AVChatManager.getInstance().observeAVChatState(stateObserver, register);
        AVChatManager.getInstance().observeHangUpNotification(callHangUpObserver, register);
        AVChatManager.getInstance().observeCalleeAckNotification(callAckObserver, register);
    }

    private SimpleAVChatStateObserver stateObserver = new SimpleAVChatStateObserver() {
        @Override
        public void onJoinedChannel(int code, String audioFile, String videoFile, int i) {
            Log.d(TAG, "onJoinedChannel code " + code);
            if (code == 200) {
//                onJoinRoomSuccess();
            } else {
//                onJoinRoomFailed(code, null);
            }
        }

        @Override
        public void onUserJoined(String account) {
//            onAVChatUserJoined(account);
            Log.d(TAG, "onUserJoined " + account);
            AVChatManager.getInstance().setupRemoteVideoRender(account, mRemoteTv, true, AVChatVideoScalingType.SCALE_ASPECT_FIT);
            mBeginTime = System.currentTimeMillis() / 1000;
            mCallListener.onCallUpdate(null, videoChatInfo.chatType == Constant.CALLOUT ? Constant.NO_NEED_WAITING : Constant.ON_STREAM);
        }

        @Override
        public void onUserLeave(String account, int event) {
//            onAVChatUserLeave(account);
            Log.d(TAG, "onUserLeave " + account + " event : " + event);
            mCallListener.onCallRemove("onUserLeave:" + account + " event " + event);
        }

        @Override
        public void onReportSpeaker(Map<String, Integer> speakers, int mixedEnergy) {
//            onAudioVolume(speakers);
            Log.d(TAG, "onReportSpeaker");
        }
    };

    private Observer<AVChatCommonEvent> callHangUpObserver = new Observer<AVChatCommonEvent>() {
        @Override
        public void onEvent(AVChatCommonEvent avChatCommonEvent) {
            /*if (avChatData != null && avChatData.getChatId() == avChatHangUpInfo.getChatId()) {
                avChatController.onHangUp(AVChatExitCode.HANGUP);
                cancelCallingNotifier();
                // 如果是incoming call主叫方挂断，那么通知栏有通知
                if (mIsInComingCall && !isCallEstablished) {
                    activeMissCallNotifier();
                }
            }*/
            Log.d(TAG, "callHangUp : " + avChatCommonEvent.getChatId());
            mCallListener.onCallRemove("he hang up");
        }
    };

    private Observer<AVChatData> incomingCallObserver = new Observer<AVChatData>() {
        @Override
        public void onEvent(AVChatData data) {
            dataChatId = data.getChatId();
            SPUtil.putLong(mContext, "chat_id", dataChatId);
            Log.d(TAG, "Extra Message->" + data.getChatId());
        }
    };


    private Observer<AVChatCalleeAckEvent> callAckObserver = new Observer<AVChatCalleeAckEvent>() {
        @Override
        public void onEvent(AVChatCalleeAckEvent ackInfo) {
            if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_REJECT) {
                Log.d(TAG, "CALLEE_ACK_REJECT : " + ackInfo.getChatId());
                mCallListener.onCallRemove("CALLEE_ACK_REJECT");
            }
            /*if (info.getChatId() == ackInfo.getChatId()) {
                AVChatSoundPlayer.instance().stop();

                if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_BUSY) {
                    AVChatSoundPlayer.instance().play(AVChatSoundPlayer.RingerTypeEnum.PEER_BUSY);
                    avChatController.onHangUp(AVChatExitCode.PEER_BUSY);
                } else if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_REJECT) {
                    avChatController.onHangUp(AVChatExitCode.REJECT);
                } else if (ackInfo.getEvent() == AVChatEventType.CALLEE_ACK_AGREE) {
                    avChatController.isCallEstablish.set(true);
                }
            }*/
        }
    };

    /**
     * IM 拨打音视频
     */
    private void outGoingCalling(String account, final AVChatType callTypeEnum) {
        Log.d(TAG, "outGoingCalling  " + account);
        //呼叫
        AVChatManager.getInstance().call2(account, callTypeEnum, mNotifyOption, new AVChatCallback<AVChatData>() {
            @Override
            public void onSuccess(AVChatData data) {
                Log.d(TAG, "onSuccess data " + data.getChatId());
                avChatData = data;
                //发起会话成功
            }

            @Override
            public void onFailed(int code) {
                Log.d(TAG, "call2 code " + code);
//                closeRtc();
//                closeSessions(-1);
            }

            @Override
            public void onException(Throwable exception) {
                Log.d(TAG, "onException " + exception);
//                closeRtc();
//                closeSessions(-1);
            }
        });
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
        JSONObject jsonObject = getJsonObject(mMyId, videoChatInfo.senderId, 1, Constant.IS_AUDE ? QI_NIU : JU_FENG);
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
        JSONObject jsonObject = getJsonObject(mMyId, videoChatInfo.senderId, 1, Constant.IS_AUDE ? QI_NIU : JU_FENG);
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
        JSONObject jsonObject = getJsonObject(mMyId, videoChatInfo.senderId, 0, Constant.IS_AUDE ? QI_NIU : JU_FENG);
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
        if (!Constant.IS_AUDE && (mCallItem != null && mCallItem.getDirection() == JCCall.DIRECTION_IN)) {
            //ju feng
            mIsAnswered = call.answer(mCallItem, true);
            mCallListener.onCallUpdate(null, Constant.WAITING_STREAM);
            Log.d(TAG, "onAnswerCall answer : " + mIsAnswered);
            return;
        }
        if (Constant.IS_C4) {
            //net ease
            if (SPUtil.getLong(mContext, "chat_id") == 0) {
                Log.d(TAG, "dataChatId = 0");
                return;
            }
            AVChatManager.getInstance().accept2(SPUtil.getLong(mContext, "chat_id"), new AVChatCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "accept success");
                }

                @Override
                public void onFailed(int i) {
                    Log.d(TAG, "onFailed " + i);
                }

                @Override
                public void onException(Throwable throwable) {
                    Log.d(TAG, "onException " + throwable);
                }
            });
            mCallListener.onCallUpdate(null, Constant.WAITING_STREAM);
            return;
        }
        if (Constant.IS_AUDE) {
            //qi niu
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
     * @param jsonType 0: call out , 1: refusal
     * @param pType    0-qiniu 1-jufeng
     * @return json
     */
    private JSONObject getJsonObject(int myId, String uid, int jsonType, int pType) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(CHILD_ID, myId);
            if (jsonType == 0) {
                jsonObject.put(RECEIVER_ID, uid);
            } else if (jsonType == 1) {
                jsonObject.put(SENDER_ID, uid);
            }
            jsonObject.put(C_TYPE, 0);
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
    void destroy() {
        Log.d(TAG, "destroy");
        if (Constant.IS_AUDE && mRTCManager != null) {
            mRTCManager.leaveRoom();
            mRTCManager.destroy();
            return;
        }
        if (Constant.IS_C4) {
            registerObserves(false);
            AVChatManager.getInstance().disableRtc();
            AVChatManager.getInstance().disableVideo();
            SPUtil.putLong(mContext, "chat_id", 0);
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
            if (type == Constant.REFUSE) {
                sendServerRefusal();
            } else if (type == Constant.HANGUP) {
                sendServerHangUp();
            }
            mRTCManager.leaveRoom();
            if (type == Constant.ISCHATING_REFUSE) {
                mCallListener.onCallRemove(" i click end call");
            }
            return;
        }
        if (Constant.IS_C4) {
            Log.d(TAG, "is ending call");
            // 如果是视频通话，关闭视频模块
            AVChatManager.getInstance().disableVideo();
            Log.d(TAG, "disableVideo");
            // 如果是视频通话，需要先关闭本地预览
            AVChatManager.getInstance().stopVideoPreview();
            Log.d(TAG, "stopVideoPreview");
            //销毁音视频引擎和释放资源
            AVChatManager.getInstance().disableRtc();
            //挂断
            if (type == Constant.REFUSE) {
                if (SPUtil.getLong(mContext, "chat_id") != 0) {
                    hangupNetease(SPUtil.getLong(mContext, "chat_id"));
                } else {
                    mCallListener.onCallRemove("REFUSE");
                }
            } else if (type == Constant.HANGUP) {
                if (avChatData != null) {
                    hangupNetease(avChatData.getChatId());
                } else {
                    mCallListener.onCallRemove("HANGUP");
                }
            }
            return;
        }
        if (mCallItem != null) {
            boolean isTerm = call.term(mCallItem, 0, "");
            Log.d(TAG, "JC term : " + isTerm);
        }
        mCallListener.onCallRemove("JUFENG TERM");
    }

    private void hangupNetease(long id) {
        AVChatManager.getInstance().hangUp2(id, new AVChatCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess");
                mCallListener.onCallRemove("hangupNetease,onSuccess");
            }

            @Override
            public void onFailed(int code) {
                Log.d(TAG, "onFailed " + code);
                mCallListener.onCallRemove("hangupNetease,onFailed");
            }

            @Override
            public void onException(Throwable exception) {
                Log.d(TAG, "onException " + exception);
                mCallListener.onCallRemove("hangupNetease,onException");
            }
        });
    }

    /**
     * 开始计时
     *
     * @param textView 显示时间的控件
     */
    void startCallInfoTimer(final TextView textView) {
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
    void switchCamera() {
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
