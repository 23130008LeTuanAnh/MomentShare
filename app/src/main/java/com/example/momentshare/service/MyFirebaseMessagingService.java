package com.example.momentshare.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.momentshare.R;
import com.example.momentshare.activity.ProfileActivity;
import com.example.momentshare.helper.FcmTokenManager;
import com.example.momentshare.util.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * MyFirebaseMessagingService nhận push notification thật từ Firebase Cloud Messaging.
 * File này thuộc phần Người 5 - Notification.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    /**
     * Người 5 thực hiện: khi Firebase cấp token mới, lưu lại token vào hồ sơ user trên Firestore.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        new FcmTokenManager().saveTokenForCurrentUser(token);
    }

    /**
     * Người 5 thực hiện: nhận FCM data/notification message và hiển thị notification thật trên thiết bị.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        super.onMessageReceived(message);

        String title = "MomentShare";
        String body = "Bạn có thông báo mới.";

        if (message.getNotification() != null) {
            if (message.getNotification().getTitle() != null) {
                title = message.getNotification().getTitle();
            }
            if (message.getNotification().getBody() != null) {
                body = message.getNotification().getBody();
            }
        }

        Map<String, String> data = message.getData();
        if (data.containsKey("title")) {
            title = data.get("title");
        }
        if (data.containsKey("body")) {
            body = data.get("body");
        }

        showLocalNotification(title, body);
    }

    private void showLocalNotification(@NonNull String title, @NonNull String body) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager == null) {
            return;
        }

        createNotificationChannel(notificationManager);

        Intent intent = new Intent(this, ProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Constants.FCM_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(@NonNull NotificationManager notificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                Constants.FCM_CHANNEL_ID,
                Constants.FCM_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Thông báo MomentShare từ Firebase Cloud Messaging");
        notificationManager.createNotificationChannel(channel);
    }
}
