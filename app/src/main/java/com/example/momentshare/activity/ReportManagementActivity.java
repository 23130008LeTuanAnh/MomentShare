package com.example.momentshare.activity;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.ReportModel;
import com.example.momentshare.model.User;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.UserRepository;
import com.example.momentshare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * ReportManagementActivity quản lý báo cáo nội dung cho Admin.
 *
 * Đã chỉnh:
 * - Dialog xử lý report dùng nút rõ ràng: Ẩn khoảnh khắc, Bỏ qua, Hủy.
 * - Tránh lỗi giao diện chỉ hiện nút Hủy mà không hiện lựa chọn xử lý.
 */
public class ReportManagementActivity extends AppCompatActivity {

    private TextView txtEmptyReports;
    private ProgressBar progressBar;
    private ListView listReports;

    private AuthManager authManager;
    private UserRepository userRepository;
    private AdminRepository adminRepository;

    private final List<ReportModel> reports = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_management);

        authManager = new AuthManager();
        userRepository = new UserRepository();
        adminRepository = new AdminRepository();

        initViews();
        checkAdminPermissionAndLoadReports();
    }

    private void initViews() {
        txtEmptyReports = findViewById(R.id.txtEmptyReports);
        progressBar = findViewById(R.id.progressBar);
        listReports = findViewById(R.id.listReports);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listReports.setAdapter(adapter);
        listReports.setOnItemClickListener((parent, view, position, id) -> showReportActionDialog(reports.get(position)));
    }

    private void checkAdminPermissionAndLoadReports() {
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
                    Toast.makeText(ReportManagementActivity.this, "Không có quyền Admin", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                loadReports();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(ReportManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void loadReports() {
        setLoading(true);
        adminRepository.loadReports(new AdminRepository.ReportsCallback() {
            @Override
            public void onSuccess(List<ReportModel> result) {
                setLoading(false);
                reports.clear();
                reports.addAll(result);
                renderReports();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(ReportManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderReports() {
        adapter.clear();
        for (ReportModel report : reports) {
            adapter.add("Lý do: " + safeText(report.getReason(), "Không có")
                    + "\nMoment: " + safeText(report.getMomentId(), "Không rõ")
                    + " - " + safeText(report.getStatus(), Constants.REPORT_STATUS_PENDING));
        }
        adapter.notifyDataSetChanged();
        txtEmptyReports.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void showReportActionDialog(ReportModel report) {
        if (!Constants.REPORT_STATUS_PENDING.equals(report.getStatus())) {
            Toast.makeText(this, "Báo cáo này đã được xử lý", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Xử lý báo cáo")
                .setMessage("Moment ID: " + safeText(report.getMomentId(), "Không rõ")
                        + "\nLý do: " + safeText(report.getReason(), "Không có"))
                .setPositiveButton("Ẩn khoảnh khắc", (dialog, which) -> hideMomentAndResolveReport(report))
                .setNeutralButton("Bỏ qua", (dialog, which) -> ignoreReport(report))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void hideMomentAndResolveReport(ReportModel report) {
        setLoading(true);
        adminRepository.resolveReportAndHideMoment(report.getReportId(), report.getMomentId(), currentUserId, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(ReportManagementActivity.this, "Đã xử lý báo cáo", Toast.LENGTH_SHORT).show();
                loadReports();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(ReportManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ignoreReport(ReportModel report) {
        setLoading(true);
        adminRepository.ignoreReport(report.getReportId(), currentUserId, new AdminRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(ReportManagementActivity.this, "Đã bỏ qua báo cáo", Toast.LENGTH_SHORT).show();
                loadReports();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(ReportManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        listReports.setEnabled(!isLoading);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
