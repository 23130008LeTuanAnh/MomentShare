package com.example.momentshare.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.NotificationRepository;
import com.example.momentshare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationActivity hiển thị danh sách thông báo trong app cho người dùng hiện tại.
 *
 * Đã chỉnh:
 * - Bấm vào thông báo Lời mời kết bạn sẽ mở đúng FriendRequestActivity.
 * - Các thông báo khác chỉ đánh dấu đã đọc, không điều hướng sai sang màn hình ảnh.
 */
public class NotificationActivity extends AppCompatActivity {

    private ListView listNotifications;
    private TextView txtEmptyNotifications;
    private ProgressBar progressBar;
    private Button btnMarkAllRead;

    private AuthManager authManager;
    private NotificationRepository notificationRepository;

    private final List<NotificationModel> notifications = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        authManager = new AuthManager();
        notificationRepository = new NotificationRepository();

        initViews();
        loadNotifications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
    }

    private void initViews() {
        listNotifications = findViewById(R.id.listNotifications);
        txtEmptyNotifications = findViewById(R.id.txtEmptyNotifications);
        progressBar = findViewById(R.id.progressBar);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);

        findViewById(R.id.btnBackNotification).setOnClickListener(v -> finish());

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listNotifications.setAdapter(adapter);
        listNotifications.setOnItemClickListener((parent, view, position, id) -> handleNotificationClick(notifications.get(position)));
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
    }

    private void loadNotifications() {
        currentUserId = authManager.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            finish();
            return;
        }

        setLoading(true);
        notificationRepository.loadNotifications(currentUserId, new NotificationRepository.NotificationListCallback() {
            @Override
            public void onSuccess(List<NotificationModel> result) {
                setLoading(false);
                notifications.clear();
                notifications.addAll(result);
                renderNotifications();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(NotificationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderNotifications() {
        adapter.clear();

        for (NotificationModel notification : notifications) {
            String readPrefix = notification.isRead() ? "Đã đọc" : "Mới";
            adapter.add(readPrefix + " - " + safeText(notification.getTitle(), "Thông báo")
                    + "\n" + safeText(notification.getMessage(), "Không có nội dung"));
        }

        adapter.notifyDataSetChanged();
        txtEmptyNotifications.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Xử lý khi bấm vào từng thông báo.
     *
     * Lời mời kết bạn phải mở FriendRequestActivity để user chấp nhận/từ chối.
     * Reaction hiện chưa lưu momentId trong NotificationModel nên không mở detail để tránh mở sai ảnh.
     */
    private void handleNotificationClick(NotificationModel notification) {
        markOneAsRead(notification, () -> {
            if (Constants.NOTIFICATION_TYPE_FRIEND_REQUEST.equals(notification.getType())) {
                Intent intent = new Intent(NotificationActivity.this, FriendRequestActivity.class);
                startActivity(intent);
                return;
            }

            Toast.makeText(NotificationActivity.this, "Đã đọc thông báo", Toast.LENGTH_SHORT).show();
        });
    }

    private void markOneAsRead(NotificationModel notification, Runnable afterMarked) {
        if (notification == null || notification.getNotificationId() == null) {
            return;
        }

        if (notification.isRead()) {
            afterMarked.run();
            return;
        }

        setLoading(true);
        notificationRepository.markAsRead(notification.getNotificationId(), new NotificationRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                notification.setRead(true);
                renderNotifications();
                afterMarked.run();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(NotificationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllAsRead() {
        if (currentUserId == null) {
            return;
        }

        setLoading(true);
        notificationRepository.markAllAsRead(currentUserId, new NotificationRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(NotificationActivity.this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                loadNotifications();
            }

            @Override
            public void onFailure(String errorMessage) {
                setLoading(false);
                Toast.makeText(NotificationActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        listNotifications.setEnabled(!isLoading);
        btnMarkAllRead.setEnabled(!isLoading);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
