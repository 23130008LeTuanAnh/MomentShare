package com.example.momentshare.util;

/**
 * Constants lưu các hằng số dùng chung trong ứng dụng MomentShare.
 * File này thuộc phần Người 5 - Database, Notification và Admin.
 * Các tên collection và trạng thái được gom lại để tránh viết sai khi nhiều module cùng dùng.
 */
public class Constants {

    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_FRIENDS = "friends";
    public static final String COLLECTION_FRIEND_REQUESTS = "friend_requests";
    public static final String COLLECTION_MOMENTS = "moments";
    public static final String COLLECTION_MOMENT_RECEIVERS = "moment_receivers";
    public static final String COLLECTION_REACTIONS = "reactions";
    public static final String COLLECTION_NOTIFICATIONS = "notifications";
    public static final String COLLECTION_REPORTS = "reports";

    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";

    public static final String STATUS_ACTIVE = "active";
    public static final String STATUS_LOCKED = "locked";

    public static final String MOMENT_STATUS_ACTIVE = "active";
    public static final String MOMENT_STATUS_HIDDEN = "hidden";
    public static final String MOMENT_STATUS_DELETED = "deleted";

    public static final String REPORT_STATUS_PENDING = "pending";
    public static final String REPORT_STATUS_RESOLVED = "resolved";
    public static final String REPORT_STATUS_IGNORED = "ignored";

    public static final String NOTIFICATION_TYPE_MOMENT = "moment";
    public static final String NOTIFICATION_TYPE_FRIEND_REQUEST = "friend_request";
    public static final String NOTIFICATION_TYPE_REACTION = "reaction";
    public static final String NOTIFICATION_TYPE_REPORT = "report";

    public static final String DEFAULT_AVATAR_URL = "";

    // Người 5 thực hiện: cấu hình kênh và topic cho Firebase Cloud Messaging push notification.
    public static final String FCM_CHANNEL_ID = "momentshare_push_channel";
    public static final String FCM_CHANNEL_NAME = "MomentShare Push Notifications";
    public static final String FCM_USER_TOPIC_PREFIX = "user_";

    private Constants() {
    }
}
