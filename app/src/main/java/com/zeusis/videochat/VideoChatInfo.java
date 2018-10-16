package com.zeusis.videochat;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Desc:    视频聊天的载体
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/10/12 15:43
 */

public class VideoChatInfo implements Parcelable {

    public String name;
    public String photo;
    public String roomToken;
    public String myId;
    public String senderId;
    public String imei;
    public int chatType;

    public VideoChatInfo(Parcel in) {
        name = in.readString();
        photo = in.readString();
        roomToken = in.readString();
        myId = in.readString();
        senderId = in.readString();
        imei = in.readString();
        chatType = in.readInt();
    }

    @Override
    public String toString() {
        return "VideoChatInfo{" +
                "name='" + name + '\'' +
                ", photo='" + photo + '\'' +
                ", roomToken='" + roomToken + '\'' +
                ", myId='" + myId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", imei='" + imei + '\'' +
                ", chatType=" + chatType +
                '}';
    }

    public VideoChatInfo(String name, String photo, String roomToken, String myId, String senderId, String imei, int chatType) {
        this.name = name;
        this.photo = photo;
        this.roomToken = roomToken;
        this.myId = myId;
        this.senderId = senderId;
        this.imei = imei;
        this.chatType = chatType;
    }

    public static final Creator<VideoChatInfo> CREATOR = new Creator<VideoChatInfo>() {
        @Override
        public VideoChatInfo createFromParcel(Parcel in) {
            return new VideoChatInfo(in);
        }

        @Override
        public VideoChatInfo[] newArray(int size) {
            return new VideoChatInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(photo);
        dest.writeString(roomToken);
        dest.writeString(myId);
        dest.writeString(senderId);
        dest.writeString(imei);
        dest.writeInt(chatType);
    }
}
