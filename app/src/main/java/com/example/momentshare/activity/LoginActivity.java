package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.helper.FcmTokenManager;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.util.ValidationUtils;

/**
 * LoginActivity quản lý màn hình đăng nhập của người dùng.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Chức năng chính:
 * - Nhận email/username và mật khẩu.
 * - Kiểm tra dữ liệu nhập.
 * - Gọi AuthManager để đăng nhập bằng Firebase Authentication.
 * - Kiểm tra trạng thái tài khoản active/locked trong Firestore.
 * - Nếu đăng nhập thành công, đăng ký FCM token cho thiết bị.
 * - Chuyển sang ProfileActivity.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText edtAccount;
    private EditText edtPassword;
    private Button btnLogin;
    private TextView txtGoToRegister;
    private ProgressBar progressBar;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // AuthManager xử lý đăng nhập bằng email hoặc username thông qua Firebase.
        authManager = new AuthManager();

        initViews();
        setupEvents();
    }

    /**
     * Ánh xạ các view từ file activity_login.xml.
     */
    private void initViews() {
        edtAccount = findViewById(R.id.edtAccount);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Khai báo các sự kiện bấm nút trên màn hình đăng nhập.
     */
    private void setupEvents() {
        findViewById(R.id.btnBackLogin).setOnClickListener(v -> finish());

        btnLogin.setOnClickListener(v -> handleLogin());

        txtGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    /**
     * Xử lý khi người dùng bấm nút Đăng nhập.
     *
     * Quy trình:
     * 1. Lấy email/username và mật khẩu.
     * 2. Kiểm tra dữ liệu rỗng.
     * 3. Gọi AuthManager.loginWithEmailOrUsername().
     * 4. Nếu đăng nhập thành công, đăng ký FCM token cho thiết bị.
     * 5. Chuyển sang ProfileActivity.
     */
    private void handleLogin() {
        String account = edtAccount.getText().toString().trim();
        String password = edtPassword.getText().toString();

        if (!validateInput(account, password)) {
            return;
        }

        setLoading(true);

        authManager.loginWithEmailOrUsername(account, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);

                // Người 5 thực hiện: sau khi đăng nhập thành công,
                // đăng ký thiết bị hiện tại với Firebase Cloud Messaging.
                // Hàm này sẽ:
                // 1. Xin quyền POST_NOTIFICATIONS trên Android 13+.
                // 2. Lấy FCM token của thiết bị.
                // 3. Lưu token vào Firestore tại users/{uid}.
                new FcmTokenManager().registerCurrentUserDevice(LoginActivity.this);

                Toast.makeText(
                        LoginActivity.this,
                        "Đăng nhập thành công",
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent = new Intent(LoginActivity.this, ProfileActivity.class);

                // Xóa các màn hình Login/Register khỏi back stack để người dùng không quay lại bằng nút Back.
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                startActivity(intent);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Kiểm tra dữ liệu nhập trên màn hình đăng nhập.
     *
     * @param account email hoặc username
     * @param password mật khẩu
     * @return true nếu hợp lệ, false nếu thiếu dữ liệu
     */
    private boolean validateInput(String account, String password) {
        if (ValidationUtils.isEmpty(account) || ValidationUtils.isEmpty(password)) {
            Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Bật/tắt trạng thái loading khi đang đăng nhập.
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!isLoading);
        txtGoToRegister.setEnabled(!isLoading);
    }
}
