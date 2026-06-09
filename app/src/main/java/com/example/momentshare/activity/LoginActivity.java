package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;

/**
 * LoginActivity quản lý màn hình đăng nhập của người dùng.
 *
 * Nhiệm vụ thuộc Người 1:
 * - Nhận email/username và mật khẩu.
 * - Kiểm tra dữ liệu nhập.
 * - Sau này gọi Firebase Authentication để đăng nhập.
 * - Kiểm tra trạng thái tài khoản active/locked trên Firestore.
 */
public class LoginActivity extends AppCompatActivity {

    private Button btnLogin;
    private TextView txtGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupEvents();
    }

    /**
     * Ánh xạ các view từ file activity_login.xml.
     */
    private void initViews() {
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
    }

    /**
     * Khai báo các sự kiện bấm nút trên màn hình đăng nhập.
     */
    private void setupEvents() {
        btnLogin.setOnClickListener(v -> {
            // Tạm thời hiển thị thông báo để xác nhận màn hình hoạt động.
            // Logic đăng nhập Firebase sẽ được viết ở commit sau.
            Toast.makeText(this, "Chuc nang dang nhap se duoc cai dat o buoc tiep theo", Toast.LENGTH_SHORT).show();
        });

        txtGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
