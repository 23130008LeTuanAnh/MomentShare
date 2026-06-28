package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.activity.HistoryActivity;
import com.example.momentshare.model.Moment;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class HomeFeedActivity extends AppCompatActivity implements MomentAdapter.OnMomentClickListener {

    private RecyclerView rvHomeFeed;
    private MomentAdapter momentAdapter;
    private MomentRepository momentRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_feed);

        ImageButton btnOpenCamera = findViewById(R.id.btnOpenCamera);
        btnOpenCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Thực hiện chuyển hướng sang CameraActivity của module Người 3
                Intent intent = new Intent(HomeFeedActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        rvHomeFeed = findViewById(R.id.rvHomeFeed);
        ImageButton btnOpenHistory = findViewById(R.id.btnOpenHistory);
        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());

        rvHomeFeed.setLayoutManager(new LinearLayoutManager(this));
        rvHomeFeed.setHasFixedSize(true);

        // Khởi tạo Repository để giao tiếp với Firebase
        momentRepository = new MomentRepository();

        // Khởi tạo Adapter với danh sách rỗng ban đầu, giao diện sẽ chưa hiện gì cả
        momentAdapter = new MomentAdapter(this, new ArrayList<>(), this);
        rvHomeFeed.setAdapter(momentAdapter);

        // Gọi hàm kéo dữ liệu thật từ Firebase về
        loadRealData();

        btnOpenHistory.setOnClickListener(v ->
                startActivity(new Intent(HomeFeedActivity.this, HistoryActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Tự động tải lại dữ liệu mỗi khi quay lại màn hình Home
        loadRealData();
    }

    private void loadRealData() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) return;

        // 1. Lấy các bài viết được gửi riêng cho bạn (Logic cũ)
        momentRepository.getHomeFeed(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> privateMoments) {
                final List<Moment> allMoments = new ArrayList<>(privateMoments);

                // 2. Kéo thêm các bài viết được gắn nhãn công khai (isPublic == true) từ Firestore
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("moments")
                        .whereEqualTo("isPublic", true)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                Moment m = doc.toObject(Moment.class);
                                if (m != null && "active".equals(m.getStatus())) {
                                    if (m.getMomentId() == null || m.getMomentId().isEmpty()) {
                                        m.setMomentId(doc.getId());
                                    }

                                    // Kiểm tra trùng lặp phần tử trùng ID
                                    boolean isDuplicate = false;
                                    for (Moment existing : allMoments) {
                                        if (existing.getMomentId().equals(m.getMomentId())) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }
                                    if (!isDuplicate) {
                                        allMoments.add(m);
                                    }
                                }
                            }

                            // Sắp xếp lại toàn bộ danh sách gộp theo thứ tự thời gian mới nhất lên đầu
                            java.util.Collections.sort(allMoments, (m1, m2) -> {
                                if (m1.getCreatedAt() == null || m2.getCreatedAt() == null) return 0;
                                return m2.getCreatedAt().compareTo(m1.getCreatedAt());
                            });

                            // Cập nhật giao diện hiển thị lên RecyclerView
                            momentAdapter.updateData(allMoments);
                        })
                        .addOnFailureListener(e -> {
                            // Nếu lỗi khi tải bài công khai, vẫn hiển thị danh sách bài riêng tư
                            momentAdapter.updateData(allMoments);
                        });
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(HomeFeedActivity.this, "Lỗi tải dữ liệu: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMomentClick(Moment moment) {
        Toast.makeText(this, "Opening moment from: " + moment.getSenderId(), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(HomeFeedActivity.this, MomentDetailActivity.class);
        putMomentExtras(intent, moment);
        startActivity(intent);
    }

    public static void putMomentExtras(Intent intent, Moment moment) {
        intent.putExtra(MomentDetailActivity.EXTRA_MOMENT_ID, moment.getMomentId());
        intent.putExtra(MomentDetailActivity.EXTRA_SENDER_ID, moment.getSenderId());
        intent.putExtra(MomentDetailActivity.EXTRA_IMAGE_URL, moment.getImageUrl());
        intent.putExtra(MomentDetailActivity.EXTRA_CAPTION, moment.getCaption());
        if (moment.getCreatedAt() != null) {
            intent.putExtra(MomentDetailActivity.EXTRA_CREATED_AT, moment.getCreatedAt().toDate().getTime());
        }
    }

}
