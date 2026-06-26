package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.activity.HomeFeedActivity;
import com.example.momentshare.activity.MomentDetailActivity;
import com.example.momentshare.model.Moment;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

// import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnItemClickListener {

    private final List<Moment> historyMoments = new ArrayList<>();
    private HistoryAdapter historyAdapter;
    private MomentRepository momentRepository;

    private String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        ImageButton btnBack = findViewById(R.id.btnBackHistory);
        RecyclerView rvHistory = findViewById(R.id.rvHistory);

        btnBack.setOnClickListener(v -> finish());

        momentRepository = new MomentRepository();

        // Khởi tạo Adapter với danh sách rỗng ban đầu
        historyAdapter = new HistoryAdapter(historyMoments, this);
        rvHistory.setLayoutManager(new GridLayoutManager(this, 2));
        rvHistory.setHasFixedSize(true);
        rvHistory.setAdapter(historyAdapter);

        loadRealHistory();
    }

    private void loadRealHistory() {
        // Kéo danh sách ảnh ĐÃ GỬI
        momentRepository.getSentHistory(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> sentMoments) {
                // Khi lấy ảnh đã gửi thành công, kéo tiếp danh sách ảnh ĐÃ NHẬN
                momentRepository.getReceivedHistory(currentUserId, new MomentRepository.MomentListCallback() {
                    @Override
                    public void onSuccess(List<Moment> receivedMoments) {
                        // Xóa dữ liệu cũ và gộp cả 2 danh sách lại
                        historyMoments.clear();
                        historyMoments.addAll(sentMoments);
                        historyMoments.addAll(receivedMoments);

                        // Sắp xếp danh sách chung theo thời gian mới nhất lên đầu
                        Collections.sort(historyMoments, (m1, m2) -> {
                            if (m1.getCreatedAt() == null || m2.getCreatedAt() == null) return 0;
                            return m2.getCreatedAt().compareTo(m1.getCreatedAt()); // Giảm dần
                        });

                        historyAdapter.notifyDataSetChanged();

                        if (historyMoments.isEmpty()) {
                            Toast.makeText(HistoryActivity.this, "Bạn chưa có khoảnh khắc nào!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Exception exception) {
                        Toast.makeText(HistoryActivity.this, "Lỗi tải ảnh đã nhận: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(HistoryActivity.this, "Lỗi tải ảnh đã gửi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onItemClick(Moment moment) {
        Intent intent = new Intent(HistoryActivity.this, MomentDetailActivity.class);
        HomeFeedActivity.putMomentExtras(intent, moment);
        startActivity(intent);
    }
}