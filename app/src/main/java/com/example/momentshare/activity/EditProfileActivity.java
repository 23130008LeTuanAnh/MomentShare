package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.ValidationUtils;

/**
 * EditProfileActivity cho phép người dùng chỉnh sửa thông tin hồ sơ cá nhân.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Chức năng chính:
 * - Kiểm tra người dùng đã đăng nhập hay chưa.
 * - Tải thông tin hiện tại từ Firestore.
 * - Hiển thị họ tên và bio hiện tại lên form.
 * - Cho phép cập nhật họ tên và bio.
 * - Lưu dữ liệu mới vào collection users trên Firestore.
 *
 * Ghi chú:
 * - Avatar sẽ được mở rộng sau bằng Firebase Storage.
 * - Hiện tại chỉ xử lý cập nhật fullName và bio để hoàn thành bản cơ bản.
 */
public class EditProfileActivity extends AppCompatActivity {

    private EditText edtFullName;
    private EditText edtBio;
    private ProgressBar progressBar;
    private Button btnSaveProfile;
    private Button btnCancelEdit;

    private AuthManager authManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // AuthManager dùng để kiểm tra tài khoản hiện tại.
        authManager = new AuthManager();

        // UserRepository dùng để đọc và cập nhật dữ liệu hồ sơ trên Firestore.
        userRepository = new UserRepository();

        initViews();
        setupEvents();
        loadCurrentProfile();
    }

    /**
     * Ánh xạ các view từ file activity_edit_profile.xml.
     */
    private void initViews() {
        edtFullName = findViewById(R.id.edtFullName);
        edtBio = findViewById(R.id.edtBio);
        progressBar = findViewById(R.id.progressBar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
    }

    /**
     * Khai báo sự kiện lưu hoặc hủy chỉnh sửa hồ sơ.
     */
    private void setupEvents() {
        btnSaveProfile.setOnClickListener(v -> handleSaveProfile());

        btnCancelEdit.setOnClickListener(v -> finish());
    }

    /**
     * Tải thông tin hồ sơ hiện tại từ Firestore để hiển thị lên form.
     *
     * Quy trình:
     * 1. Kiểm tra người dùng đã đăng nhập chưa.
     * 2. Lấy currentUserId từ FirebaseAuth.
     * 3. Gọi UserRepository.getUserById().
     * 4. Đưa fullName và bio hiện tại vào EditText.
     */
    private void loadCurrentProfile() {
        if (!authManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        String currentUserId = authManager.getCurrentUserId();

        if (ValidationUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Không thể xác định tài khoản hiện tại", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        setLoading(true);

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);

                edtFullName.setText(user.getFullName() == null ? "" : user.getFullName());
                edtBio.setText(user.getBio() == null ? "" : user.getBio());
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);

                Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    /**
     * Xử lý khi người dùng bấm nút Lưu hồ sơ.
     *
     * Quy trình:
     * 1. Lấy fullName và bio từ form.
     * 2. Kiểm tra họ tên không được rỗng.
     * 3. Gọi UserRepository.updateProfile().
     * 4. Nếu thành công, quay lại ProfileActivity.
     */
    private void handleSaveProfile() {
        String fullName = edtFullName.getText().toString().trim();
        String bio = edtBio.getText().toString().trim();

        if (ValidationUtils.isEmpty(fullName)) {
            Toast.makeText(this, "Họ tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = authManager.getCurrentUserId();

        if (ValidationUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Không thể xác định tài khoản hiện tại", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        setLoading(true);

        userRepository.updateProfile(currentUserId, fullName, bio, new UserRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);

                Toast.makeText(
                        EditProfileActivity.this,
                        "Cập nhật hồ sơ thành công",
                        Toast.LENGTH_SHORT
                ).show();

                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);

                Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Bật/tắt trạng thái loading khi đang tải hoặc cập nhật hồ sơ.
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        btnCancelEdit.setEnabled(!isLoading);
        edtFullName.setEnabled(!isLoading);
        edtBio.setEnabled(!isLoading);
    }

    /**
     * Điều hướng về LoginActivity nếu người dùng chưa đăng nhập.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
