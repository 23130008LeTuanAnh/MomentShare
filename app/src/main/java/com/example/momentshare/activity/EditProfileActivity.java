package com.example.momentshare.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.StorageRepository;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.ValidationUtils;

/**
 * EditProfileActivity cho phép người dùng chỉnh sửa hồ sơ cá nhân.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Chức năng chính:
 * - Kiểm tra người dùng đã đăng nhập hay chưa.
 * - Tải thông tin hiện tại từ Firestore.
 * - Hiển thị họ tên, bio và avatar hiện tại.
 * - Cho phép chọn avatar mới từ thư viện ảnh.
 * - Upload avatar mới lên Firebase Storage.
 * - Cập nhật fullName, bio và avatarUrl vào Firestore.
 */
public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar;
    private EditText edtFullName;
    private EditText edtBio;
    private ProgressBar progressBar;
    private Button btnSaveProfile;
    private Button btnCancelEdit;

    private AuthManager authManager;
    private UserRepository userRepository;
    private StorageRepository storageRepository;

    private Uri selectedAvatarUri;
    private ActivityResultLauncher<String> pickAvatarLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // AuthManager dùng để kiểm tra tài khoản hiện tại.
        authManager = new AuthManager();

        // UserRepository dùng để đọc và cập nhật dữ liệu hồ sơ trên Firestore.
        userRepository = new UserRepository();

        // StorageRepository dùng để upload avatar lên Firebase Storage.
        storageRepository = new StorageRepository();

        initAvatarPicker();
        initViews();
        setupEvents();
        loadCurrentProfile();
    }

    /**
     * Khởi tạo launcher chọn ảnh từ thư viện.
     *
     * ActivityResultContracts.GetContent() giúp mở bộ chọn file ảnh
     * và trả về Uri của ảnh người dùng đã chọn.
     */
    private void initAvatarPicker() {
        pickAvatarLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedAvatarUri = uri;

                        // Hiển thị ảnh vừa chọn lên màn hình trước khi upload.
                        imgAvatar.setImageURI(uri);
                    }
                }
        );
    }

    /**
     * Ánh xạ các view từ file activity_edit_profile.xml.
     */
    private void initViews() {
        imgAvatar = findViewById(R.id.imgAvatar);
        edtFullName = findViewById(R.id.edtFullName);
        edtBio = findViewById(R.id.edtBio);
        progressBar = findViewById(R.id.progressBar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
    }

    /**
     * Khai báo sự kiện lưu, hủy và chọn avatar.
     */
    private void setupEvents() {
        imgAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));

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
     * 4. Đưa fullName, bio và avatar hiện tại lên giao diện.
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

                // Nếu user đã có avatarUrl, tải avatar bằng Glide.
                if (!ValidationUtils.isEmpty(user.getAvatarUrl())) {
                    Glide.with(EditProfileActivity.this)
                            .load(user.getAvatarUrl())
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .circleCrop()
                            .into(imgAvatar);
                } else {
                    imgAvatar.setImageResource(R.mipmap.ic_launcher);
                }
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
     * 3. Nếu không chọn avatar mới, chỉ cập nhật fullName và bio.
     * 4. Nếu có chọn avatar mới, upload avatar rồi cập nhật avatarUrl vào Firestore.
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

        if (selectedAvatarUri == null) {
            updateProfileWithoutAvatar(currentUserId, fullName, bio);
        } else {
            uploadAvatarAndUpdateProfile(currentUserId, fullName, bio);
        }
    }

    /**
     * Cập nhật hồ sơ khi người dùng không chọn avatar mới.
     */
    private void updateProfileWithoutAvatar(String userId, String fullName, String bio) {
        userRepository.updateProfile(userId, fullName, bio, new UserRepository.ActionCallback() {
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
     * Upload avatar lên Firebase Storage rồi cập nhật hồ sơ với avatarUrl mới.
     */
    private void uploadAvatarAndUpdateProfile(String userId, String fullName, String bio) {
        storageRepository.uploadAvatar(userId, selectedAvatarUri, new StorageRepository.UploadCallback() {
            @Override
            public void onSuccess(String downloadUrl) {
                userRepository.updateProfileWithAvatar(
                        userId,
                        fullName,
                        bio,
                        downloadUrl,
                        new UserRepository.ActionCallback() {
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

                                Toast.makeText(
                                        EditProfileActivity.this,
                                        errorMessage,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                );
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);

                Toast.makeText(EditProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Bật/tắt trạng thái loading khi đang tải, upload hoặc cập nhật hồ sơ.
     */
    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSaveProfile.setEnabled(!isLoading);
        btnCancelEdit.setEnabled(!isLoading);
        edtFullName.setEnabled(!isLoading);
        edtBio.setEnabled(!isLoading);
        imgAvatar.setEnabled(!isLoading);
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
