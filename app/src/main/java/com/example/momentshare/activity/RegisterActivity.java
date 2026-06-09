package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;

/**
 * RegisterActivity quản lý màn hình đăng ký tài khoản.
 *
 * Nhiệm vụ thuộc Người 1:
 * - Nhận họ tên, username, email, mật khẩu và xác nhận mật khẩu.
 * - Kiểm tra dữ liệu nhập theo yêu cầu F01.
 * - Sau này tạo tài khoản bằng Firebase Authentication.
 * - Sau này lưu thông tin người dùng vào collection users trên Firestore.
 */
public class RegisterActivity extends AppCompatActivity {

    private Button btnRegister;
    private TextView txtGoToLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupEvents();
    }

    /**
     * Ánh xạ các view từ file activity_register.xml.
     */
    private void initViews() {
        btnRegister = findViewById(R.id.btnRegister);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);
    }

    /**
     * Khai báo các sự kiện bấm nút trên màn hình đăng ký.
     */
    private void setupEvents() {
        btnRegister.setOnClickListener(v -> {
            // Tạm thời hiển thị thông báo để xác nhận màn hình hoạt động.
            // Logic đăng ký Firebase sẽ được viết ở commit sau.
            Toast.makeText(this, "Chuc nang dang ky se duoc cai dat o buoc tiep theo", Toast.LENGTH_SHORT).show();
        });

        txtGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
