package com.example.momentshare.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.StatisticsModel;
import com.example.momentshare.repository.AdminRepository;

/**
 * StatisticsActivity hiển thị thống kê hệ thống ở dạng đơn giản để phục vụ demo và báo cáo.
 * File này thuộc phần Người 5 - Thống kê hệ thống.
 */
public class StatisticsActivity extends AppCompatActivity {

    private TextView txtStatisticsContent;
    private ProgressBar progressBar;
    private Button btnReloadStatistics;

    private AdminRepository adminRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        adminRepository = new AdminRepository();

        txtStatisticsContent = findViewById(R.id.txtStatisticsContent);
        progressBar = findViewById(R.id.progressBar);
        btnReloadStatistics = findViewById(R.id.btnReloadStatistics);

        btnReloadStatistics.setOnClickListener(v -> loadStatistics());
        loadStatistics();
    }

    private void loadStatistics() {
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
        return "Tổng người dùng: " + statistics.getTotalUsers()
                + "\nTài khoản bị khóa: " + statistics.getLockedUsers()
                + "\nTổng khoảnh khắc: " + statistics.getTotalMoments()
                + "\nTổng báo cáo: " + statistics.getTotalReports()
                + "\nBáo cáo đang chờ: " + statistics.getPendingReports();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnReloadStatistics.setEnabled(!isLoading);
    }
}
