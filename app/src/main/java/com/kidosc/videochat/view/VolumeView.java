package com.kidosc.videochat.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.view.View;

import com.kidosc.videochat.R;


public class VolumeView extends View implements VolumeDialog.OnChangeListener {

    private static final String TAG = "VolumeView";
    private Paint paint = new Paint();
    // 控件高度  px
    private int height = 10;
    // 控件宽度
    private int width = 111;
    // 最大音量
    private int MAX = 5;
    // 两个音量矩形最左侧之间的间隔
    private int rectMargen = 4;
    // 音量矩形高
    private int rectH = 10;
    // 音量矩形宽
    private int recW = 18;
    // 当前选中的音量
    private int current = 2;
    // 最左侧音量矩形距离控件最左侧距离
    private int leftMargen = 2;
    private AudioManager mAudioManager;

    public VolumeView(Context context) {
        super(context);
        init();
    }

    public VolumeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VolumeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        updateStreamVolume();
    }

    private void updateStreamVolume() {
        if (null == mAudioManager) {
            mAudioManager = (AudioManager) this.getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        current = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        leftMargen = 2;

        // 绘制被选中的黄色音量矩形
        paint.setColor(getResources().getColor(R.color.volume_orange));
        for (int i = 0; i < current; i++) {
            canvas.drawRect(leftMargen + i * rectMargen, (height - rectH) / 2, leftMargen + i * rectMargen + recW, (height - rectH) / 2 + rectH,
                    paint);
            leftMargen += recW;
        }

        // 绘制没有被选中的白色音量矩形
        paint.setColor(getResources().getColor(R.color.volume_whrite));
        for (int i = current; i < MAX; i++) {
            canvas.drawRect(leftMargen + i * rectMargen, (height - rectH) / 2, leftMargen + i * rectMargen + recW, (height - rectH) / 2 + rectH,
                    paint);
            leftMargen += recW;
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(width, height);
    }

    @Override
    public void onChange() {
        updateStreamVolume();
        postInvalidate();
    }

}
