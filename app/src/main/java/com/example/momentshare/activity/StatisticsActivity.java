package com.example.momentshare.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.helper.AdminAccessHelper;
import com.example.momentshare.model.StatisticsModel;
import com.example.momentshare.repository.AdminRepository;

/**
 * StatisticsActivity hiển thị thống kê chi tiết của hệ thống.
 * Chỉ ADMIN active mới được xem.
 */
public class StatisticsActivity extends AppCompatActivity {

    private TextView txtStatisticsContent;
    private ProgressBar progressBar;
    private Button btnReloadStatistics;

    private AdminAccessHelper adminAccessHelper;
    private AdminRepository adminRepository;
    private boolean adminVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        adminAccessHelper = new AdminAccessHelper();
        adminRepository = new AdminRepository();

        txtStatisticsContent = findViewById(R.id.txtStatisticsContent);
        progressBar = findViewById(R.id.progressBar);
        btnReloadStatistics = findViewById(R.id.btnReloadStatistics);

        btnReloadStatistics.setOnClickListener(v -> loadStatistics());
        verifyAdminAndLoad();
    }

    private void verifyAdminAndLoad() {
        setLoading(true);
        adminAccessHelper.requireActiveAdmin(this, (adminId, adminUser) -> {
            adminVerified = true;
            loadStatistics();
        });
    }

    private void loadStatistics() {
        if (!adminVerified) {
            return;
        }

        setLoading(true);
        adminRepository.loadStatistics(new AdminRepository.StatisticsCallback() {
            @Override
            public void onSuccess(StatisticsModel statistics) {
                setLoading(false);
                txtStatisticsContent.setText(buildStatisticsText(statistics));
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(StatisticsActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String buildStatisticsText(StatisticsModel statistics) {
        return "THỐNG KÊ NGƯỜI DÙNG"
                + "\nTổng người dùng: " + statistics.getTotalUsers()
                + "\nĐang hoạt động: " + statistics.getActiveUsers()
                + "\nTài khoản bị khóa: " + statistics.getLockedUsers()
                + "\n\nTHỐNG KÊ KHOẢNH KHẮC"
                + "\nTổng khoảnh khắc: " + statistics.getTotalMoments()
                + "\nĐang hiển thị: " + statistics.getActiveMoments()
                + "\nĐã bị ẩn: " + statistics.getHiddenMoments()
                + "\n\nTHỐNG KÊ BÁO CÁO"
                + "\nTổng báo cáo: " + statistics.getTotalReports()
                + "\nĐang chờ xử lý: " + statistics.getPendingReports()
                + "\nĐã xử lý: " + statistics.getResolvedReports()
                + "\nĐã bỏ qua: " + statistics.getIgnoredReports();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnReloadStatistics.setEnabled(!isLoading);
    }
}
