package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.momentshare.R;
import com.example.momentshare.helper.FcmTokenManager;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.FriendRepository;
import com.example.momentshare.repository.MomentRepository;
import com.example.momentshare.repository.NotificationRepository;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.ValidationUtils;

import java.util.List;

/**
 * ProfileActivity hiển thị hồ sơ cá nhân.
 *
 * Đã chỉnh:
 * - Hiển thị số thông báo chưa đọc trên nút Thông báo.
 * - Hiển thị số lời mời kết bạn đang chờ trên nút Lời mời kết bạn.
 * - Tự cập nhật lại badge trong onResume().
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
    private Button btnHomeFeed;
    private Button btnNotification;
    private Button btnFriendList;
    private Button btnFriendRequests;
    private Button btnAdminDashboard;
    private Button btnLogout;

    private AuthManager authManager;
    private UserRepository userRepository;
    private MomentRepository momentRepository;
    private NotificationRepository notificationRepository;
    private FriendRepository friendRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = new AuthManager();
        userRepository = new UserRepository();
        momentRepository = new MomentRepository(this);
        notificationRepository = new NotificationRepository();
        friendRepository = new FriendRepository();

        initViews();
        setupEvents();

        new FcmTokenManager().registerCurrentUserDevice(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

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
        btnHomeFeed = findViewById(R.id.btnHomeFeed);
        btnNotification = findViewById(R.id.btnNotification);
        btnFriendList = findViewById(R.id.btnFriendList);
        btnFriendRequests = findViewById(R.id.btnFriendRequests);
        btnAdminDashboard = findViewById(R.id.btnAdminDashboard);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupEvents() {
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));
        btnHomeFeed.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, HomeFeedActivity.class)));
        btnNotification.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, NotificationActivity.class)));
        btnAdminDashboard.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AdminDashboardActivity.class)));
        btnFriendList.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, FriendListActivity.class)));
        btnFriendRequests.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, FriendRequestActivity.class)));
        btnLogout.setOnClickListener(v -> handleLogout());
    }

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
        loadBadgeCounts(currentUserId);

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                displayUserProfile(user);
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ProfileActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                authManager.logout();
                navigateToLogin();
            }
        });
    }

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

        btnAdminDashboard.setVisibility(Constants.ROLE_ADMIN.equals(user.getRole()) ? View.VISIBLE : View.GONE);
        loadProfileStatistics(user.getUserId());
    }

    private void loadBadgeCounts(String userId) {
        btnNotification.setText("Thông báo");
        btnFriendRequests.setText("Lời mời kết bạn");

        notificationRepository.countUnreadNotifications(userId, new NotificationRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                btnNotification.setText(count > 0 ? "Thông báo (" + count + ")" : "Thông báo");
            }

            @Override
            public void onFailure(String errorMessage) {
                btnNotification.setText("Thông báo");
            }
        });

        friendRepository.countPendingRequests(userId, new FriendRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                btnFriendRequests.setText(count > 0 ? "Lời mời kết bạn (" + count + ")" : "Lời mời kết bạn");
            }

            @Override
            public void onFailure(String errorMessage) {
                btnFriendRequests.setText("Lời mời kết bạn");
            }
        });
    }

    private void loadProfileStatistics(String userId) {
        if (ValidationUtils.isEmpty(userId)) {
            txtFriendCount.setText("0 bạn bè");
            txtSentCount.setText("0 ảnh đã gửi");
            txtReceivedCount.setText("0 ảnh đã nhận");
            return;
        }

        friendRepository.getFriendList(userId, new FriendRepository.UserListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                txtFriendCount.setText(users.size() + " bạn bè");
            }

            @Override
            public void onFailure(String errorMessage) {
                txtFriendCount.setText("0 bạn bè");
            }
        });

        momentRepository.countSentMoments(userId, new MomentRepository.CountCallback() {
            @Override
            public void onSuccess(long count) {
                txtSentCount.setText(count + " ảnh đã gửi");
            }

            @Override
            public void onFailure(String errorMessage) {
                txtSentCount.setText("0 ảnh đã gửi");
            }
        });

        momentRepository.countReceivedMoments(userId, new MomentRepository.CountCallback() {
            @Override
            public void onSuccess(long count) {
                txtReceivedCount.setText(count + " ảnh đã nhận");
            }

            @Override
            public void onFailure(String errorMessage) {
                txtReceivedCount.setText("0 ảnh đã nhận");
            }
        });
    }

    private void showLoadingText() {
        imgAvatar.setImageResource(R.mipmap.ic_launcher);
        txtFullName.setText("Đang tải hồ sơ...");
        txtUsername.setText("@...");
        txtEmail.setText("...");
        txtBio.setText("...");
        txtFriendCount.setText("0 bạn bè");
        txtSentCount.setText("0 ảnh đã gửi");
        txtReceivedCount.setText("0 ảnh đã nhận");
        btnAdminDashboard.setVisibility(View.GONE);
    }

    private void handleLogout() {
        authManager.logout();
        Toast.makeText(this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
