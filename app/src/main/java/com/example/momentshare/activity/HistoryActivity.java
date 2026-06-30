package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.model.Moment;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnItemClickListener {

    private final List<Moment> historyMoments = new ArrayList<>();
    private HistoryAdapter historyAdapter;
    private MomentRepository momentRepository;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setContentView(R.layout.activity_history);

        ImageButton btnBack = findViewById(R.id.btnBackHistory);
        btnBack.setOnClickListener(v -> finish());

        RecyclerView rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new GridLayoutManager(this, 3));
        historyAdapter = new HistoryAdapter(historyMoments, this);
        rvHistory.setAdapter(historyAdapter);

        momentRepository = new MomentRepository(this);

        loadHistoryMoments();
    }

    private void loadHistoryMoments() {
        momentRepository.getMomentsSentByUser(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> sentMoments) {
                historyMoments.clear();
                historyMoments.addAll(sentMoments);

                momentRepository.getMomentsReceivedByUser(currentUserId, new MomentRepository.MomentListCallback() {
                    @Override
                    public void onSuccess(List<Moment> receivedMoments) {
                        historyMoments.addAll(receivedMoments);
                        updateUI();
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(HistoryActivity.this, "Không thể tải ảnh đã nhận", Toast.LENGTH_SHORT).show();
                        // QUAN TRỌNG: Vẫn phải đẩy dữ liệu "ảnh đã gửi" lên màn hình kể cả khi ảnh nhận bị lỗi
                        updateUI();
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(HistoryActivity.this, "Lỗi tải lịch sử ảnh: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Tách riêng luồng xử lý giao diện ra để code an toàn và tránh nhân bản dữ liệu
    private void updateUI() {
        // 1. Loại bỏ các ảnh bị trùng lặp (nếu một ảnh vừa nằm trong danh sách gửi và nhận)
        Set<String> uniqueIds = new HashSet<>();
        List<Moment> uniqueMoments = new ArrayList<>();
        for (Moment m : historyMoments) {
            if (m.getMomentId() != null && !uniqueIds.contains(m.getMomentId())) {
                uniqueIds.add(m.getMomentId());
                uniqueMoments.add(m);
            }
        }
        historyMoments.clear();
        historyMoments.addAll(uniqueMoments);

        // 2. Sắp xếp lại theo thời gian mới nhất lên trên
        Collections.sort(historyMoments, (m1, m2) -> {
            if (m1.getCreatedAt() == null || m2.getCreatedAt() == null) return 0;
            return m2.getCreatedAt().compareTo(m1.getCreatedAt());
        });

        historyAdapter.notifyDataSetChanged();

        if (historyMoments.isEmpty()) {
            Toast.makeText(HistoryActivity.this, "Bạn chưa có khoảnh khắc nào!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(Moment moment) {
        Intent intent = new Intent(HistoryActivity.this, MomentDetailActivity.class);
        HomeFeedActivity.putMomentExtras(intent, moment);
        startActivity(intent);
    }
}
