package com.kidosc.videochat.event;

/**
 * Desc:
 * Email:   frank.xiong@kidosc.com
 * Date:    2018/12/27 18:18
 */

public class VideoChatEvent {
    public int type;
    public long chatId;

    public VideoChatEvent(int type, long chatId) {
        this.type = type;
        this.chatId = chatId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
