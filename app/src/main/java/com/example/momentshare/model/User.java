package com.example.momentshare.model;

import com.google.firebase.Timestamp;

/**
 * User đại diện cho thông tin người dùng trong ứng dụng MomentShare.
 *
 * Dữ liệu này tương ứng với collection users trên Firestore:
 * - userId
 * - fullName
 * - username
 * - email
 * - avatarUrl
 * - bio
 * - role
 * - status
 * - createdAt
 */
public class User {

    private String userId;
    private String fullName;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private String role;
    private String status;
    private Timestamp createdAt;

    /**
     * Constructor rỗng bắt buộc để Firestore có thể tự map dữ liệu sang object User.
     */
    public User() {
    }

    /**
     * Constructor dùng khi tạo tài khoản mới.
     *
     * @param userId mã người dùng từ Firebase Authentication
     * @param fullName họ tên người dùng
     * @param username username dùng để tìm kiếm/kết bạn
     * @param email email đăng nhập
     * @param avatarUrl link ảnh đại diện
     * @param bio mô tả cá nhân
     * @param role vai trò USER hoặc ADMIN
     * @param status trạng thái active hoặc locked
     * @param createdAt thời gian tạo tài khoản
     */
    public User(String userId, String fullName, String username, String email,
                String avatarUrl, String bio, String role, String status,
                Timestamp createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.role = role;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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