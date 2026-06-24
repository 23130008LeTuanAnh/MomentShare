package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * FriendUser đại diện cho một người bạn có thể được chọn để nhận khoảnh khắc.
 * Dữ liệu này được map từ collection users + quan hệ friends trên Firestore.
 */
public class FriendUser {

    private String userId;
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;
    private String status;
    private Timestamp createdAt;

    public FriendUser() {
    }

    public FriendUser(String userId, String fullName, String username, String email,
                      String avatarUrl, String status, Timestamp createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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
