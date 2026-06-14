package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.ValidationUtils;

/**
 * SplashActivity là màn hình khởi động đầu tiên của ứng dụng MomentShare.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Chức năng chính:
 * - Hiển thị màn hình chào khi mở ứng dụng.
 * - Kiểm tra trạng thái đăng nhập bằng Firebase Authentication.
 * - Nếu chưa đăng nhập, chuyển sang LoginActivity.
 * - Nếu đã đăng nhập, kiểm tra hồ sơ người dùng trên Firestore.
 * - Nếu tài khoản active, chuyển sang ProfileActivity.
 * - Nếu tài khoản locked hoặc hồ sơ lỗi, đăng xuất và chuyển về LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500;

    private AuthManager authManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // AuthManager dùng để kiểm tra trạng thái đăng nhập Firebase.
        authManager = new AuthManager();

        // UserRepository dùng để kiểm tra hồ sơ và trạng thái tài khoản trên Firestore.
        userRepository = new UserRepository();

        // Chờ một khoảng ngắn để hiển thị Splash rồi mới kiểm tra đăng nhập.
        new Handler(Looper.getMainLooper()).postDelayed(
                this::checkLoginState,
                SPLASH_DELAY_MS
        );
    }

    /**
     * Kiểm tra trạng thái đăng nhập hiện tại.
     *
     * Quy trình:
     * 1. Nếu chưa có currentUser trong FirebaseAuth, chuyển sang LoginActivity.
     * 2. Nếu đã đăng nhập, lấy userId hiện tại.
     * 3. Tải hồ sơ users/{userId} từ Firestore.
     * 4. Nếu tài khoản bị khóa, đăng xuất và quay về LoginActivity.
     * 5. Nếu tài khoản hợp lệ, chuyển sang ProfileActivity.
     */
    private void checkLoginState() {
        if (!authManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        String currentUserId = authManager.getCurrentUserId();

        if (ValidationUtils.isEmpty(currentUserId)) {
            authManager.logout();
            navigateToLogin();
            return;
        }

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (Constants.STATUS_LOCKED.equals(user.getStatus())) {
                    authManager.logout();

                    Toast.makeText(
                            SplashActivity.this,
                            "Tài khoản đã bị khóa",
                            Toast.LENGTH_SHORT
                    ).show();

                    navigateToLogin();
                    return;
                }

                navigateToProfile();
            }

            @Override
            public void onFailure(String errorMessage) {
                // Nếu không lấy được hồ sơ thì đăng xuất để tránh trạng thái đăng nhập lỗi.
                authManager.logout();

                Toast.makeText(
                        SplashActivity.this,
                        errorMessage,
                        Toast.LENGTH_SHORT
                ).show();

                navigateToLogin();
            }
        });
    }

    /**
     * Chuyển sang màn hình đăng nhập và xóa back stack.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    /**
     * Chuyển sang màn hình hồ sơ cá nhân và xóa back stack.
     *
     * Sau này khi nhóm có HomeFeedActivity, có thể đổi ProfileActivity thành HomeFeedActivity.
     */
    private void navigateToProfile() {
        Intent intent = new Intent(SplashActivity.this, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
