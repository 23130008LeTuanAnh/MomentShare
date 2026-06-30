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
import com.example.momentshare.model.Moment;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFeedActivity hiển thị các khoảnh khắc trên trang Home.
 *
 * Đã chỉnh:
 * - Xóa Toast debug "Opening moment from..." khi mở detail.
 */
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
                Intent intent = new Intent(HomeFeedActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        rvHomeFeed = findViewById(R.id.rvHomeFeed);
        ImageButton btnOpenHistory = findViewById(R.id.btnOpenHistory);
        findViewById(R.id.btnBackHome).setOnClickListener(v -> finish());

        rvHomeFeed.setLayoutManager(new LinearLayoutManager(this));
        rvHomeFeed.setHasFixedSize(true);

        momentRepository = new MomentRepository(this);
        momentAdapter = new MomentAdapter(this, new ArrayList<>(), this);
        rvHomeFeed.setAdapter(momentAdapter);

        loadRealData();

        btnOpenHistory.setOnClickListener(v ->
                startActivity(new Intent(HomeFeedActivity.this, HistoryActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRealData();
    }

    private void loadRealData() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) return;

        momentRepository.getHomeFeed(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> privateMoments) {
                final List<Moment> allMoments = new ArrayList<>(privateMoments);

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("moments")
                        .whereEqualTo("isPublic", true)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                Moment moment = doc.toObject(Moment.class);
                                if (moment != null && "active".equals(moment.getStatus())) {
                                    if (moment.getMomentId() == null || moment.getMomentId().isEmpty()) {
                                        moment.setMomentId(doc.getId());
                                    }

                                    boolean isDuplicate = false;
                                    for (Moment existing : allMoments) {
                                        if (existing.getMomentId() != null && existing.getMomentId().equals(moment.getMomentId())) {
                                            isDuplicate = true;
                                            break;
                                        }
                                    }

                                    if (!isDuplicate) {
                                        allMoments.add(moment);
                                    }
                                }
                            }

                            java.util.Collections.sort(allMoments, (m1, m2) -> {
                                if (m1.getCreatedAt() == null && m2.getCreatedAt() == null) return 0;
                                if (m1.getCreatedAt() == null) return 1;
                                if (m2.getCreatedAt() == null) return -1;
                                return m2.getCreatedAt().compareTo(m1.getCreatedAt());
                            });

                            momentAdapter.updateData(allMoments);
                        })
                        .addOnFailureListener(e -> momentAdapter.updateData(allMoments));
            }

            @Override
            public void onError(Exception exception) {
                Toast.makeText(HomeFeedActivity.this, "Lỗi tải dữ liệu: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMomentClick(Moment moment) {
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
