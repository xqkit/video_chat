package com.kidosc.videochat.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.kidosc.videochat.R;

public class VolumeDialog {

    private final Context mContext;
    private final H mHandler = new H();

    private Window mWindow;
    private CustomDialog mDialog;
    private ViewGroup mDialogView;
    private VolumeView mVolumeView;
    private boolean mShowing;
    private final int mWindowType;
    private OnChangeListener onChangeListener;

    public VolumeDialog(Context context, int windowType) {
        mContext = context;
        mWindowType = windowType;
        initDialog();
    }

    private void initDialog() {
        mDialog = new CustomDialog(mContext);

        mShowing = false;
        mWindow = mDialog.getWindow();
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        mWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mWindow.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        mDialog.setCanceledOnTouchOutside(true);

        final WindowManager.LayoutParams lp = mWindow.getAttributes();
        lp.type = mWindowType;
        lp.format = PixelFormat.TRANSLUCENT;
//        lp.setTitle(VolumeDialog.class.getSimpleName());
//        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
//        lp.y = res.getDimensionPixelSize(R.dimen.volume_offset_top);
        lp.gravity = Gravity.CENTER;
        lp.windowAnimations = -1;
        mWindow.setAttributes(lp);
        mWindow.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        mDialog.setContentView(R.layout.volume_dialog);
        mDialogView = (ViewGroup) mDialog.findViewById(R.id.volume_dialog);
        mVolumeView = (VolumeView) mDialogView.findViewById(R.id.volume_view);
        onChangeListener = mVolumeView;
    }

    public void show() {
        if (null == mDialog && null != mContext) {
            initDialog();
        }
        if (!mDialog.isShowing()) {
            mHandler.sendEmptyMessage(H.SHOW);
        } else {
            rescheduleTimeoutH();
        }
        onChangeListener.onChange();
    }

    public void dismiss() {
        if (mDialog.isShowing()) {
            mHandler.sendEmptyMessage(H.DISMISS);
        }
    }

    private void showH() {
        mHandler.removeMessages(H.SHOW);
        mHandler.removeMessages(H.DISMISS);
        rescheduleTimeoutH();
        if (mShowing) {
            return;
        }
        mShowing = true;
        mDialog.show();
    }

    private void dismissH() {
        mHandler.removeMessages(H.DISMISS);
        mHandler.removeMessages(H.SHOW);
        if (!mShowing) {
            return;
        }
        mShowing = false;
        mDialog.dismiss();
    }

    private void rescheduleTimeoutH() {
        mHandler.removeMessages(H.DISMISS);
        //computeTimeoutH();
        final int timeout = 2000;
        mHandler.sendMessageDelayed(mHandler.obtainMessage(H.DISMISS), timeout);
    }

    private final class CustomDialog extends Dialog {
        public CustomDialog(Context context) {
            super(context);
        }

        public CustomDialog(Context context, int themeResId) {
            super(context, themeResId);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            rescheduleTimeoutH();
            return super.dispatchTouchEvent(ev);
        }

        @Override
        protected void onStop() {
            super.onStop();
            if (mShowing) {
                mShowing = false;
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isShowing()) {
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                    dismissH();
                    return true;
                }
            }
            return false;
        }
    }

    public interface OnChangeListener {
        void onChange();
    }

    private final class H extends Handler {
        private static final int SHOW = 1;
        private static final int DISMISS = 2;

        H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW:
                    showH();
                    break;
                case DISMISS:
                    dismissH();
                    break;
                default:
                    break;
            }
        }
    }
}
