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
import com.example.momentshare.helper.AdminAccessHelper;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * UserManagementActivity cho phép Admin xem danh sách, khóa/mở khóa và đổi quyền tài khoản.
 * Có kiểm tra admin không tự khóa hoặc tự hạ quyền chính mình.
 */
public class UserManagementActivity extends AppCompatActivity {

    private ListView listUsers;
    private TextView txtEmptyUsers;
    private ProgressBar progressBar;

    private AdminAccessHelper adminAccessHelper;
    private AdminRepository adminRepository;

    private final List<User> users = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String currentAdminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        adminAccessHelper = new AdminAccessHelper();
        adminRepository = new AdminRepository();

        initViews();
        verifyAdminAndLoadUsers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentAdminId != null) {
            loadUsers();
        }
    }

    private void initViews() {
        listUsers = findViewById(R.id.listUsers);
        txtEmptyUsers = findViewById(R.id.txtEmptyUsers);
        progressBar = findViewById(R.id.progressBar);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listUsers.setAdapter(adapter);
        listUsers.setOnItemClickListener((parent, view, position, id) -> showUserActionDialog(users.get(position)));
    }

    private void verifyAdminAndLoadUsers() {
        setLoading(true);
        adminAccessHelper.requireActiveAdmin(this, (adminId, adminUser) -> {
            currentAdminId = adminId;
            loadUsers();
        });
    }

    private void loadUsers() {
        setLoading(true);
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
            adapter.add(buildUserRowText(user));
        }

        adapter.notifyDataSetChanged();
        txtEmptyUsers.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String buildUserRowText(User user) {
        String fullName = safeText(user.getFullName(), "Người dùng MomentShare");
        String username = safeText(user.getUsername(), "username");
        String email = safeText(user.getEmail(), "Chưa có email");
        String role = safeText(user.getRole(), Constants.ROLE_USER);
        String status = safeText(user.getStatus(), Constants.STATUS_ACTIVE);

        return fullName
                + "\n@" + username + " - " + email
                + "\nQuyền: " + role + " | Trạng thái: " + status;
    }

    private void showUserActionDialog(User user) {
        if (user == null || user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            Toast.makeText(this, "Không xác định được tài khoản", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isLocked = Constants.STATUS_LOCKED.equals(user.getStatus());
        boolean isAdmin = Constants.ROLE_ADMIN.equals(user.getRole());

        String[] actions = {
                isLocked ? "Mở khóa tài khoản" : "Khóa tài khoản",
                isAdmin ? "Chuyển về USER" : "Cấp quyền ADMIN"
        };

        new AlertDialog.Builder(this)
                .setTitle(safeText(user.getFullName(), "Người dùng"))
                .setMessage("@" + safeText(user.getUsername(), "username")
                        + "\n" + safeText(user.getEmail(), "Chưa có email"))
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
        if (currentAdminId != null
                && currentAdminId.equals(user.getUserId())
                && Constants.STATUS_LOCKED.equals(newStatus)) {
            Toast.makeText(this, "Không thể khóa chính tài khoản admin đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        adminRepository.updateUserStatus(user.getUserId(), newStatus, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, "Đã cập nhật trạng thái tài khoản", Toast.LENGTH_SHORT).show();
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
        if (currentAdminId != null
                && currentAdminId.equals(user.getUserId())
                && Constants.ROLE_USER.equals(newRole)) {
            Toast.makeText(this, "Không thể tự hạ quyền tài khoản admin đang đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        adminRepository.updateUserRole(user.getUserId(), newRole, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(UserManagementActivity.this, "Đã cập nhật quyền tài khoản", Toast.LENGTH_SHORT).show();
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
