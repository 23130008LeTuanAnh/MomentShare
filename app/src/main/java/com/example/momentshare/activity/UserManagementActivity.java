package com.example.momentshare.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * UserManagementActivity cho phép Admin xem danh sách, khóa/mở khóa và đổi quyền tài khoản.
 * File này thuộc phần Người 5 - Quản lý người dùng.
 * Có kiểm tra để admin không tự khóa hoặc tự hạ quyền tài khoản đang đăng nhập.
 */
public class UserManagementActivity extends AppCompatActivity {

    private ListView listUsers;
    private TextView txtEmptyUsers;
    private ProgressBar progressBar;

    private AuthManager authManager;
    private UserRepository userRepository;
    private AdminRepository adminRepository;

    private final List<User> users = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        authManager = new AuthManager();
        userRepository = new UserRepository();
        adminRepository = new AdminRepository();

        initViews();
        checkAdminPermissionAndLoadUsers();
    }

    private void initViews() {
        listUsers = findViewById(R.id.listUsers);
        txtEmptyUsers = findViewById(R.id.txtEmptyUsers);
        progressBar = findViewById(R.id.progressBar);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listUsers.setAdapter(adapter);
        listUsers.setOnItemClickListener((parent, view, position, id) -> showUserActionDialog(users.get(position)));
    }

    private void checkAdminPermissionAndLoadUsers() {
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            finish();
            return;
        }

        setLoading(true);
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!Constants.ROLE_ADMIN.equals(user.getRole())) {
                    Toast.makeText(UserManagementActivity.this, "Bạn không có quyền quản lý người dùng", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadUsers() {
        adminRepository.loadUsers(new AdminRepository.UsersCallback() {
            @Override
            public void onSuccess(List<User> result) {
                setLoading(false);
                users.clear();
                users.addAll(result);
                renderUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderUsers() {
        adapter.clear();

        for (User user : users) {
            String role = safeText(user.getRole(), Constants.ROLE_USER);
            String status = safeText(user.getStatus(), Constants.STATUS_ACTIVE);
            adapter.add(safeText(user.getFullName(), "Người dùng")
                    + "\n@" + safeText(user.getUsername(), "username")
                    + " - " + role + " - " + status);
        }

        adapter.notifyDataSetChanged();
        txtEmptyUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showUserActionDialog(User user) {
        boolean isLocked = Constants.STATUS_LOCKED.equals(user.getStatus());
        boolean isAdmin = Constants.ROLE_ADMIN.equals(user.getRole());

        String[] actions = {
                isLocked ? "Mở khóa tài khoản" : "Khóa tài khoản",
                isAdmin ? "Chuyển về USER" : "Cấp quyền ADMIN"
        };

        new AlertDialog.Builder(this)
                .setTitle(safeText(user.getFullName(), "Người dùng"))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        updateUserStatus(user, isLocked ? Constants.STATUS_ACTIVE : Constants.STATUS_LOCKED);
                    } else {
                        updateUserRole(user, isAdmin ? Constants.ROLE_USER : Constants.ROLE_ADMIN);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateUserStatus(User user, String newStatus) {
        if (currentUserId.equals(user.getUserId()) && Constants.STATUS_LOCKED.equals(newStatus)) {
            Toast.makeText(this, "Không thể khóa chính tài khoản đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        adminRepository.updateUserStatus(user.getUserId(), newStatus, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserRole(User user, String newRole) {
        if (currentUserId.equals(user.getUserId()) && Constants.ROLE_USER.equals(newRole)) {
            Toast.makeText(this, "Không thể tự hạ quyền tài khoản đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        adminRepository.updateUserRole(user.getUserId(), newRole, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, "Đã cập nhật quyền", Toast.LENGTH_SHORT).show();
                loadUsers();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        listUsers.setEnabled(!isLoading);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
