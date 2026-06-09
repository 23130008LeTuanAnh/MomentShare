package com.example.momentshare.util;

/**
 * Constants lưu các hằng số dùng chung trong ứng dụng MomentShare.
 *
 * Mục đích:
 * - Tránh viết sai tên collection Firestore.
 * - Quản lý thống nhất role và trạng thái tài khoản.
 * - Dễ bảo trì khi các module khác cần dùng chung.
 */
public class Constants {

    /**
     * Collection lưu thông tin người dùng trong Cloud Firestore.
     */
    public static final String COLLECTION_USERS = "users";

    /**
     * Vai trò người dùng thường.
     */
    public static final String ROLE_USER = "USER";

    /**
     * Vai trò quản trị viên.
     */
    public static final String ROLE_ADMIN = "ADMIN";

    /**
     * Trạng thái tài khoản đang hoạt động.
     */
    public static final String STATUS_ACTIVE = "active";

    /**
     * Trạng thái tài khoản bị khóa.
     */
    public static final String STATUS_LOCKED = "locked";

    /**
     * Avatar mặc định tạm thời.
     * Sau này có thể thay bằng URL ảnh mặc định trên Firebase Storage.
     */
    public static final String DEFAULT_AVATAR_URL = "";

    /**
     * Constructor private để không cho tạo object Constants.
     */
    private Constants() {
    }
}