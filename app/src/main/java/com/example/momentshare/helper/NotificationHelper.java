package com.example.momentshare.helper;

import androidx.annotation.NonNull;

import com.example.momentshare.repository.NotificationRepository;
import com.example.momentshare.util.Constants;

/**
 * NotificationHelper tạo nội dung thông báo theo từng sự kiện nghiệp vụ.
 * File này thuộc phần Người 5 - Notification.
 * Bản hiện tại lưu thông báo trong app; có thể mở rộng sang Firebase Cloud Messaging sau.
 */
public class NotificationHelper {

    private final NotificationRepository notificationRepository;

    public NotificationHelper() {
        notificationRepository = new NotificationRepository();
    }

    public void notifyNewMoment(@NonNull String receiverId,
                                @NonNull NotificationRepository.ActionCallback callback) {
        notificationRepository.createNotification(
                receiverId,
                Constants.NOTIFICATION_TYPE_MOMENT,
                "Khoảnh khắc mới",
                "Bạn vừa nhận được một khoảnh khắc mới.",
                callback
        );
    }

    public void notifyFriendRequest(@NonNull String receiverId,
                                    @NonNull NotificationRepository.ActionCallback callback) {
        notificationRepository.createNotification(
                receiverId,
                Constants.NOTIFICATION_TYPE_FRIEND_REQUEST,
                "Lời mời kết bạn",
                "Bạn có một lời mời kết bạn mới.",
                callback
        );
    }

    public void notifyReaction(@NonNull String ownerId,
                               @NonNull NotificationRepository.ActionCallback callback) {
        notificationRepository.createNotification(
                ownerId,
                Constants.NOTIFICATION_TYPE_REACTION,
                "Reaction mới",
                "Một người bạn vừa thả cảm xúc vào khoảnh khắc của bạn.",
                callback
        );
    }

    public void notifyReportHandled(@NonNull String reporterId,
                                    @NonNull NotificationRepository.ActionCallback callback) {
        notificationRepository.createNotification(
                reporterId,
                Constants.NOTIFICATION_TYPE_REPORT,
                "Báo cáo đã được xử lý",
                "Admin đã xem xét báo cáo nội dung của bạn.",
                callback
        );
    }
}
