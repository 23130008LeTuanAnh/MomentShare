package com.example.momentshare.helper;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.momentshare.activity.LoginActivity;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.Constants;

/**
 * AdminAccessHelper kiểm tra quyền truy cập khu vực quản trị.
 *
 * Lý do tách file này:
 * - Không lặp code kiểm tra ADMIN ở AdminDashboardActivity, UserManagementActivity,
 *   ReportManagementActivity và StatisticsActivity.
 * - Không chỉ ẩn nút Admin trên UI, mà mỗi màn hình admin đều tự kiểm tra quyền.
 * - Chỉ tài khoản role ADMIN và status active mới được vào khu vực quản trị.
 */
public class AdminAccessHelper {

    public interface AdminAccessCallback {
        void onGranted(@NonNull String adminId, @NonNull User adminUser);
    }

    private final AuthManager authManager;
    private final UserRepository userRepository;

    public AdminAccessHelper() {
        authManager = new AuthManager();
        userRepository = new UserRepository();
    }

    /**
     * Kiểm tra quyền ADMIN cho Activity hiện tại.
     *
     * Nếu chưa đăng nhập: chuyển về LoginActivity.
     * Nếu không phải ADMIN hoặc tài khoản bị khóa: thông báo và đóng màn hình.
     * Nếu đúng ADMIN active: trả callback để màn hình tải dữ liệu.
     */
    public void requireActiveAdmin(@NonNull Activity activity,
                                   @NonNull AdminAccessCallback callback) {
        if (!authManager.isLoggedIn()) {
            goToLogin(activity);
            return;
        }

        String currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            goToLogin(activity);
            return;
        }

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!isActiveAdmin(user)) {
                    Toast.makeText(
                            activity,
                            "Bạn không có quyền truy cập khu vực quản trị",
                            Toast.LENGTH_SHORT
                    ).show();
                    activity.finish();
                    return;
                }

                callback.onGranted(currentUserId, user);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show();
                activity.finish();
            }
        });
    }

    /**
     * Điều kiện ADMIN hợp lệ của MomentShare.
     */
    public static boolean isActiveAdmin(User user) {
        return user != null
                && Constants.ROLE_ADMIN.equals(user.getRole())
                && Constants.STATUS_ACTIVE.equals(user.getStatus());
    }

    private void goToLogin(@NonNull Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
    }
}
