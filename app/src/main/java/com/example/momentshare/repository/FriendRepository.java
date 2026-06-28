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

    public FriendRepository() {
        db = FirebaseFirestore.getInstance();
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
     * Tìm kiếm người dùng theo username hoặc email.
     */
    public void searchUsers(String query, UserListCallback callback) {
        String rawQuery = query == null ? "" : query.trim();
        // Người 1 thực hiện: chuẩn hóa từ khóa tìm kiếm để email viết hoa vẫn tìm đúng user.
        String normalizedUsername = ValidationUtils.normalizeUsername(rawQuery);
        String normalizedEmail = ValidationUtils.normalizeEmail(rawQuery);

        // Firestore không hỗ trợ tìm kiếm mờ tốt, ở đây tìm chính xác username hoặc email.
        // Người 1 thực hiện: username và email đều được chuẩn hóa chữ thường để tránh lệch khi người dùng nhập Email viết hoa.
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", normalizedUsername)
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

                    searchUsersByEmail(normalizedEmail, callback);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Người 1 thực hiện: tìm người dùng bằng email đã chuẩn hóa chữ thường.
     */
    private void searchUsersByEmail(String normalizedEmail, UserListCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("email", normalizedEmail)
                .get()
                .addOnSuccessListener(emailSnapshot -> {
                    List<User> users = new ArrayList<>();
                    for (DocumentSnapshot doc : emailSnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Gửi lời mời kết bạn.
     */
    public void sendFriendRequest(String senderId, String receiverId, ActionCallback callback) {
        String requestId = db.collection(Constants.COLLECTION_FRIEND_REQUESTS).document().getId();
        FriendRequest request = new FriendRequest(requestId, senderId, receiverId, "pending", Timestamp.now());

        db.collection(Constants.COLLECTION_FRIEND_REQUESTS)
                .document(requestId)
                .set(request)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
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
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
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
