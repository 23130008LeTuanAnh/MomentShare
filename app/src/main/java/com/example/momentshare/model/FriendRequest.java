package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * FriendRequest đại diện cho một lời mời kết bạn.
 * Trạng thái: pending, accepted, rejected.
 */
public class FriendRequest {
    private String requestId;
    private String senderId;
    private String receiverId;
    private String status; // pending, accepted, rejected
    private Timestamp createdAt;

    public FriendRequest() {
    }

    public FriendRequest(String requestId, String senderId, String receiverId, String status, Timestamp createdAt) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
