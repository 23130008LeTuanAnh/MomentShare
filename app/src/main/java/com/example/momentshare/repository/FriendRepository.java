package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.FriendRequest;
import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FriendRepository xử lý bạn bè và lời mời kết bạn.
 *
 * Đã chỉnh:
 * - Lấy lời mời pending theo receiverId rồi lọc status bằng Java để tránh lỗi index.
 * - Thêm countPendingRequests() để Profile hiện số lời mời.
 * - Notification lời mời kết bạn lưu targetId = senderId để bấm thông báo/phản hồi mở đúng màn hình lời mời.
 */
public class FriendRepository {

    private final FirebaseFirestore db;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public FriendRepository() {
        db = FirebaseFirestore.getInstance();
        notificationRepository = new NotificationRepository();
        userRepository = new UserRepository();
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface UserListCallback {
        void onSuccess(List<User> users);
        void onFailure(String errorMessage);
    }

    public interface RequestListCallback {
        void onSuccess(List<FriendRequest> requests);
        void onFailure(String errorMessage);
    }

    public interface StatusCallback {
        void onSuccess(String status); // none, pending, accepted, received_pending
        void onFailure(String errorMessage);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onFailure(String errorMessage);
    }

    public void searchUsers(String query, UserListCallback callback) {
        String rawQuery = query == null ? "" : query.trim().toLowerCase();
        if (rawQuery.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        db.collection(Constants.COLLECTION_USERS)
                .orderBy("username")
                .startAt(rawQuery)
                .endAt(rawQuery + "\uf8ff")
                .limit(20)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) users.add(user);
                    }

                    if (!users.isEmpty()) {
                        callback.onSuccess(users);
                        return;
                    }

                    db.collection(Constants.COLLECTION_USERS)
                            .orderBy("email")
                            .startAt(rawQuery)
                            .endAt(rawQuery + "\uf8ff")
                            .limit(20)
                            .get()
                            .addOnSuccessListener(emailSnapshot -> {
                                for (DocumentSnapshot doc : emailSnapshot.getDocuments()) {
                                    User user = doc.toObject(User.class);
                                    if (user != null) users.add(user);
                                }
                                callback.onSuccess(users);
                            })
                            .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void sendFriendRequest(String senderId, String receiverId, ActionCallback callback) {
        checkFriendshipStatus(senderId, receiverId, new StatusCallback() {
            @Override
            public void onSuccess(String status) {
                if ("accepted".equals(status)) {
                    callback.onFailure("Hai người đã là bạn bè");
                    return;
                }

                if ("pending".equals(status)) {
                    callback.onFailure("Bạn đã gửi lời mời kết bạn trước đó");
                    return;
                }

                if ("received_pending".equals(status)) {
                    callback.onFailure("Người này đã gửi lời mời cho bạn. Hãy vào mục Lời mời kết bạn để phản hồi.");
                    return;
                }

                String requestId = db.collection(Constants.COLLECTION_FRIEND_REQUESTS).document().getId();
                FriendRequest request = new FriendRequest(requestId, senderId, receiverId, "pending", Timestamp.now());

                db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                        .document(requestId)
                        .set(request)
                        .addOnSuccessListener(aVoid -> {
                            sendRequestNotification(senderId, receiverId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    private void sendRequestNotification(String senderId, String receiverId) {
        userRepository.getUserById(senderId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User sender) {
                String senderName = buildUserDisplayName(sender);
                notificationRepository.createNotification(
                        receiverId,
                        Constants.NOTIFICATION_TYPE_FRIEND_REQUEST,
                        "Lời mời kết bạn",
                        senderName + " đã gửi lời mời kết bạn cho bạn.",
                        senderId,
                        new NotificationRepository.ActionCallback() {
                            @Override
                            public void onSuccess() { }

                            @Override
                            public void onFailure(@NonNull String errorMessage) { }
                        }
                );
            }

            @Override
            public void onFailure(String errorMessage) { }
        });
    }

    public void getPendingRequests(String userId, RequestListCallback callback) {
        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("receiverId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        FriendRequest request = doc.toObject(FriendRequest.class);
                        if (request == null) continue;

                        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
                            request.setRequestId(doc.getId());
                        }

                        String status = request.getStatus() == null ? "" : request.getStatus().trim();
                        if ("pending".equalsIgnoreCase(status)) {
                            requests.add(request);
                        }
                    }

                    requests.sort((left, right) -> {
                        if (left.getCreatedAt() == null && right.getCreatedAt() == null) return 0;
                        if (left.getCreatedAt() == null) return 1;
                        if (right.getCreatedAt() == null) return -1;
                        return right.getCreatedAt().compareTo(left.getCreatedAt());
                    });

                    callback.onSuccess(requests);
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void countPendingRequests(String userId, CountCallback callback) {
        getPendingRequests(userId, new RequestListCallback() {
            @Override
            public void onSuccess(List<FriendRequest> requests) {
                callback.onSuccess(requests == null ? 0 : requests.size());
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    public void acceptFriendRequest(String requestId, String senderId, String receiverId, ActionCallback callback) {
        WriteBatch batch = db.batch();
        batch.update(db.collection(Constants.COLLECTION_FRIEND_REQUESTS).document(requestId), "status", "accepted");

        Map<String, Object> friend1 = new HashMap<>();
        friend1.put("userId", senderId);
        friend1.put("friendUserId", receiverId);
        friend1.put("createdAt", Timestamp.now());
        batch.set(db.collection(Constants.COLLECTION_FRIENDS).document(senderId + "_" + receiverId), friend1);

        Map<String, Object> friend2 = new HashMap<>();
        friend2.put("userId", receiverId);
        friend2.put("friendUserId", senderId);
        friend2.put("createdAt", Timestamp.now());
        batch.set(db.collection(Constants.COLLECTION_FRIENDS).document(receiverId + "_" + senderId), friend2);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    sendAcceptNotification(senderId, receiverId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    private void sendAcceptNotification(String senderId, String receiverId) {
        userRepository.getUserById(receiverId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User receiver) {
                String receiverName = buildUserDisplayName(receiver);
                notificationRepository.createNotification(
                        senderId,
                        Constants.NOTIFICATION_TYPE_FRIEND_REQUEST,
                        "Đã chấp nhận kết bạn",
                        receiverName + " đã chấp nhận lời mời kết bạn của bạn.",
                        receiverId,
                        new NotificationRepository.ActionCallback() {
                            @Override
                            public void onSuccess() { }

                            @Override
                            public void onFailure(@NonNull String errorMessage) { }
                        }
                );
            }

            @Override
            public void onFailure(String errorMessage) { }
        });
    }

    public void rejectFriendRequest(String requestId, ActionCallback callback) {
        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                .document(requestId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void getFriendList(String userId, UserListCallback callback) {
        db.collection(Constants.COLLECTION_FRIENDS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> friendIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String friendUserId = doc.getString("friendUserId");
                        if (friendUserId != null && !friendUserId.trim().isEmpty()) {
                            friendIds.add(friendUserId);
                        }
                    }

                    if (friendIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    List<User> friendUsers = new ArrayList<>();
                    final int[] count = {0};
                    for (String friendId : friendIds) {
                        db.collection(Constants.COLLECTION_USERS).document(friendId).get()
                                .addOnSuccessListener(userDoc -> {
                                    User user = userDoc.toObject(User.class);
                                    if (user != null) friendUsers.add(user);
                                    count[0]++;
                                    if (count[0] == friendIds.size()) callback.onSuccess(friendUsers);
                                })
                                .addOnFailureListener(e -> {
                                    count[0]++;
                                    if (count[0] == friendIds.size()) callback.onSuccess(friendUsers);
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void unfriend(String userId, String friendId, ActionCallback callback) {
        WriteBatch batch = db.batch();
        batch.delete(db.collection(Constants.COLLECTION_FRIENDS).document(userId + "_" + friendId));
        batch.delete(db.collection(Constants.COLLECTION_FRIENDS).document(friendId + "_" + userId));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void checkFriendshipStatus(String userId, String otherId, StatusCallback callback) {
        db.collection(Constants.COLLECTION_FRIENDS)
                .document(userId + "_" + otherId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess("accepted");
                        return;
                    }

                    db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                            .whereEqualTo("senderId", userId)
                            .whereEqualTo("receiverId", otherId)
                            .get()
                            .addOnSuccessListener(q1 -> {
                                boolean hasPendingSent = false;
                                for (DocumentSnapshot requestDoc : q1.getDocuments()) {
                                    String status = requestDoc.getString("status");
                                    if ("pending".equalsIgnoreCase(status)) {
                                        hasPendingSent = true;
                                        break;
                                    }
                                }

                                if (hasPendingSent) {
                                    callback.onSuccess("pending");
                                    return;
                                }

                                db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                                        .whereEqualTo("senderId", otherId)
                                        .whereEqualTo("receiverId", userId)
                                        .get()
                                        .addOnSuccessListener(q2 -> {
                                            boolean hasPendingReceived = false;
                                            for (DocumentSnapshot requestDoc : q2.getDocuments()) {
                                                String status = requestDoc.getString("status");
                                                if ("pending".equalsIgnoreCase(status)) {
                                                    hasPendingReceived = true;
                                                    break;
                                                }
                                            }
                                            callback.onSuccess(hasPendingReceived ? "received_pending" : "none");
                                        })
                                        .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
                            })
                            .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    private String buildUserDisplayName(User user) {
        if (user == null) return "Người dùng MomentShare";

        String fullName = user.getFullName();
        String username = user.getUsername();

        if (fullName != null && !fullName.trim().isEmpty()
                && username != null && !username.trim().isEmpty()) {
            return fullName + " (@" + username + ")";
        }
        if (fullName != null && !fullName.trim().isEmpty()) return fullName;
        if (username != null && !username.trim().isEmpty()) return "@" + username;
        return "Người dùng MomentShare";
    }

    private String buildErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "Có lỗi xảy ra, vui lòng thử lại";
        }
        return e.getMessage();
    }
}
