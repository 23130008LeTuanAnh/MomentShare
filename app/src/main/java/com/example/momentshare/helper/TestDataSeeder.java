package com.example.momentshare.helper;

import androidx.annotation.NonNull;

import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.model.ReportModel;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

/**
 * TestDataSeeder tạo dữ liệu mẫu nhỏ để nhóm demo nhanh module Notification và Admin.
 * File này thuộc phần Người 5 - Test case và dữ liệu demo.
 * Chỉ dùng trong giai đoạn kiểm thử, không gọi tự động khi app chạy thật.
 */
public class TestDataSeeder {

    public interface SeedCallback {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseFirestore db;

    public TestDataSeeder() {
        db = FirebaseConfig.getFirestore();
    }

    public void seedAdminDemoData(@NonNull String currentUserId,
                                  @NonNull SeedCallback callback) {
        DocumentReference notificationRef = db.collection(Constants.COLLECTION_NOTIFICATIONS).document();
        DocumentReference reportRef = db.collection(Constants.COLLECTION_REPORTS).document();

        NotificationModel notification = new NotificationModel(
                notificationRef.getId(),
                currentUserId,
                Constants.NOTIFICATION_TYPE_REPORT,
                "Thông báo demo",
                "Đây là thông báo mẫu để kiểm thử màn hình thông báo.",
                false,
                Timestamp.now()
        );

        ReportModel report = new ReportModel(
                reportRef.getId(),
                currentUserId,
                "demo_moment_id",
                "Nội dung demo cần admin xem xét",
                Constants.REPORT_STATUS_PENDING,
                Timestamp.now(),
                "",
                null
        );

        // Người 5 thêm: dùng batch để tạo cùng lúc notification và report demo.
        WriteBatch batch = db.batch();
        batch.set(notificationRef, notification);
        batch.set(reportRef, report);

        batch.commit()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể tạo dữ liệu demo: " + e.getMessage()));
    }

    public void makeCurrentUserAdmin(@NonNull String currentUserId,
                                     @NonNull AdminRepository.ActionCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .update("role", Constants.ROLE_ADMIN)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể cấp quyền admin demo: " + e.getMessage()));
    }
}
