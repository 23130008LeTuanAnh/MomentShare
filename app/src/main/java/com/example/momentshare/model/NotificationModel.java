package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * NotificationModel đại diện cho thông báo trong ứng dụng.
 *
 * Đã chỉnh:
 * - Thêm targetId để thông báo biết cần mở màn hình nào/dữ liệu nào.
 *   Ví dụ:
 *   + reaction: targetId = momentId
 *   + moment: targetId = momentId
 *   + friend_request: targetId = senderId
 */
public class NotificationModel {

    private String notificationId;
    private String userId;
    private String type;
    private String title;
    private String message;
    private boolean isRead;
    private Timestamp createdAt;
    private String targetId;

    public NotificationModel() {
    }

    public NotificationModel(String notificationId, String userId, String type,
                             String title, String message, boolean isRead,
                             Timestamp createdAt) {
        this.notificationId = notificationId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
        this.targetId = "";
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
}
