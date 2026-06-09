package com.example.momentshare.activity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;

/**
 * EditProfileActivity cho phép người dùng chỉnh sửa thông tin hồ sơ cá nhân.
 *
 * Nhiệm vụ thuộc Người 1:
 * - Hiển thị thông tin hiện tại của người dùng.
 * - Cho phép cập nhật họ tên và bio.
 * - Sau này có thể mở rộng cập nhật avatar bằng Firebase Storage.
 */
public class EditProfileActivity extends AppCompatActivity {

    private Button btnSaveProfile;
    private Button btnCancelEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        setupEvents();
    }

    /**
     * Ánh xạ các view từ file activity_edit_profile.xml.
     */
    private void initViews() {
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
    }

    /**
     * Khai báo sự kiện lưu hoặc hủy chỉnh sửa hồ sơ.
     */
    private void setupEvents() {
        btnSaveProfile.setOnClickListener(v -> {
            // Tạm thời hiển thị thông báo để xác nhận màn hình hoạt động.
            // Logic cập nhật Firestore sẽ được viết ở commit sau.
            Toast.makeText(this, "Chuc nang cap nhat ho so se duoc cai dat o buoc tiep theo", Toast.LENGTH_SHORT).show();
        });

        btnCancelEdit.setOnClickListener(v -> finish());
    }
}
