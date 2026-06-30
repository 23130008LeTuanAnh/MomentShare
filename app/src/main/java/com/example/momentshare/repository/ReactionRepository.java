package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.NotificationModel;
import com.example.momentshare.model.Reaction;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * ReactionRepository xử lý thả cảm xúc cho Moment.
 *
 * Đã chỉnh:
 * - Bỏ orderBy trong query để tránh lỗi FAILED_PRECONDITION do thiếu composite index.
 * - Sort bằng Java ở phía client.
 */
public class ReactionRepository {

    private static final String COLLECTION_REACTIONS = "reactions";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface ReactionListCallback {
        void onSuccess(List<Reaction> reactions);
        void onError(Exception e);
    }

    public interface SaveReactionCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public interface SaveUserReactionCallback {
        void onSuccess(String emoji);
        void onError(Exception e);
    }

    public interface ReactionCountCallback {
        void onSuccess(int count);
        void onError(Exception e);
    }

    public void getReactionsForMoment(@NonNull String momentId, @NonNull ReactionListCallback callback) {
        db.collection(COLLECTION_REACTIONS)
                .whereEqualTo("momentId", momentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Reaction> reactions = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Reaction reaction = doc.toObject(Reaction.class);
                        if (reaction != null) {
                            if (reaction.getReactionId() == null || reaction.getReactionId().trim().isEmpty()) {
                                reaction.setReactionId(doc.getId());
                            }
                            reactions.add(reaction);
                        }
                    }

                    reactions.sort((left, right) -> {
                        if (left.getCreatedAt() == null && right.getCreatedAt() == null) return 0;
                        if (left.getCreatedAt() == null) return 1;
                        if (right.getCreatedAt() == null) return -1;
                        return right.getCreatedAt().compareTo(left.getCreatedAt());
                    });

                    callback.onSuccess(reactions);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Lưu reaction của user cho một moment.
     *
     * Logic đúng theo đề tài:
     * - Một user chỉ có một reaction trên một ảnh.
     * - Nếu user đổi emoji thì cập nhật reaction cũ.
     * - Chỉ tạo notification khi reaction mới hoặc emoji thay đổi.
     * - Không gửi notification cho chính chủ ảnh nếu họ tự reaction.
     */
    public void saveReaction(String momentId, String userId, String emoji, SaveReactionCallback callback) {
        if (momentId == null || momentId.trim().isEmpty()
                || userId == null || userId.trim().isEmpty()
                || emoji == null || emoji.trim().isEmpty()) {
            callback.onError(new IllegalArgumentException("Thiếu dữ liệu reaction"));
            return;
        }

        String cleanMomentId = momentId.trim();
        String cleanUserId = userId.trim();
        String cleanEmoji = emoji.trim();
        String reactionId = cleanMomentId + "_" + cleanUserId;
        DocumentReference reactionRef = db.collection(COLLECTION_REACTIONS).document(reactionId);

        reactionRef.get()
                .addOnSuccessListener(document -> {
                    String oldEmoji = "";
                    if (document.exists()) {
                        Reaction oldReaction = document.toObject(Reaction.class);
                        if (oldReaction != null && oldReaction.getEmoji() != null) {
                            oldEmoji = oldReaction.getEmoji().trim();
                        }
                    }

                    boolean shouldNotifyOwner = oldEmoji.isEmpty() || !oldEmoji.equals(cleanEmoji);

                    Reaction reaction = new Reaction(
                            reactionId,
                            cleanMomentId,
                            cleanUserId,
                            cleanEmoji,
                            Timestamp.now()
                    );

                    reactionRef.set(reaction)
                            .addOnSuccessListener(unused -> {
                                if (shouldNotifyOwner) {
                                    createReactionNotification(cleanMomentId, cleanUserId, cleanEmoji, callback);
                                } else {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(callback::onError);
                })
                .addOnFailureListener(callback::onError);
    }

    /**
     * Tạo notification cho chủ khoảnh khắc khi có người thả/đổi reaction.
     * Reaction đã lưu thành công thì lỗi tạo notification không làm hủy thao tác reaction.
     */
    private void createReactionNotification(@NonNull String momentId,
                                            @NonNull String reactorId,
                                            @NonNull String emoji,
                                            @NonNull SaveReactionCallback callback) {
        db.collection(Constants.COLLECTION_MOMENTS)
                .document(momentId)
                .get()
                .addOnSuccessListener(momentDocument -> {
                    if (!momentDocument.exists()) {
                        callback.onSuccess();
                        return;
                    }

                    String ownerId = momentDocument.getString("senderId");
                    if (ownerId == null || ownerId.trim().isEmpty() || ownerId.equals(reactorId)) {
                        callback.onSuccess();
                        return;
                    }

                    DocumentReference notificationRef = db.collection(Constants.COLLECTION_NOTIFICATIONS).document();

                    NotificationModel notification = new NotificationModel();
                    notification.setNotificationId(notificationRef.getId());
                    notification.setUserId(ownerId);
                    notification.setType(Constants.NOTIFICATION_TYPE_REACTION);
                    notification.setTitle("Reaction mới");
                    notification.setMessage("Có người đã thả " + emoji + " vào khoảnh khắc của bạn.");
                    notification.setTargetId(momentId);
                    notification.setRead(false);
                    notification.setCreatedAt(Timestamp.now());

                    notificationRef.set(notification)
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onSuccess());
                })
                .addOnFailureListener(e -> callback.onSuccess());
    }

    public void getUserReaction(String momentId, String userId, SaveUserReactionCallback callback) {
        if (momentId == null || momentId.trim().isEmpty()
                || userId == null || userId.trim().isEmpty()) {
            callback.onSuccess("");
            return;
        }

        String reactionId = momentId + "_" + userId;

        db.collection(COLLECTION_REACTIONS)
                .document(reactionId)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) {
                        callback.onSuccess("");
                        return;
                    }

                    Reaction reaction = document.toObject(Reaction.class);
                    callback.onSuccess(reaction == null ? "" : reaction.getEmoji());
                })
                .addOnFailureListener(callback::onError);
    }

    public void countReactionsForMoment(@NonNull String momentId, @NonNull ReactionCountCallback callback) {
        db.collection(COLLECTION_REACTIONS)
                .whereEqualTo("momentId", momentId)
                .get()
                .addOnSuccessListener(snapshot -> callback.onSuccess(snapshot.size()))
                .addOnFailureListener(callback::onError);
    }
}
