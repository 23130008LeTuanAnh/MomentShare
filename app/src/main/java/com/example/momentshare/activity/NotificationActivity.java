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
import com.example.momentshare.model.Moment;
import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.repository.AuthManager;
import com.example.momentshare.repository.MomentRepository;
import com.example.momentshare.repository.NotificationRepository;
import com.example.momentshare.util.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationActivity hiển thị thông báo trong app.
 *
 * Đã chỉnh:
 * - Bấm thông báo friend_request mở đúng FriendRequestActivity.
 * - Bấm notification reaction/moment có targetId sẽ mở MomentDetailActivity.
 * - Vẫn điều hướng được kể cả notification đã đọc.
 */
public class NotificationActivity extends AppCompatActivity {

    private ListView listNotifications;
    private TextView txtEmptyNotifications;
    private ProgressBar progressBar;
    private Button btnMarkAllRead;

    private AuthManager authManager;
    private NotificationRepository notificationRepository;
    private MomentRepository momentRepository;

    private final List<NotificationModel> notifications = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        authManager = new AuthManager();
        notificationRepository = new NotificationRepository();
        momentRepository = new MomentRepository(this);

        initViews();
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

    private void handleNotificationClick(NotificationModel notification) {
        if (notification == null) return;

        if (!notification.isRead()) {
            notificationRepository.markAsRead(notification.getNotificationId(), new NotificationRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    notification.setRead(true);
                    renderNotifications();
                }

                @Override
                public void onFailure(String errorMessage) { }
            });
        }

        navigateByNotification(notification);
    }

    private void navigateByNotification(NotificationModel notification) {
        String type = safeText(notification.getType(), "");
        String targetId = safeText(notification.getTargetId(), "");

        if (Constants.NOTIFICATION_TYPE_FRIEND_REQUEST.equals(type)) {
            Intent intent = new Intent(this, FriendRequestActivity.class);
            if (!targetId.isEmpty()) {
                intent.putExtra(FriendRequestActivity.EXTRA_FILTER_SENDER_ID, targetId);
            }
            startActivity(intent);
            return;
        }

        if (Constants.NOTIFICATION_TYPE_MESSAGE.equals(type)) {
            Intent intent = new Intent(this, ChatActivity.class);
            if (!targetId.isEmpty()) {
                intent.putExtra(ChatActivity.EXTRA_FRIEND_ID, targetId);
            }
            startActivity(intent);
            return;
        }

        if ((Constants.NOTIFICATION_TYPE_REACTION.equals(type)
                || Constants.NOTIFICATION_TYPE_MOMENT.equals(type))
                && !targetId.isEmpty()) {
            openMomentDetail(targetId);
        }
    }

    private void openMomentDetail(String momentId) {
        setLoading(true);
        momentRepository.getMomentById(momentId, new MomentRepository.MomentCallback() {
            @Override
            public void onSuccess(Moment moment) {
                setLoading(false);
                if (moment == null) {
                    Toast.makeText(NotificationActivity.this, "Khoảnh khắc không còn tồn tại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent(NotificationActivity.this, MomentDetailActivity.class);
                HomeFeedActivity.putMomentExtras(intent, moment);
                startActivity(intent);
            }

            @Override
            public void onError(Exception exception) {
                setLoading(false);
                Toast.makeText(NotificationActivity.this, "Không mở được khoảnh khắc", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllAsRead() {
        if (currentUserId == null) return;

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
