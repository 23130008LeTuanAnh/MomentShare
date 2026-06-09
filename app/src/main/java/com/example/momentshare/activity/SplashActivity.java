package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;

/**
 * SplashActivity là màn hình khởi động đầu tiên của ứng dụng MomentShare.
 *
 * Nhiệm vụ hiện tại:
 * - Hiển thị tên ứng dụng và khẩu hiệu trong thời gian ngắn.
 * - Điều hướng sang LoginActivity để người dùng đăng nhập.
 *
 * Ghi chú triển khai tiếp theo:
 * - Sau khi tích hợp Firebase Authentication, màn hình này sẽ kiểm tra trạng thái đăng nhập.
 * - Nếu người dùng đã đăng nhập và tài khoản active, chuyển sang ProfileActivity hoặc HomeFeedActivity.
 * - Nếu chưa đăng nhập, chuyển sang LoginActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY_MS = 1500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Tạm thời chuyển sang màn hình đăng nhập sau 1.5 giây.
        // Phần kiểm tra đăng nhập thật sẽ được bổ sung khi tích hợp Firebase.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, SPLASH_DELAY_MS);
    }
}
