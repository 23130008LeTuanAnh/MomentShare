package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.helper.TestDataSeeder;
import com.example.momentshare.model.StatisticsModel;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.Constants;

/**
 * AdminDashboardActivity là màn hình tổng quan cho quản trị viên.
 *
 * Đã chỉnh:
 * - Nút Quản lý báo cáo hiển thị số báo cáo đang chờ.
 */
public class AdminDashboardActivity extends AppCompatActivity {

    private TextView txtAdminTitle;
    private TextView txtTotalUsers;
    private TextView txtTotalMoments;
    private TextView txtTotalReports;
    private TextView txtPendingReports;
    private TextView txtLockedUsers;
    private ProgressBar progressBar;
    private Button btnManageUsers;
    private Button btnManageReports;
    private Button btnStatistics;
    private Button btnSeedDemo;
    private Button btnBackProfile;

    private AuthManager authManager;
    private UserRepository userRepository;
    private AdminRepository adminRepository;
    private TestDataSeeder testDataSeeder;

    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        authManager = new AuthManager();
        userRepository = new UserRepository();
        adminRepository = new AdminRepository();
        testDataSeeder = new TestDataSeeder();

        initViews();
        setupEvents();
        checkAdminPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentUserId != null) {
            loadStatistics();
        }
    }

    private void initViews() {
        txtAdminTitle = findViewById(R.id.txtAdminTitle);
        txtTotalUsers = findViewById(R.id.txtTotalUsers);
        txtTotalMoments = findViewById(R.id.txtTotalMoments);
        txtTotalReports = findViewById(R.id.txtTotalReports);
        txtPendingReports = findViewById(R.id.txtPendingReports);
        txtLockedUsers = findViewById(R.id.txtLockedUsers);
        progressBar = findViewById(R.id.progressBar);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnManageReports = findViewById(R.id.btnManageReports);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnSeedDemo = findViewById(R.id.btnSeedDemo);
        btnBackProfile = findViewById(R.id.btnBackProfile);
    }

    private void setupEvents() {
        btnManageUsers.setOnClickListener(v -> startActivity(new Intent(this, UserManagementActivity.class)));
        btnManageReports.setOnClickListener(v -> startActivity(new Intent(this, ReportManagementActivity.class)));
        btnStatistics.setOnClickListener(v -> startActivity(new Intent(this, StatisticsActivity.class)));
        btnSeedDemo.setOnClickListener(v -> seedDemoData());
        btnBackProfile.setOnClickListener(v -> finish());
    }

    private void checkAdminPermission() {
        if (!authManager.isLoggedIn()) {
            goToLogin();
            return;
        }

        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            goToLogin();
            return;
        }

        setLoading(true);
        userRepository.getUserById(currentUserId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (!Constants.ROLE_ADMIN.equals(user.getRole())) {
                    Toast.makeText(AdminDashboardActivity.this, "Tài khoản không có quyền Admin", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                txtAdminTitle.setText("Admin Dashboard - " + safeText(user.getFullName(), "MomentShare"));
                loadStatistics();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(AdminDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadStatistics() {
        setLoading(true);
        adminRepository.loadStatistics(new AdminRepository.StatisticsCallback() {
            @Override
            public void onSuccess(StatisticsModel statistics) {
                setLoading(false);
                displayStatistics(statistics);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(AdminDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayStatistics(StatisticsModel statistics) {
        txtTotalUsers.setText(String.valueOf(statistics.getTotalUsers()));
        txtTotalMoments.setText(String.valueOf(statistics.getTotalMoments()));
        txtTotalReports.setText(String.valueOf(statistics.getTotalReports()));
        txtPendingReports.setText(String.valueOf(statistics.getPendingReports()));
        txtLockedUsers.setText(String.valueOf(statistics.getLockedUsers()));

        if (statistics.getPendingReports() > 0) {
            btnManageReports.setText("Quản lý báo cáo (" + statistics.getPendingReports() + ")");
        } else {
            btnManageReports.setText("Quản lý báo cáo");
        }
    }

    private void seedDemoData() {
        if (currentUserId == null) return;

        setLoading(true);
        testDataSeeder.seedAdminDemoData(currentUserId, new TestDataSeeder.SeedCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(AdminDashboardActivity.this, "Đã tạo dữ liệu demo", Toast.LENGTH_SHORT).show();
                loadStatistics();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(AdminDashboardActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnManageUsers.setEnabled(!isLoading);
        btnManageReports.setEnabled(!isLoading);
        btnStatistics.setEnabled(!isLoading);
        btnSeedDemo.setEnabled(!isLoading);
    }

    private void goToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
