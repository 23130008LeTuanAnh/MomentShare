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
import com.example.momentshare.model.ReportModel;
import com.example.momentshare.repository.AdminRepository;
import com.example.momentshare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * ReportManagementActivity hiển thị và xử lý báo cáo nội dung.
 * Admin có thể ẩn khoảnh khắc hoặc bỏ qua report.
 */
public class ReportManagementActivity extends AppCompatActivity {

    private ListView listReports;
    private TextView txtEmptyReports;
    private ProgressBar progressBar;

    private AdminAccessHelper adminAccessHelper;
    private AdminRepository adminRepository;

    private final List<ReportModel> reports = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String currentAdminId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_management);

        adminAccessHelper = new AdminAccessHelper();
        adminRepository = new AdminRepository();

        initViews();
        verifyAdminAndLoadReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentAdminId != null) {
            loadReports();
        }
    }

    private void initViews() {
        listReports = findViewById(R.id.listReports);
        txtEmptyReports = findViewById(R.id.txtEmptyReports);
        progressBar = findViewById(R.id.progressBar);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listReports.setAdapter(adapter);
        listReports.setOnItemClickListener((parent, view, position, id) -> showReportActionDialog(reports.get(position)));
    }

    private void verifyAdminAndLoadReports() {
        setLoading(true);
        adminAccessHelper.requireActiveAdmin(this, (adminId, adminUser) -> {
            currentAdminId = adminId;
            loadReports();
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
            adapter.add(buildReportRowText(report));
        }

        adapter.notifyDataSetChanged();
        txtEmptyReports.setVisibility(reports.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String buildReportRowText(ReportModel report) {
        String status = safeText(report.getStatus(), Constants.REPORT_STATUS_PENDING);
        String reason = safeText(report.getReason(), "Không có lý do");
        String momentId = safeText(report.getMomentId(), "Không rõ moment");
        String reporterId = safeText(report.getReporterId(), "Không rõ người báo cáo");

        return "Trạng thái: " + status
                + "\nLý do: " + reason
                + "\nMoment: " + momentId
                + "\nNgười báo cáo: " + reporterId;
    }

    private void showReportActionDialog(ReportModel report) {
        if (report == null || report.getReportId() == null || report.getReportId().trim().isEmpty()) {
            Toast.makeText(this, "Không xác định được báo cáo", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Constants.REPORT_STATUS_PENDING.equals(report.getStatus())) {
            Toast.makeText(this, "Báo cáo này đã được xử lý", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] actions = {
                "Ẩn khoảnh khắc",
                "Bỏ qua báo cáo"
        };

        new AlertDialog.Builder(this)
                .setTitle("Xử lý báo cáo")
                .setMessage("Moment ID: " + safeText(report.getMomentId(), "Không rõ")
                        + "\nLý do: " + safeText(report.getReason(), "Không có"))
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        hideMomentAndResolveReport(report);
                    } else {
                        ignoreReport(report);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void hideMomentAndResolveReport(ReportModel report) {
        setLoading(true);
        adminRepository.resolveReportAndHideMoment(
                report.getReportId(),
                safeText(report.getMomentId(), ""),
                currentAdminId,
                new AdminRepository.ActionCallback() {
                    @Override
                    public void onSuccess() {
                        setLoading(false);
                        Toast.makeText(ReportManagementActivity.this, "Đã ẩn khoảnh khắc và xử lý báo cáo", Toast.LENGTH_SHORT).show();
                        loadReports();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoading(false);
                        Toast.makeText(ReportManagementActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void ignoreReport(ReportModel report) {
        setLoading(true);
        adminRepository.ignoreReport(report.getReportId(), currentAdminId, new AdminRepository.ActionCallback() {
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
