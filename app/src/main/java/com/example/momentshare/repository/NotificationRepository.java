package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.helper.FirebaseConfig;
import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * NotificationRepository xử lý dữ liệu thông báo trong collection notifications.
 */
public class NotificationRepository {

    public interface NotificationListCallback {
        void onSuccess(@NonNull List<NotificationModel> notifications);
        void onFailure(@NonNull String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseFirestore db;

    public NotificationRepository() {
        db = FirebaseConfig.getFirestore();
    }

    public void createNotification(@NonNull String userId,
                                   @NonNull String type,
                                   @NonNull String title,
                                   @NonNull String message,
                                   @NonNull ActionCallback callback) {
        DocumentReference notificationRef = db.collection(Constants.COLLECTION_NOTIFICATIONS).document();

        NotificationModel notification = new NotificationModel();
        notification.setNotificationId(notificationRef.getId());
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedAt(Timestamp.now());

        notificationRef.set(notification)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể tạo thông báo: " + e.getMessage()));
    }

    public void loadNotifications(@NonNull String userId,
                                  @NonNull NotificationListCallback callback) {
        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<NotificationModel> notifications = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        NotificationModel notification = document.toObject(NotificationModel.class);
                        if (notification != null) {
                            notifications.add(notification);
                        }
                    }

                    sortByNewest(notifications);
                    callback.onSuccess(notifications);
                })
                .addOnFailureListener(e -> callback.onFailure("Không tải được thông báo: " + e.getMessage()));
    }

    public void markAsRead(@NonNull String notificationId,
                           @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notificationId)
                .update("read", true, "isRead", true)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể đánh dấu đã đọc: " + e.getMessage()));
    }

    public void markAllAsRead(@NonNull String userId,
                              @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("read", true);
                    updates.put("isRead", true);

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        batch.update(document.getReference(), updates);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure("Không thể cập nhật thông báo: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Không tải được thông báo: " + e.getMessage()));
    }

    private void sortByNewest(@NonNull List<NotificationModel> notifications) {
        Collections.sort(notifications, (left, right) -> {
            Timestamp leftTime = left.getCreatedAt();
            Timestamp rightTime = right.getCreatedAt();

            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });
    }
}
