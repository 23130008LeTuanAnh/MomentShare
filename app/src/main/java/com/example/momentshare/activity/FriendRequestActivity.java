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
 * FriendRequestActivity hiển thị lời mời kết bạn đang chờ.
 *
 * Đã chỉnh:
 * - Tự reload trong onResume().
 * - Nhận EXTRA_FILTER_SENDER_ID từ nút Phản hồi/thông báo để lọc đúng người đã gửi lời mời.
 */
public class FriendRequestActivity extends AppCompatActivity {

    public static final String EXTRA_FILTER_SENDER_ID = "extra_filter_sender_id";

    private ImageButton btnBack;
    private ProgressBar progressBar;
    private TextView txtNoRequest;
    private RecyclerView rvRequest;

    private FriendRequestAdapter adapter;
    private final List<FriendRequest> requestList = new ArrayList<>();
    private FriendRepository friendRepository;
    private String currentUserId;
    private String filterSenderId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_request);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        filterSenderId = getIntent().getStringExtra(EXTRA_FILTER_SENDER_ID);
        if (filterSenderId == null) {
            filterSenderId = "";
        }

        if (currentUserId.isEmpty()) {
            finish();
            return;
        }

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
                requestList.clear();

                for (FriendRequest request : requests) {
                    if (filterSenderId.trim().isEmpty()
                            || filterSenderId.equals(request.getSenderId())) {
                        requestList.add(request);
                    }
                }

                adapter.notifyDataSetChanged();
                txtNoRequest.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                txtNoRequest.setVisibility(View.VISIBLE);
                Toast.makeText(FriendRequestActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        rvRequest.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        if (isLoading) {
            txtNoRequest.setVisibility(View.GONE);
        }
    }
}
