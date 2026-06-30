package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.example.momentshare.util.ValidationUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * ProfileActivity hiển thị hồ sơ cá nhân.
 *
 * Đã chỉnh phần phân quyền Admin:
 * - Đọc trực tiếp role/status từ Firestore users/{uid}.
 * - Không phụ thuộc vào User.getRole()/getStatus() để tránh lỗi model không map field.
 * - User thường không thấy Admin Dashboard.
 * - Admin active sẽ thấy badge ADMIN và nút Admin Dashboard.
 */
public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    /**
     * Bật true để test xem app đọc được role/status gì từ Firestore.
     * Sau khi test xong có thể đổi thành false để không hiện Toast debug nữa.
     */
    private static final boolean DEBUG_ADMIN_ROLE = false;

    private ImageView imgAvatar;

    private TextView txtFullName;
    private TextView txtUsername;
    private TextView txtEmail;
    private TextView txtBio;
    private TextView txtRoleBadge;

    private TextView txtFriendCount;
    private TextView txtSentCount;
    private TextView txtReceivedCount;

    private Button btnEditProfile;
    private Button btnHomeFeed;
    private Button btnNotification;
    private Button btnHistory;
    private Button btnFriendList;
    private Button btnFriendRequests;
    private Button btnAdminDashboard;
    private Button btnLogout;

    private AuthManager authManager;
    private UserRepository userRepository;
    private MomentRepository momentRepository;
    private NotificationRepository notificationRepository;
    private FriendRepository friendRepository;
    private FirebaseFirestore db;

    private String currentUserId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        authManager = new AuthManager();
        userRepository = new UserRepository();
        momentRepository = new MomentRepository(this);
        notificationRepository = new NotificationRepository();
        friendRepository = new FriendRepository();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupEvents();

        // Đăng ký FCM token cho thiết bị hiện tại.
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
        txtRoleBadge = findViewById(R.id.txtRoleBadge);

        txtFriendCount = findViewById(R.id.txtFriendCount);
        txtSentCount = findViewById(R.id.txtSentCount);
        txtReceivedCount = findViewById(R.id.txtReceivedCount);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnHomeFeed = findViewById(R.id.btnHomeFeed);
        btnNotification = findViewById(R.id.btnNotification);
        btnHistory = findViewById(R.id.btnHistory);
        btnFriendList = findViewById(R.id.btnFriendList);
        btnFriendRequests = findViewById(R.id.btnFriendRequests);
        btnAdminDashboard = findViewById(R.id.btnAdminDashboard);
        btnLogout = findViewById(R.id.btnLogout);

        // Mặc định luôn ẩn Admin UI.
        // Chỉ hiện sau khi Firestore xác nhận role = ADMIN.
        hideAdminUi();
    }

    private void setupEvents() {
        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnHomeFeed.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HomeFeedActivity.class);
            startActivity(intent);
        });

        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, NotificationActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnFriendList.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FriendListActivity.class);
            startActivity(intent);
        });

        btnFriendRequests.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, FriendRequestActivity.class);
            startActivity(intent);
        });

        btnAdminDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, AdminDashboardActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> handleLogout());
    }

    private void loadUserProfile() {
        if (!authManager.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        currentUserId = authManager.getCurrentUserId();

        if (ValidationUtils.isEmpty(currentUserId)) {
            Toast.makeText(this, "Không thể xác định tài khoản hiện tại", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }

        showLoadingText();

        // Gọi ngay sau khi có UID.
        // Nếu là ADMIN thì hàm này sẽ bật lại Admin Dashboard sau khi showLoadingText đã ẩn nó.
        applyAdminVisibilityFromFirestore(currentUserId);

        loadBadgeCounts(currentUserId);
        loadProfileStatistics(currentUserId);

        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                displayUserProfile(user);

                // Gọi lại lần nữa sau khi load profile xong để chắc chắn nút Admin không bị ẩn lại.
                applyAdminVisibilityFromFirestore(currentUserId);
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
        if (user == null) {
            showLoadingText();
            return;
        }

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
    }

    /**
     * Đọc trực tiếp role/status từ Firestore để phân biệt ADMIN và USER.
     *
     * Firestore cần có:
     * users/{uid}
     * role: "ADMIN" hoặc "USER"
     * status: "active" hoặc "locked"
     */
    private void applyAdminVisibilityFromFirestore(String userId) {
        if (ValidationUtils.isEmpty(userId)) {
            Log.d(TAG, "userId rỗng, ẩn Admin UI");
            hideAdminUi();
            return;
        }

        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        Log.d(TAG, "Không tìm thấy document users/" + userId);
                        hideAdminUi();

                        if (DEBUG_ADMIN_ROLE) {
                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Không tìm thấy users/" + userId,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                        return;
                    }

                    String role = getStringField(documentSnapshot, "role");
                    String status = getStringField(documentSnapshot, "status");

                    String normalizedRole = role.trim();
                    String normalizedStatus = status.trim();

                    Log.d(TAG, "Current userId = " + userId);
                    Log.d(TAG, "Firestore role = " + normalizedRole);
                    Log.d(TAG, "Firestore status = " + normalizedStatus);

                    if (DEBUG_ADMIN_ROLE) {
                        Toast.makeText(
                                ProfileActivity.this,
                                "role=" + normalizedRole + ", status=" + normalizedStatus,
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    boolean isAdmin = "ADMIN".equalsIgnoreCase(normalizedRole);

                    // Nếu status chưa có thì vẫn cho ADMIN hiện để dễ demo.
                    // Nếu status = locked thì không hiện.
                    boolean isActive = normalizedStatus.isEmpty()
                            || "active".equalsIgnoreCase(normalizedStatus);

                    if (isAdmin && isActive) {
                        showAdminUi();
                    } else {
                        hideAdminUi();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi đọc role/status từ Firestore", e);
                    hideAdminUi();

                    if (DEBUG_ADMIN_ROLE) {
                        Toast.makeText(
                                ProfileActivity.this,
                                "Lỗi đọc role/status: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                });
    }

    private String getStringField(DocumentSnapshot documentSnapshot, String fieldName) {
        String value = documentSnapshot.getString(fieldName);
        return value == null ? "" : value;
    }

    private void showAdminUi() {
        if (btnAdminDashboard != null) {
            btnAdminDashboard.setVisibility(View.VISIBLE);
        }

        if (txtRoleBadge != null) {
            txtRoleBadge.setText("ADMIN");
            txtRoleBadge.setVisibility(View.VISIBLE);
        }

        Log.d(TAG, "Đã hiện Admin Dashboard");
    }

    private void hideAdminUi() {
        if (btnAdminDashboard != null) {
            btnAdminDashboard.setVisibility(View.GONE);
        }

        if (txtRoleBadge != null) {
            txtRoleBadge.setVisibility(View.GONE);
        }

        Log.d(TAG, "Đã ẩn Admin Dashboard");
    }

    private void loadBadgeCounts(String userId) {
        btnNotification.setText("Thông báo");
        btnFriendRequests.setText("Lời mời kết bạn");

        notificationRepository.countUnreadNotifications(userId, new NotificationRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                if (count > 0) {
                    btnNotification.setText("Thông báo (" + count + ")");
                } else {
                    btnNotification.setText("Thông báo");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                btnNotification.setText("Thông báo");
            }
        });

        friendRepository.countPendingRequests(userId, new FriendRepository.CountCallback() {
            @Override
            public void onSuccess(int count) {
                if (count > 0) {
                    btnFriendRequests.setText("Lời mời kết bạn (" + count + ")");
                } else {
                    btnFriendRequests.setText("Lời mời kết bạn");
                }
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

        btnNotification.setText("Thông báo");
        btnFriendRequests.setText("Lời mời kết bạn");

        hideAdminUi();
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
        finish();
    }
}