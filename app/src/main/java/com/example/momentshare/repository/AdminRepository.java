package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.helper.FirebaseConfig;
import com.example.momentshare.model.ReportModel;
import com.example.momentshare.model.StatisticsModel;
import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminRepository gom các thao tác quản trị: thống kê, quản lý người dùng và xử lý báo cáo.
 *
 * Đã chỉnh:
 * - Xử lý report vẫn thành công nếu momentId demo/không tồn tại.
 * - Nếu moment tồn tại thì ẩn moment bằng status hidden.
 */
public class AdminRepository {

    public interface ActionCallback {
        void onSuccess();
        void onFailure(@NonNull String errorMessage);
    }

    public interface UsersCallback {
        void onSuccess(@NonNull List<User> users);
        void onFailure(@NonNull String errorMessage);
    }

    public interface ReportsCallback {
        void onSuccess(@NonNull List<ReportModel> reports);
        void onFailure(@NonNull String errorMessage);
    }

    public interface StatisticsCallback {
        void onSuccess(@NonNull StatisticsModel statistics);
        void onFailure(@NonNull String errorMessage);
    }

    private final FirebaseFirestore db;

    public AdminRepository() {
        db = FirebaseConfig.getFirestore();
    }

    public void loadUsers(@NonNull UsersCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        User user = document.toObject(User.class);
                        if (user != null) users.add(user);
                    }
                    sortUsersByName(users);
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onFailure("Không tải được danh sách người dùng: " + e.getMessage()));
    }

    public void updateUserStatus(@NonNull String userId,
                                 @NonNull String status,
                                 @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("status", status)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể cập nhật trạng thái tài khoản: " + e.getMessage()));
    }

    public void updateUserRole(@NonNull String userId,
                               @NonNull String role,
                               @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("role", role)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể cập nhật quyền tài khoản: " + e.getMessage()));
    }

    public void loadReports(@NonNull ReportsCallback callback) {
        db.collection(Constants.COLLECTION_REPORTS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ReportModel> reports = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ReportModel report = document.toObject(ReportModel.class);
                        if (report != null) reports.add(report);
                    }
                    sortReportsByNewest(reports);
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(e -> callback.onFailure("Không tải được danh sách báo cáo: " + e.getMessage()));
    }

    public void createReport(@NonNull String reporterId,
                             @NonNull String momentId,
                             @NonNull String reason,
                             @NonNull ActionCallback callback) {
        if (reporterId.trim().isEmpty() || momentId.trim().isEmpty() || reason.trim().isEmpty()) {
            callback.onFailure("Thiếu thông tin báo cáo");
            return;
        }

        db.collection(Constants.COLLECTION_REPORTS)
                .whereEqualTo("reporterId", reporterId)
                .whereEqualTo("momentId", momentId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        callback.onFailure("Bạn đã báo cáo khoảnh khắc này rồi");
                        return;
                    }

                    String reportId = db.collection(Constants.COLLECTION_REPORTS).document().getId();
                    ReportModel report = new ReportModel(
                            reportId,
                            reporterId,
                            momentId,
                            reason.trim(),
                            Constants.REPORT_STATUS_PENDING,
                            Timestamp.now(),
                            "",
                            null
                    );

                    db.collection(Constants.COLLECTION_REPORTS)
                            .document(reportId)
                            .set(report)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure("Không thể tạo báo cáo: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Không thể kiểm tra báo cáo trùng: " + e.getMessage()));
    }

    public void ignoreReport(@NonNull String reportId,
                             @NonNull String handledBy,
                             @NonNull ActionCallback callback) {
        updateReport(reportId, Constants.REPORT_STATUS_IGNORED, handledBy, callback);
    }

    public void resolveReportAndHideMoment(@NonNull String reportId,
                                           @NonNull String momentId,
                                           @NonNull String handledBy,
                                           @NonNull ActionCallback callback) {
        if (momentId == null || momentId.trim().isEmpty()) {
            updateReport(reportId, Constants.REPORT_STATUS_RESOLVED, handledBy, callback);
            return;
        }

        db.collection(Constants.COLLECTION_MOMENTS)
                .document(momentId)
                .get()
                .addOnSuccessListener(momentDoc -> {
                    WriteBatch batch = db.batch();
                    batch.update(
                            db.collection(Constants.COLLECTION_REPORTS).document(reportId),
                            buildHandledReportData(Constants.REPORT_STATUS_RESOLVED, handledBy)
                    );

                    if (momentDoc.exists()) {
                        batch.update(
                                db.collection(Constants.COLLECTION_MOMENTS).document(momentId),
                                "status",
                                Constants.MOMENT_STATUS_HIDDEN
                        );
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onFailure("Không thể xử lý báo cáo: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Không thể kiểm tra khoảnh khắc bị báo cáo: " + e.getMessage()));
    }

    public void loadStatistics(@NonNull StatisticsCallback callback) {
        final int[] totalUsers = {0};
        final int[] lockedUsers = {0};
        final int[] totalMoments = {0};
        final int[] totalReports = {0};
        final int[] pendingReports = {0};

        db.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    totalUsers[0] = usersSnapshot.size();
                    for (QueryDocumentSnapshot document : usersSnapshot) {
                        if (Constants.STATUS_LOCKED.equals(document.getString("status"))) lockedUsers[0]++;
                    }

                    db.collection(Constants.COLLECTION_MOMENTS)
                            .get()
                            .addOnSuccessListener(momentsSnapshot -> {
                                totalMoments[0] = momentsSnapshot.size();
                                db.collection(Constants.COLLECTION_REPORTS)
                                        .get()
                                        .addOnSuccessListener(reportsSnapshot -> {
                                            totalReports[0] = reportsSnapshot.size();
                                            for (QueryDocumentSnapshot document : reportsSnapshot) {
                                                if (Constants.REPORT_STATUS_PENDING.equals(document.getString("status"))) pendingReports[0]++;
                                            }
                                            callback.onSuccess(new StatisticsModel(totalUsers[0], totalMoments[0], totalReports[0], pendingReports[0], lockedUsers[0]));
                                        })
                                        .addOnFailureListener(e -> callback.onFailure("Không tải được thống kê báo cáo: " + e.getMessage()));
                            })
                            .addOnFailureListener(e -> callback.onFailure("Không tải được thống kê khoảnh khắc: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Không tải được thống kê người dùng: " + e.getMessage()));
    }

    private void updateReport(@NonNull String reportId,
                              @NonNull String status,
                              @NonNull String handledBy,
                              @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_REPORTS)
                .document(reportId)
                .update(buildHandledReportData(status, handledBy))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể cập nhật báo cáo: " + e.getMessage()));
    }

    private Map<String, Object> buildHandledReportData(@NonNull String status,
                                                       @NonNull String handledBy) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("handledBy", handledBy);
        updates.put("handledAt", Timestamp.now());
        return updates;
    }

    private void sortUsersByName(@NonNull List<User> users) {
        Collections.sort(users, (left, right) -> safeText(left.getFullName()).compareToIgnoreCase(safeText(right.getFullName())));
    }

    private void sortReportsByNewest(@NonNull List<ReportModel> reports) {
        Collections.sort(reports, (left, right) -> {
            Timestamp leftTime = left.getCreatedAt();
            Timestamp rightTime = right.getCreatedAt();
            if (leftTime == null && rightTime == null) return 0;
            if (leftTime == null) return 1;
            if (rightTime == null) return -1;
            return rightTime.compareTo(leftTime);
        });
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
