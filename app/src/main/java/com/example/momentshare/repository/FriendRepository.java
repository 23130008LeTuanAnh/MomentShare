package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.FriendRequest;
import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.ValidationUtils;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        void onSuccess(String status); // "none", "pending", "accepted", "received_pending"
        void onFailure(String errorMessage);
    }

    /**
     * Tìm kiếm người dùng theo username hoặc email (tìm gần đúng từ đầu).
     */
    public void searchUsers(String query, UserListCallback callback) {
        String rawQuery = query == null ? "" : query.trim().toLowerCase();
        if (rawQuery.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        // Tìm gần đúng theo username (bắt đầu bằng query)
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
                        if (user != null) {
                            users.add(user);
                        }
                    }

                    if (!users.isEmpty()) {
                        callback.onSuccess(users);
                        return;
                    }

                    // Nếu không thấy username, tìm theo email (bắt đầu bằng query)
                    db.collection(Constants.COLLECTION_USERS)
                            .orderBy("email")
                            .startAt(rawQuery)
                            .endAt(rawQuery + "\uf8ff")
                            .limit(20)
                            .get()
                            .addOnSuccessListener(emailSnapshot -> {
                                for (DocumentSnapshot doc : emailSnapshot.getDocuments()) {
                                    User user = doc.toObject(User.class);
                                    if (user != null) {
                                        users.add(user);
                                    }
                                }
                                callback.onSuccess(users);
                            })
                            .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Gửi lời mời kết bạn (tự kiểm tra trùng).
     */
    public void sendFriendRequest(String senderId, String receiverId, ActionCallback callback) {
        // 1. Kiểm tra xem đã có lời mời hoặc đã là bạn chưa
        checkFriendshipStatus(senderId, receiverId, new StatusCallback() {
            @Override
            public void onSuccess(String status) {
                if (!"none".equals(status)) {
                    callback.onFailure("Yêu cầu đã tồn tại hoặc đã là bạn bè");
                    return;
                }

                // 2. Tạo yêu cầu mới
                String requestId = db.collection(Constants.COLLECTION_FRIEND_REQUESTS).document().getId();
                FriendRequest request = new FriendRequest(requestId, senderId, receiverId, "pending", Timestamp.now());

                db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                        .document(requestId)
                        .set(request)
                        .addOnSuccessListener(aVoid -> {
                            // 3. Tạo thông báo cho người nhận
                            sendRequestNotification(senderId, receiverId);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
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
                notificationRepository.createNotification(
                        receiverId,
                        Constants.NOTIFICATION_TYPE_FRIEND_REQUEST,
                        "Lời mời kết bạn",
                        sender.getFullName() + " (@" + sender.getUsername() + ") đã gửi lời mời kết bạn cho bạn.",
                        new NotificationRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {}
                            @Override
                            public void onFailure(@NonNull String errorMessage) {}
                        }
                );
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {}
        });
    }

    /**
     * Xem các lời mời kết bạn đang chờ (được gửi tới mình).
     */
    public void getPendingRequests(String userId, RequestListCallback callback) {
        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("status", "pending")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<FriendRequest> requests = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        requests.add(doc.toObject(FriendRequest.class));
                    }
                    callback.onSuccess(requests);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Chấp nhận lời mời kết bạn.
     * Tạo quan hệ bạn bè 2 chiều trong collection 'friends'.
     */
    public void acceptFriendRequest(String requestId, String senderId, String receiverId, ActionCallback callback) {
        WriteBatch batch = db.batch();

        // 1. Cập nhật trạng thái lời mời
        batch.update(db.collection(Constants.COLLECTION_FRIEND_REQUESTS).document(requestId), "status", "accepted");

        // 2. Thêm vào danh sách bạn bè của người gửi
        Map<String, Object> friend1 = new HashMap<>();
        friend1.put("userId", senderId);
        friend1.put("friendUserId", receiverId);
        friend1.put("createdAt", Timestamp.now());
        batch.set(db.collection(Constants.COLLECTION_FRIENDS).document(senderId + "_" + receiverId), friend1);

        // 3. Thêm vào danh sách bạn bè của người nhận
        Map<String, Object> friend2 = new HashMap<>();
        friend2.put("userId", receiverId);
        friend2.put("friendUserId", senderId);
        friend2.put("createdAt", Timestamp.now());
        batch.set(db.collection(Constants.COLLECTION_FRIENDS).document(receiverId + "_" + senderId), friend2);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    // Tạo thông báo cho người gửi lời mời
                    sendAcceptNotification(senderId, receiverId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    private void sendAcceptNotification(String senderId, String receiverId) {
        userRepository.getUserById(receiverId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User receiver) {
                notificationRepository.createNotification(
                        senderId,
                        Constants.NOTIFICATION_TYPE_FRIEND_REQUEST,
                        "Chấp nhận kết bạn",
                        receiver.getFullName() + " (@" + receiver.getUsername() + ") đã chấp nhận lời mời kết bạn của bạn.",
                        new NotificationRepository.ActionCallback() {
                            @Override
                            public void onSuccess() {}
                            @Override
                            public void onFailure(@NonNull String errorMessage) {}
                        }
                );
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {}
        });
    }

    /**
     * Từ chối lời mời kết bạn.
     */
    public void rejectFriendRequest(String requestId, ActionCallback callback) {
        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                .document(requestId)
                .update("status", "rejected")
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Lấy danh sách bạn bè.
     */
    public void getFriendList(String userId, UserListCallback callback) {
        db.collection(Constants.COLLECTION_FRIENDS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> friendIds = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        friendIds.add(doc.getString("friendUserId"));
                    }

                    if (friendIds.isEmpty()) {
                        callback.onSuccess(new ArrayList<>());
                        return;
                    }

                    // Tải thông tin chi tiết từng người bạn
                    List<User> friendUsers = new ArrayList<>();
                    final int[] count = {0};
                    for (String friendId : friendIds) {
                        db.collection(Constants.COLLECTION_USERS).document(friendId).get()
                                .addOnSuccessListener(userDoc -> {
                                    User u = userDoc.toObject(User.class);
                                    if (u != null) friendUsers.add(u);
                                    count[0]++;
                                    if (count[0] == friendIds.size()) {
                                        callback.onSuccess(friendUsers);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    count[0]++;
                                    if (count[0] == friendIds.size()) {
                                        callback.onSuccess(friendUsers);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Hủy kết bạn.
     */
    public void unfriend(String userId, String friendId, ActionCallback callback) {
        WriteBatch batch = db.batch();
        batch.delete(db.collection(Constants.COLLECTION_FRIENDS).document(userId + "_" + friendId));
        batch.delete(db.collection(Constants.COLLECTION_FRIENDS).document(friendId + "_" + userId));
        
        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Kiểm tra quan hệ hiện tại giữa 2 người dùng.
     */
    public void checkFriendshipStatus(String userId, String otherId, StatusCallback callback) {
        // 1. Kiểm tra đã là bạn chưa
        db.collection(Constants.COLLECTION_FRIENDS)
                .document(userId + "_" + otherId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        callback.onSuccess("accepted");
                    } else {
                        // 2. Kiểm tra lời mời gửi đi
                        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                                .whereEqualTo("senderId", userId)
                                .whereEqualTo("receiverId", otherId)
                                .whereEqualTo("status", "pending")
                                .get()
                                .addOnSuccessListener(q1 -> {
                                    if (!q1.isEmpty()) {
                                        callback.onSuccess("pending");
                                    } else {
                                        // 3. Kiểm tra lời mời nhận về
                                        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                                                .whereEqualTo("senderId", otherId)
                                                .whereEqualTo("receiverId", userId)
                                                .whereEqualTo("status", "pending")
                                                .get()
                                                .addOnSuccessListener(q2 -> {
                                                    if (!q2.isEmpty()) {
                                                        callback.onSuccess("received_pending");
                                                    } else {
                                                        callback.onSuccess("none");
                                                    }
                                                })
                                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                                    }
                                })
                                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
