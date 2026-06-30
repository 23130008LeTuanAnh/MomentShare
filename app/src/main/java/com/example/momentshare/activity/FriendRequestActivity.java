package com.example.momentshare.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.momentshare.R;
import com.example.momentshare.model.FriendRequest;
import com.example.momentshare.repository.FriendRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

/**
 * FriendRequestActivity hiển thị các lời mời kết bạn đang chờ.
 *
 * Đã chỉnh:
 * - Nhận EXTRA_TARGET_SENDER_ID từ nút Phản hồi trong màn hình Tìm kiếm bạn bè.
 * - Nếu có target sender thì ưu tiên hiển thị lời mời của đúng người đó.
 * - Reload lại danh sách trong onResume để khi quay lại màn hình luôn thấy lời mời mới nhất.
 */
public class FriendRequestActivity extends AppCompatActivity {

    public static final String EXTRA_TARGET_SENDER_ID = "extra_target_sender_id";

    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView txtNoRequest;
    private RecyclerView rvRequest;

    private FriendRequestAdapter adapter;
    private final List<FriendRequest> requestList = new ArrayList<>();
    private FriendRepository friendRepository;
    private String currentUserId;
    private String targetSenderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        if (currentUserId.isEmpty()) {
            finish();
            return;
        }

        targetSenderId = getIntent().getStringExtra(EXTRA_TARGET_SENDER_ID);

        friendRepository = new FriendRepository();
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRequests();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBackRequest);
        progressBar = findViewById(R.id.progressBarRequest);
        txtNoRequest = findViewById(R.id.txtNoRequest);
        rvRequest = findViewById(R.id.rvFriendRequest);

        rvRequest.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendRequestAdapter(this, requestList);
        rvRequest.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadRequests() {
        setLoading(true);

        friendRepository.getPendingRequests(currentUserId, new FriendRepository.RequestListCallback() {
            @Override
            public void onSuccess(List<FriendRequest> requests) {
                setLoading(false);

                List<FriendRequest> displayRequests = filterByTargetSenderIfNeeded(requests);

                requestList.clear();
                requestList.addAll(displayRequests);
                adapter.notifyDataSetChanged();

                if (displayRequests.isEmpty()) {
                    txtNoRequest.setText(isEmpty(targetSenderId)
                            ? "Không có lời mời nào"
                            : "Lời mời này không còn tồn tại hoặc đã được xử lý");
                    txtNoRequest.setVisibility(View.VISIBLE);
                } else {
                    txtNoRequest.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                txtNoRequest.setText("Không tải được lời mời kết bạn");
                txtNoRequest.setVisibility(View.VISIBLE);
                Toast.makeText(FriendRequestActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<FriendRequest> filterByTargetSenderIfNeeded(List<FriendRequest> requests) {
        if (isEmpty(targetSenderId)) {
            return requests;
        }

        List<FriendRequest> filtered = new ArrayList<>();
        for (FriendRequest request : requests) {
            if (request != null && targetSenderId.equals(request.getSenderId())) {
                filtered.add(request);
            }
        }
        return filtered;
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvRequest.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        txtNoRequest.setVisibility(View.GONE);
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
