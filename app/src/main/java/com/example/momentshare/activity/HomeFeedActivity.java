package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
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

    private void loadRealData() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để xem dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        momentRepository.getHomeFeed(currentUserId, new MomentRepository.MomentListCallback() {
            @Override
            public void onSuccess(List<Moment> moments) {
                momentAdapter.updateData(moments);

                if (moments.isEmpty()) {
                    Toast.makeText(HomeFeedActivity.this, "Chưa có khoảnh khắc nào từ bạn bè!", Toast.LENGTH_SHORT).show();
                }
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