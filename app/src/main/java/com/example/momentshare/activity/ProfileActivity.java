package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.ValidationUtils;

/**
 * ProfileActivity hiển thị hồ sơ cá nhân của người dùng.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Chức năng chính:
 * - Kiểm tra người dùng đã đăng nhập hay chưa.
 * - Lấy userId hiện tại từ Firebase Authentication.
 * - Tải dữ liệu người dùng từ Firestore collection users.
 * - Hiển thị avatar, họ tên, username, email, bio.
 * - Hiển thị thống kê cơ bản: số bạn bè, số ảnh đã gửi, số ảnh đã nhận.
 * - Chuyển sang EditProfileActivity để chỉnh sửa hồ sơ.
 * - Đăng xuất tài khoản hiện tại.
 */
public class ProfileActivity extends AppCompatActivity {

    private ImageView imgAvatar;

    private TextView txtFullName;
    private TextView txtUsername;
    private TextView txtEmail;
    private TextView txtBio;
    private TextView txtFriendCount;
    private TextView txtSentCount;
    private TextView txtReceivedCount;

    private Button btnEditProfile;
    private Button btnLogout;

    private AuthManager authManager;
    private UserRepository userRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // AuthManager dùng để kiểm tra đăng nhập và đăng xuất.
        authManager = new AuthManager();

        // UserRepository dùng để tải dữ liệu hồ sơ từ Firestore.
        userRepository = new UserRepository();

        initViews();
        setupEvents();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Mỗi lần quay lại ProfileActivity, tải lại hồ sơ để cập nhật dữ liệu mới nhất.
        loadUserProfile();
    }

    /**
     * Ánh xạ các view từ file activity_profile.xml.
     */
    private void initViews() {
        imgAvatar = findViewById(R.id.imgAvatar);

        txtFullName = findViewById(R.id.txtFullName);
        txtUsername = findViewById(R.id.txtUsername);
        txtEmail = findViewById(R.id.txtEmail);
        txtBio = findViewById(R.id.txtBio);
        txtFriendCount = findViewById(R.id.txtFriendCount);
        txtSentCount = findViewById(R.id.txtSentCount);
        txtReceivedCount = findViewById(R.id.txtReceivedCount);

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

        btnLogout.setOnClickListener(v -> handleLogout());
    }

    /**
     * Tải hồ sơ người dùng hiện tại từ Firestore.
     *
     * Quy trình:
     * 1. Kiểm tra FirebaseAuth có currentUser hay không.
     * 2. Lấy userId hiện tại.
     * 3. Gọi UserRepository.getUserById().
     * 4. Nếu thành công, hiển thị dữ liệu lên giao diện.
     * 5. Nếu thất bại, báo lỗi và quay lại LoginActivity.
     */
    private void loadUserProfile() {
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

        showLoadingText();

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                displayUserProfile(user);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

                // Nếu không tải được hồ sơ thì đăng xuất để tránh app ở trạng thái không hợp lệ.
                authManager.logout();
                navigateToLogin();
            }
        });
    }

    /**
     * Hiển thị dữ liệu người dùng lên màn hình Profile.
     *
     * @param user dữ liệu người dùng lấy từ Firestore
     */
    private void displayUserProfile(User user) {
        String fullName = user.getFullName();
        String username = user.getUsername();
        String email = user.getEmail();
        String bio = user.getBio();
        String avatarUrl = user.getAvatarUrl();

        txtFullName.setText(ValidationUtils.isEmpty(fullName) ? "Người dùng MomentShare" : fullName);
        txtUsername.setText(ValidationUtils.isEmpty(username) ? "@username" : "@" + username);
        txtEmail.setText(ValidationUtils.isEmpty(email) ? "Chưa có email" : email);
        txtBio.setText(ValidationUtils.isEmpty(bio) ? "Chưa có mô tả cá nhân" : bio);

        // Hiển thị avatar từ Firestore avatarUrl.
        // Nếu người dùng chưa có avatarUrl thì hiển thị icon mặc định.
        if (!ValidationUtils.isEmpty(avatarUrl)) {
            Glide.with(ProfileActivity.this)
                    .load(avatarUrl)
                    .placeholder(R.mipmap.ic_launcher)
                    .error(R.mipmap.ic_launcher)
                    .circleCrop()
                    .into(imgAvatar);
        } else {
            imgAvatar.setImageResource(R.mipmap.ic_launcher);
        }

        // Các chỉ số này sẽ được cập nhật thật khi nhóm hoàn thiện module Friends và Moments.
        txtFriendCount.setText("0 bạn bè");
        txtSentCount.setText("0 ảnh đã gửi");
        txtReceivedCount.setText("0 ảnh đã nhận");
    }

    /**
     * Hiển thị trạng thái đang tải hồ sơ.
     */
    private void showLoadingText() {
        imgAvatar.setImageResource(R.mipmap.ic_launcher);

        txtFullName.setText("Đang tải hồ sơ...");
        txtUsername.setText("@...");
        txtEmail.setText("...");
        txtBio.setText("...");
        txtFriendCount.setText("0 bạn bè");
        txtSentCount.setText("0 ảnh đã gửi");
        txtReceivedCount.setText("0 ảnh đã nhận");
    }

    /**
     * Xử lý đăng xuất tài khoản hiện tại.
     */
    private void handleLogout() {
        authManager.logout();

        Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

        navigateToLogin();
    }

    /**
     * Điều hướng về LoginActivity và xóa toàn bộ màn hình trước đó khỏi back stack.
     */
    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
