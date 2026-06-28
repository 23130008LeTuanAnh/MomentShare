package com.example.momentshare.helper;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

/**
 * FcmTokenManager quản lý token Firebase Cloud Messaging của thiết bị.
 * File này thuộc phần Người 5 - Notification.
 */
public class FcmTokenManager {

    private static final int REQUEST_POST_NOTIFICATIONS = 5005;

    private final FirebaseFirestore db;
    private final FirebaseMessaging messaging;

    public FcmTokenManager() {
        db = FirebaseConfig.getFirestore();
        messaging = FirebaseMessaging.getInstance();
    }

    /**
     * Người 5 thực hiện: đăng ký thiết bị hiện tại với FCM sau khi người dùng đăng nhập.
     *
     * Việc này gồm 3 bước:
     * 1. Xin quyền POST_NOTIFICATIONS trên Android 13+.
     * 2. Lấy FCM token thật của thiết bị.
     * 3. Lưu token vào users/{uid} để Admin/server/Cloud Functions có thể gửi push notification.
     */
    public void registerCurrentUserDevice(@NonNull Activity activity) {
        requestNotificationPermissionIfNeeded(activity);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String userId = currentUser.getUid();
        messaging.getToken()
                .addOnSuccessListener(token -> saveTokenForUser(userId, token))
                .addOnFailureListener(e -> {
                    // Người 5 thực hiện: không làm crash app nếu thiết bị chưa lấy được FCM token.
                    // Người dùng vẫn dùng được in-app notification trong collection notifications.
                });

        subscribeCurrentUserTopic(userId);
    }

    /**
     * Người 5 thực hiện: lưu token mới khi Firebase cấp hoặc refresh FCM token.
     */
    public void saveTokenForCurrentUser(@NonNull String token) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        saveTokenForUser(currentUser.getUid(), token);
        subscribeCurrentUserTopic(currentUser.getUid());
    }

    private void saveTokenForUser(@NonNull String userId, @NonNull String token) {
        if (token.trim().isEmpty()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("fcmToken", token);
        updates.put("fcmTopic", buildUserTopic(userId));
        updates.put("fcmTokenUpdatedAt", Timestamp.now());

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates);
    }

    private void subscribeCurrentUserTopic(@NonNull String userId) {
        messaging.subscribeToTopic(buildUserTopic(userId));
    }

    private String buildUserTopic(@NonNull String userId) {
        return Constants.FCM_USER_TOPIC_PREFIX + userId;
    }

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
