package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * Moment đại diện cho một khoảnh khắc được người dùng gửi đi.
 */
public class Moment {

    private String momentId;
    private String senderId;
    private String imageUrl;
    private String caption;
    private Timestamp createdAt;
    private String status;

    public Moment() {
    }

    public Moment(String momentId, String senderId, String imageUrl, String caption,
                  Timestamp createdAt, String status) {
        this.momentId = momentId;
        this.senderId = senderId;
        this.imageUrl = imageUrl;
        this.caption = caption;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getMomentId() {
        return momentId;
    }

    public void setMomentId(String momentId) {
        this.momentId = momentId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
