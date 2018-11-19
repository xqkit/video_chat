package com.kidosc.videochat.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.kidosc.videochat.R;

/**
 * Desc:    调节音量大小
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/10/25 13:40
 */

public class VolumeProgress extends View {
    Paint mBgCirclePaint;
    Paint mArcPaint;
    float mRadius;
    float mArcRadius;
    float mStrokeWidth;
    int mBgCircleColor;
    int mArcColor;
    private int mProgress;
    private Bitmap mRingPicture;
    private Bitmap mMutePicture;

    public VolumeProgress(Context context) {
        super(context);
    }

    public VolumeProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mRadius = 80;
        mStrokeWidth = 8;
        mBgCircleColor = 0xFF6F7274;
        mArcColor = 0xFF2C9CFF;
        mArcRadius = mRadius + mStrokeWidth / 2;
        //外圆弧背景
        mBgCirclePaint = new Paint();
        mBgCirclePaint.setAntiAlias(true);
        mBgCirclePaint.setColor(mBgCircleColor);
        mBgCirclePaint.setStyle(Paint.Style.STROKE);
        mBgCirclePaint.setStrokeWidth(mStrokeWidth);
        //外圆弧
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setColor(mArcColor);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mStrokeWidth);
        //mRingPaint.setStrokeCap(Paint.Cap.ROUND);//设置线冒样式，有圆 有方
        mRingPicture = resizeImage(context, R.drawable.ic_vol);
        mMutePicture = resizeImage(context, R.drawable.ic_vol_mute);
    }

    public static Bitmap resizeImage(Context context, int id) {
        //使用BitmapFactory.Options的inSampleSize参数来缩放
        BitmapFactory.Options options = new BitmapFactory.Options();
        //不加载bitmap到内存中
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), id, options);
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        options.outHeight = outWidth / 2;
        options.outWidth = outHeight / 2;
        options.inDither = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 2;
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(context.getResources(), id, options);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //圆的中心
        int mCenterX = getWidth() / 2;
        int mCenterY = getHeight() / 2;
        //外圆弧背景
        RectF reactFBg = new RectF();
        reactFBg.left = mCenterX - mArcRadius;
        reactFBg.top = mCenterY - mArcRadius;
        reactFBg.right = mArcRadius * 2 + (mCenterX - mArcRadius);
        reactFBg.bottom = mArcRadius * 2 + (mCenterY - mArcRadius);
        canvas.drawArc(reactFBg, 0, 360, false, mBgCirclePaint);
        //外圆弧
        int mTotalProgress = 5;
        if (mProgress > 0) {
            RectF reactF = new RectF();
            reactF.left = mCenterX - mArcRadius;
            reactF.top = mCenterY - mArcRadius;
            reactF.right = mArcRadius * 2 + (mCenterX - mArcRadius);
            reactF.bottom = mArcRadius * 2 + (mCenterY - mArcRadius);
            canvas.drawArc(reactF, 270, ((float) mProgress / mTotalProgress) * 360
                    , false, mArcPaint);
        }
        if (mProgress > 0 && mProgress <= mTotalProgress) {
            canvas.drawBitmap(mRingPicture, mCenterX - mRingPicture.getWidth() / 2, mCenterY
                    - mRingPicture.getHeight() / 2, new Paint());
        } else if (mProgress == 0) {
            canvas.drawBitmap(mMutePicture, mCenterX - mRingPicture.getWidth() / 2, mCenterY
                    - mRingPicture.getHeight() / 2, new Paint());
        }
    }

    public void setProgress(int progress) {
        mProgress = progress;
        postInvalidate();
    }
}