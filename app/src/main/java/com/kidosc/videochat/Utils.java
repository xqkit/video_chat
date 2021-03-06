package com.kidosc.videochat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Utils {

    public static int[] mDefaultPhoto = {R.drawable.guardian_1, R.drawable.guardian_2
            , R.drawable.guardian_3, R.drawable.guardian_4, R.drawable.guardian_5
            , R.drawable.guardian_6, R.drawable.guardian_7, R.drawable.guardian_8
            , R.drawable.guardian_9, R.drawable.guardian_10, R.drawable.guardian_11
            , R.drawable.guardian_12, R.drawable.guardian_13, R.drawable.guardian_14
    };

    /**
     * 设置背景
     *
     * @param photo
     * @param view
     */
    public static void setDefaultBackgroundPhoto(String photo, View view) {
        if (null == photo || photo.isEmpty()) {
            view.setBackgroundResource(R.drawable.k2_png_watchface_favorite_contact_avatar_default);
        } else {
            if (photo.length() <= 2) {
                //默认头像
                int defaultPhoto = Integer.parseInt(photo);
                int photoPosition = mDefaultPhoto[defaultPhoto - 1 != -1 ? defaultPhoto - 1 : 0];
                view.setBackgroundResource(photoPosition);
            } else {
                Bitmap loacalPhoto = getLoacalBitmap(photo);
                if (null != loacalPhoto) {
                    //读取服务器自定义头像
                    view.setBackground(new BitmapDrawable(loacalPhoto));
                } else {
                    view.setBackgroundResource(R.drawable.k2_png_watchface_favorite_contact_avatar_default);
                }
            }
        }
    }

    /**
     * 加载本地图片
     *
     * @param url
     * @return
     */
    public static Bitmap getLoacalBitmap(String url) {
        try {
            FileInputStream fis = new FileInputStream(url);
            //把流转化为Bitmap图片
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
