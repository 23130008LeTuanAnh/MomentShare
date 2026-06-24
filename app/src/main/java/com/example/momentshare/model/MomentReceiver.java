package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * MomentReceiver lưu danh sách người nhận của một khoảnh khắc.
 */
public class MomentReceiver {

    private String id;
    private String momentId;
    private String receiverId;
    private boolean isViewed;
    private Timestamp viewedAt;

    public MomentReceiver() {
    }

    public MomentReceiver(String id, String momentId, String receiverId,
                          boolean isViewed, Timestamp viewedAt) {
        this.id = id;
        this.momentId = momentId;
        this.receiverId = receiverId;
        this.isViewed = isViewed;
        this.viewedAt = viewedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMomentId() {
        return momentId;
    }

    public void setMomentId(String momentId) {
        this.momentId = momentId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public boolean isViewed() {
        return isViewed;
    }

    public void setViewed(boolean viewed) {
        isViewed = viewed;
    }

    public Timestamp getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(Timestamp viewedAt) {
        this.viewedAt = viewedAt;
    }
}
