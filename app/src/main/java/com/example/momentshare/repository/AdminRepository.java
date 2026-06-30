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
 * AdminRepository gom toàn bộ logic quản trị MomentShare.
 *
 * Logic chính:
 * - Quản lý người dùng: xem danh sách, khóa/mở khóa, đổi quyền.
 * - Quản lý báo cáo: tạo report, bỏ qua report, ẩn moment bị báo cáo.
 * - Thống kê: đếm user, moment, report theo trạng thái.
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
                        if (user != null) {
                            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                                user.setUserId(document.getId());
                            }
                            users.add(user);
                        }
                    }

                    sortUsersByName(users);
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không tải được danh sách người dùng: " + e.getMessage()));
    }

    public void updateUserStatus(@NonNull String userId,
                                 @NonNull String status,
                                 @NonNull ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể cập nhật trạng thái tài khoản: " + e.getMessage()));
    }

    public void updateUserRole(@NonNull String userId,
                               @NonNull String role,
                               @NonNull ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("role", role);

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể cập nhật quyền tài khoản: " + e.getMessage()));
    }

    public void loadReports(@NonNull ReportsCallback callback) {
        db.collection(Constants.COLLECTION_REPORTS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ReportModel> reports = new ArrayList<>();

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ReportModel report = document.toObject(ReportModel.class);
                        if (report != null) {
                            if (report.getReportId() == null || report.getReportId().trim().isEmpty()) {
                                report.setReportId(document.getId());
                            }
                            reports.add(report);
                        }
                    }

                    sortReports(reports);
                    callback.onSuccess(reports);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không tải được danh sách báo cáo: " + e.getMessage()));
    }

    public void createReport(@NonNull String reporterId,
                             @NonNull String momentId,
                             @NonNull String reason,
                             @NonNull ActionCallback callback) {
        if (reporterId.trim().isEmpty() || momentId.trim().isEmpty() || reason.trim().isEmpty()) {
            callback.onFailure("Thiếu thông tin báo cáo");
            return;
        }

        // Chặn user báo cáo trùng cùng một moment.
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
                            .addOnFailureListener(e ->
                                    callback.onFailure("Không thể tạo báo cáo: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể kiểm tra báo cáo trùng: " + e.getMessage()));
    }

    /**
     * Admin bỏ qua báo cáo vì nội dung không vi phạm.
     */
    public void ignoreReport(@NonNull String reportId,
                             @NonNull String handledBy,
                             @NonNull ActionCallback callback) {
        updateReport(reportId, Constants.REPORT_STATUS_IGNORED, handledBy, callback);
    }

    /**
     * Admin xử lý report bằng cách ẩn moment và đánh dấu report resolved.
     *
     * Nếu momentId không tồn tại hoặc là dữ liệu demo, vẫn cập nhật report thành resolved
     * để màn hình admin không bị kẹt report pending.
     */
    public void resolveReportAndHideMoment(@NonNull String reportId,
                                           @NonNull String momentId,
                                           @NonNull String handledBy,
                                           @NonNull ActionCallback callback) {
        if (reportId.trim().isEmpty()) {
            callback.onFailure("Không xác định được báo cáo cần xử lý");
            return;
        }

        if (momentId.trim().isEmpty()) {
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
                            .addOnFailureListener(e ->
                                    callback.onFailure("Không thể xử lý báo cáo: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể kiểm tra khoảnh khắc bị báo cáo: " + e.getMessage()));
    }

    public void loadStatistics(@NonNull StatisticsCallback callback) {
        final int[] totalUsers = {0};
        final int[] activeUsers = {0};
        final int[] lockedUsers = {0};

        final int[] totalMoments = {0};
        final int[] activeMoments = {0};
        final int[] hiddenMoments = {0};

        final int[] totalReports = {0};
        final int[] pendingReports = {0};
        final int[] resolvedReports = {0};
        final int[] ignoredReports = {0};

        db.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnSuccessListener(usersSnapshot -> {
                    totalUsers[0] = usersSnapshot.size();

                    for (QueryDocumentSnapshot document : usersSnapshot) {
                        String status = document.getString("status");
                        if (Constants.STATUS_LOCKED.equals(status)) {
                            lockedUsers[0]++;
                        } else {
                            activeUsers[0]++;
                        }
                    }

                    db.collection(Constants.COLLECTION_MOMENTS)
                            .get()
                            .addOnSuccessListener(momentsSnapshot -> {
                                totalMoments[0] = momentsSnapshot.size();

                                for (QueryDocumentSnapshot document : momentsSnapshot) {
                                    String status = document.getString("status");
                                    if (Constants.MOMENT_STATUS_HIDDEN.equals(status)) {
                                        hiddenMoments[0]++;
                                    } else if (Constants.MOMENT_STATUS_DELETED.equals(status)) {
                                        // Không tính deleted là active.
                                    } else {
                                        activeMoments[0]++;
                                    }
                                }

                                db.collection(Constants.COLLECTION_REPORTS)
                                        .get()
                                        .addOnSuccessListener(reportsSnapshot -> {
                                            totalReports[0] = reportsSnapshot.size();

                                            for (QueryDocumentSnapshot document : reportsSnapshot) {
                                                String status = document.getString("status");
                                                if (Constants.REPORT_STATUS_RESOLVED.equals(status)) {
                                                    resolvedReports[0]++;
                                                } else if (Constants.REPORT_STATUS_IGNORED.equals(status)) {
                                                    ignoredReports[0]++;
                                                } else {
                                                    pendingReports[0]++;
                                                }
                                            }

                                            callback.onSuccess(new StatisticsModel(
                                                    totalUsers[0],
                                                    activeUsers[0],
                                                    lockedUsers[0],
                                                    totalMoments[0],
                                                    activeMoments[0],
                                                    hiddenMoments[0],
                                                    totalReports[0],
                                                    pendingReports[0],
                                                    resolvedReports[0],
                                                    ignoredReports[0]
                                            ));
                                        })
                                        .addOnFailureListener(e ->
                                                callback.onFailure("Không tải được thống kê báo cáo: " + e.getMessage()));
                            })
                            .addOnFailureListener(e ->
                                    callback.onFailure("Không tải được thống kê khoảnh khắc: " + e.getMessage()));
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không tải được thống kê người dùng: " + e.getMessage()));
    }

    private void updateReport(@NonNull String reportId,
                              @NonNull String status,
                              @NonNull String handledBy,
                              @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_REPORTS)
                .document(reportId)
                .update(buildHandledReportData(status, handledBy))
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể cập nhật báo cáo: " + e.getMessage()));
    }

    private Map<String, Object> buildHandledReportData(@NonNull String status,
                                                       @NonNull String handledBy) {
        Map<String, Object> data = new HashMap<>();
        data.put("status", status);
        data.put("handledBy", handledBy);
        data.put("handledAt", Timestamp.now());
        return data;
    }

    private void sortUsersByName(@NonNull List<User> users) {
        Collections.sort(users, (left, right) -> {
            String leftName = buildUserSortName(left);
            String rightName = buildUserSortName(right);
            return leftName.compareToIgnoreCase(rightName);
        });
    }

    private String buildUserSortName(User user) {
        if (user == null) return "";
        String fullName = user.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) return fullName.trim();
        String username = user.getUsername();
        if (username != null && !username.trim().isEmpty()) return username.trim();
        String email = user.getEmail();
        if (email != null && !email.trim().isEmpty()) return email.trim();
        return "";
    }

    private void sortReports(@NonNull List<ReportModel> reports) {
        Collections.sort(reports, (left, right) -> {
            int leftPriority = getReportPriority(left);
            int rightPriority = getReportPriority(right);

            if (leftPriority != rightPriority) {
                return leftPriority - rightPriority;
            }

            Timestamp leftTime = left.getCreatedAt();
            Timestamp rightTime = right.getCreatedAt();

            if (leftTime == null && rightTime == null) return 0;
            if (leftTime == null) return 1;
            if (rightTime == null) return -1;
            return rightTime.compareTo(leftTime);
        });
    }

    private int getReportPriority(ReportModel report) {
        if (report == null || report.getStatus() == null) return 1;
        if (Constants.REPORT_STATUS_PENDING.equals(report.getStatus())) return 0;
        if (Constants.REPORT_STATUS_RESOLVED.equals(report.getStatus())) return 1;
        return 2;
    }
}
