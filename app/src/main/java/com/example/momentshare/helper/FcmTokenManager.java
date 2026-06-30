package com.example.momentshare.helper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * FcmTokenManager quản lý token Firebase Cloud Messaging của thiết bị.
 * File này thuộc phần Người 5 - Notification.
 *
 * Chức năng chính:
 * 1. Xin quyền hiển thị thông báo trên Android 13 trở lên.
 * 2. Lấy FCM token thật của thiết bị.
 * 3. Lưu token vào Firestore tại users/{uid}.
 * 4. Subscribe user vào topic riêng để có thể gửi notification theo từng user.
 */
public class FcmTokenManager {

    private static final String TAG = "FcmTokenManager";
    private static final int REQUEST_POST_NOTIFICATIONS = 5005;

    private final FirebaseFirestore db;
    private final FirebaseMessaging messaging;

    public FcmTokenManager() {
        db = FirebaseConfig.getFirestore();
        messaging = FirebaseMessaging.getInstance();
    }

    /**
     * Gọi hàm này sau khi user đăng nhập thành công.
     *
     * Hàm này sẽ:
     * 1. Xin quyền POST_NOTIFICATIONS nếu thiết bị Android 13+.
     * 2. Lấy FCM token hiện tại.
     * 3. Lưu token vào document users/{uid}.
     * 4. Đăng ký topic riêng cho user.
     */
    public void registerCurrentUserDevice(@NonNull Activity activity) {
        requestNotificationPermissionIfNeeded(activity);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Không có user đăng nhập, bỏ qua đăng ký FCM token.");
            return;
        }

        String userId = currentUser.getUid();

        messaging.getToken()
                .addOnSuccessListener(token -> {
                    Log.d(TAG, "Lấy FCM token thành công: " + token);
                    saveTokenForUser(userId, token);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lấy FCM token thất bại.", e);
                    // Không làm crash app nếu thiết bị chưa lấy được token.
                    // User vẫn dùng được in-app notification trong collection notifications.
                });

        subscribeCurrentUserTopic(userId);
    }

    /**
     * Hàm này được gọi khi Firebase cấp token mới hoặc refresh token.
     * Thường được gọi từ MyFirebaseMessagingService.onNewToken().
     */
    public void saveTokenForCurrentUser(@NonNull String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.d(TAG, "Không có user đăng nhập, chưa lưu FCM token mới.");
            return;
        }

        String userId = currentUser.getUid();

        saveTokenForUser(userId, token);
        subscribeCurrentUserTopic(userId);
    }

    /**
     * Lưu FCM token vào Firestore.
     *
     * Đã chỉnh:
     * - Không dùng update() vì update() sẽ lỗi nếu users/{uid} chưa tồn tại.
     * - Dùng set(..., SetOptions.merge()) để:
     *   + Nếu document đã có: cập nhật thêm fcmToken, không mất dữ liệu cũ.
     *   + Nếu document chưa có: tự tạo document mới.
     */
    private void saveTokenForUser(@NonNull String userId, @NonNull String token) {
        if (token.trim().isEmpty()) {
            Log.d(TAG, "FCM token rỗng, không lưu.");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        updates.put("fcmTopic", buildUserTopic(userId));
        updates.put("fcmTokenUpdatedAt", Timestamp.now());

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Đã lưu FCM token vào users/" + userId);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Lưu FCM token thất bại cho user: " + userId, e);
                });
    }

    /**
     * Subscribe user vào topic riêng.
     * Ví dụ:
     * user_abc123
     */
    private void subscribeCurrentUserTopic(@NonNull String userId) {
        String topic = buildUserTopic(userId);

        messaging.subscribeToTopic(topic)
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Đã subscribe topic: " + topic);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Subscribe topic thất bại: " + topic, e);
                });
    }

    /**
     * Tạo topic riêng cho từng user.
     */
    private String buildUserTopic(@NonNull String userId) {
        return Constants.FCM_USER_TOPIC_PREFIX + userId;
    }

    /**
     * Xin quyền thông báo trên Android 13 trở lên.
     *
     * Android dưới 13 không cần xin quyền POST_NOTIFICATIONS lúc runtime.
     */
    private void requestNotificationPermissionIfNeeded(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_POST_NOTIFICATIONS
        );
    }
}
