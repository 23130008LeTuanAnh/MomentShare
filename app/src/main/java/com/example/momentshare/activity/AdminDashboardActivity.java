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
import com.example.momentshare.helper.AdminAccessHelper;
import com.example.momentshare.helper.TestDataSeeder;
import com.example.momentshare.model.StatisticsModel;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AdminRepository;

/**
 * AdminDashboardActivity là trang tổng quan quản trị.
 * Chỉ tài khoản ADMIN active mới được truy cập.
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

    private AdminAccessHelper adminAccessHelper;
    private AdminRepository adminRepository;
    private TestDataSeeder testDataSeeder;

    private String currentAdminId;
    private boolean adminVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        adminAccessHelper = new AdminAccessHelper();
        adminRepository = new AdminRepository();
        testDataSeeder = new TestDataSeeder();

        initViews();
        setupEvents();
        verifyAdminAndLoad();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adminVerified) {
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

    private void verifyAdminAndLoad() {
        setLoading(true);
        adminAccessHelper.requireActiveAdmin(this, new AdminAccessHelper.AdminAccessCallback() {
            @Override
            public void onGranted(String adminId, User adminUser) {
                currentAdminId = adminId;
                adminVerified = true;
                txtAdminTitle.setText("Admin Dashboard - " + safeText(adminUser.getFullName(), "MomentShare"));
                loadStatistics();
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
    }

    private void seedDemoData() {
        if (currentAdminId == null || currentAdminId.trim().isEmpty()) {
            Toast.makeText(this, "Chưa xác định tài khoản admin", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        testDataSeeder.seedAdminDemoData(currentAdminId, new TestDataSeeder.SeedCallback() {
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
        btnBackProfile.setEnabled(!isLoading);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
