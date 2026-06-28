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
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.util.ValidationUtils;

/**
 * RegisterActivity xử lý màn hình đăng ký tài khoản.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Chức năng chính:
 * - Nhận họ tên, username, email, mật khẩu và xác nhận mật khẩu.
 * - Kiểm tra dữ liệu nhập theo yêu cầu đề tài.
 * - Gọi AuthManager để đăng ký tài khoản bằng Firebase Authentication.
 * - Lưu hồ sơ người dùng vào Firestore thông qua AuthManager/UserRepository.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName;
    private EditText edtUsername;
    private EditText edtEmail;
    private EditText edtPassword;
    private EditText edtConfirmPassword;
    private Button btnRegister;
    private TextView txtGoToLogin;
    private ProgressBar progressBar;

    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo AuthManager để xử lý đăng ký bằng Firebase.
        authManager = new AuthManager();

        initViews();
        setupEvents();
    }

    /**
     * Ánh xạ các thành phần giao diện từ activity_register.xml.
     */
    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtGoToLogin = findViewById(R.id.txtGoToLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Khai báo sự kiện cho nút đăng ký và text chuyển về đăng nhập.
     */
    private void setupEvents() {
        findViewById(R.id.btnBackRegister).setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> handleRegister());

        txtGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Xử lý khi người dùng bấm nút Đăng ký.
     *
     * Quy trình:
     * 1. Lấy dữ liệu từ form.
     * 2. Kiểm tra dữ liệu nhập.
     * 3. Gọi AuthManager.register().
     * 4. Nếu thành công, chuyển về LoginActivity.
     */
    private void handleRegister() {
        String fullName = edtFullName.getText().toString().trim();
        // Người 1 thực hiện: chuẩn hóa username/email trước khi validate và gửi lên Firebase/Firestore.
        String username = ValidationUtils.normalizeUsername(edtUsername.getText().toString());
        String email = ValidationUtils.normalizeEmail(edtEmail.getText().toString());
        String password = edtPassword.getText().toString();
        String confirmPassword = edtConfirmPassword.getText().toString();

        if (!validateInput(fullName, username, email, password, confirmPassword)) {
            return;
        }

        setLoading(true);

        authManager.register(fullName, username, email, password, new AuthManager.AuthCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);

                Toast.makeText(
                        RegisterActivity.this,
                        "Đăng ký thành công. Vui lòng đăng nhập.",
                        Toast.LENGTH_SHORT
                ).show();

                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Kiểm tra dữ liệu nhập trên màn hình đăng ký.
     *
     * @return true nếu dữ liệu hợp lệ, false nếu có lỗi
     */
    private boolean validateInput(String fullName,
                                  String username,
                                  String email,
                                  String password,
                                  String confirmPassword) {

        if (ValidationUtils.isEmpty(fullName)
                || ValidationUtils.isEmpty(username)
                || ValidationUtils.isEmpty(email)
                || ValidationUtils.isEmpty(password)
                || ValidationUtils.isEmpty(confirmPassword)) {

            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return false;
        }

        // Người 1 thực hiện: kiểm tra username theo quy tắc mới trước khi tạo tài khoản.
        if (!ValidationUtils.isValidUsername(username)) {
            Toast.makeText(this, ValidationUtils.getUsernameErrorMessage(), Toast.LENGTH_LONG).show();
            return false;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            Toast.makeText(this, "Email không hợp lệ", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!ValidationUtils.isPasswordMatched(password, confirmPassword)) {
            Toast.makeText(this, "Mật khẩu xác nhận không trùng khớp", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    /**
     * Bật/tắt trạng thái loading khi đang đăng ký.
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!isLoading);
        txtGoToLogin.setEnabled(!isLoading);
    }
}
