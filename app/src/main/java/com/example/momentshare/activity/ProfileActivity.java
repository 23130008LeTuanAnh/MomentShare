package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;

/**
 * ProfileActivity hiển thị hồ sơ cá nhân của người dùng.
 *
 * Nhiệm vụ thuộc Người 1:
 * - Hiển thị họ tên, username, email, bio, avatar.
 * - Hiển thị số bạn bè, số ảnh đã gửi và số ảnh đã nhận.
 * - Cho phép chuyển sang EditProfileActivity.
 * - Cho phép đăng xuất khỏi tài khoản hiện tại.
 */
public class ProfileActivity extends AppCompatActivity {

    private Button btnEditProfile;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupEvents();
    }

    /**
     * Ánh xạ các view từ file activity_profile.xml.
     */
    private void initViews() {
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);
    }

    /**
     * Khai báo sự kiện cho màn hình hồ sơ.
     */
    private void setupEvents() {
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // Tạm thời điều hướng về LoginActivity.
            // Sau khi tích hợp Firebase, tại đây sẽ gọi FirebaseAuth.signOut().
            Toast.makeText(this, "Dang xuat thanh cong", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}
