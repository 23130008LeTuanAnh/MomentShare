package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.FriendRequest;
import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * FriendRepository xử lý tìm kiếm bạn bè, danh sách bạn bè và lời mời kết bạn.
 *
 * Phần đã chỉnh cho đúng đề tài:
 * - Tìm kiếm user theo username, email hoặc fullName.
 * - Lọc tài khoản hiện tại, tài khoản bị khóa và tài khoản admin khỏi kết quả kết bạn.
 * - Tìm kiếm không phụ thuộc chữ hoa/thường và có hỗ trợ bỏ dấu tiếng Việt khi lọc client.
 * - Danh sách bạn bè chỉ hiển thị tài khoản còn active.
 * - Sắp xếp kết quả rõ ràng để dễ demo.
 */
public class FriendRepository {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

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

    /**
     * Tìm bạn bằng username, email hoặc fullName.
     *
     * Dùng cách get users rồi lọc ở client để tránh lỗi thiếu index Firestore khi demo.
     * Với đồ án/lớp học và dữ liệu ít, cách này ổn định hơn orderBy nhiều field.
     */
    public void searchUsers(String query, UserListCallback callback) {
        String rawQuery = query == null ? "" : query.trim();
        String normalizedQuery = normalizeSearchText(rawQuery);

        if (normalizedQuery.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        String currentUserId = getCurrentUserId();

        db.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, User> resultMap = new LinkedHashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user == null) continue;

                        if (isEmpty(user.getUserId())) {
                            user.setUserId(doc.getId());
                        }

                        if (!canShowInFriendSearch(user, currentUserId)) continue;
                        if (!matchesUserKeyword(user, normalizedQuery)) continue;

                        resultMap.put(user.getUserId(), user);
                    }

                    List<User> users = new ArrayList<>(resultMap.values());
                    sortUsersByKeyword(users, normalizedQuery);
                    callback.onSuccess(users);
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void sendFriendRequest(String senderId, String receiverId, ActionCallback callback) {
        if (isEmpty(senderId)) {
            callback.onFailure("Bạn cần đăng nhập để gửi lời mời kết bạn");
            return;
        }

        if (isEmpty(receiverId)) {
            callback.onFailure("Không xác định được tài khoản nhận lời mời");
            return;
        }

        if (senderId.equals(receiverId)) {
            callback.onFailure("Bạn không thể kết bạn với chính mình");
            return;
        }

        db.collection(Constants.COLLECTION_USERS).document(receiverId).get()
                .addOnSuccessListener(receiverDoc -> {
                    User receiver = receiverDoc.toObject(User.class);
                    if (receiver == null) {
                        callback.onFailure("Tài khoản này không tồn tại");
                        return;
                    }

                    if (isEmpty(receiver.getUserId())) {
                        receiver.setUserId(receiverDoc.getId());
                    }

                    if (!Constants.STATUS_ACTIVE.equalsIgnoreCase(safeString(receiver.getStatus()))) {
                        callback.onFailure("Tài khoản này đang bị khóa hoặc không khả dụng");
                        return;
                    }

                    if (Constants.ROLE_ADMIN.equalsIgnoreCase(safeString(receiver.getRole()))) {
                        callback.onFailure("Không thể gửi lời mời kết bạn đến tài khoản quản trị");
                        return;
                    }

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
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
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

                        if (isEmpty(request.getRequestId())) {
                            request.setRequestId(doc.getId());
                        }

                        String status = safeString(request.getStatus());
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
                        if (!isEmpty(friendUserId) && !friendIds.contains(friendUserId)) {
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
                                    if (user != null) {
                                        if (isEmpty(user.getUserId())) {
                                            user.setUserId(userDoc.getId());
                                        }

                                        if (Constants.STATUS_ACTIVE.equalsIgnoreCase(safeString(user.getStatus()))) {
                                            friendUsers.add(user);
                                        }
                                    }

                                    count[0]++;
                                    if (count[0] == friendIds.size()) {
                                        friendUsers.sort((left, right) -> buildSortableName(left).compareTo(buildSortableName(right)));
                                        callback.onSuccess(friendUsers);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    count[0]++;
                                    if (count[0] == friendIds.size()) {
                                        friendUsers.sort((left, right) -> buildSortableName(left).compareTo(buildSortableName(right)));
                                        callback.onSuccess(friendUsers);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void unfriend(String userId, String friendId, ActionCallback callback) {
        if (isEmpty(userId) || isEmpty(friendId)) {
            callback.onFailure("Không xác định được tài khoản cần hủy kết bạn");
            return;
        }

        WriteBatch batch = db.batch();
        batch.delete(db.collection(Constants.COLLECTION_FRIENDS).document(userId + "_" + friendId));
        batch.delete(db.collection(Constants.COLLECTION_FRIENDS).document(friendId + "_" + userId));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(buildErrorMessage(e)));
    }

    public void checkFriendshipStatus(String userId, String otherId, StatusCallback callback) {
        if (isEmpty(userId) || isEmpty(otherId)) {
            callback.onSuccess("none");
            return;
        }

        if (userId.equals(otherId)) {
            callback.onSuccess("self");
            return;
        }

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
                                    if ("pending".equalsIgnoreCase(safeString(status))) {
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
                                                if ("pending".equalsIgnoreCase(safeString(status))) {
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

    private boolean canShowInFriendSearch(User user, String currentUserId) {
        if (user == null || isEmpty(user.getUserId())) return false;
        if (!isEmpty(currentUserId) && currentUserId.equals(user.getUserId())) return false;
        if (!Constants.STATUS_ACTIVE.equalsIgnoreCase(safeString(user.getStatus()))) return false;
        return !Constants.ROLE_ADMIN.equalsIgnoreCase(safeString(user.getRole()));
    }

    private boolean matchesUserKeyword(User user, String normalizedQuery) {
        String username = normalizeSearchText(user.getUsername());
        String email = normalizeSearchText(user.getEmail());
        String fullName = normalizeSearchText(user.getFullName());

        return username.contains(normalizedQuery)
                || email.contains(normalizedQuery)
                || fullName.contains(normalizedQuery);
    }

    private void sortUsersByKeyword(List<User> users, String normalizedQuery) {
        users.sort((left, right) -> {
            int leftScore = buildMatchScore(left, normalizedQuery);
            int rightScore = buildMatchScore(right, normalizedQuery);
            if (leftScore != rightScore) return Integer.compare(leftScore, rightScore);
            return buildSortableName(left).compareTo(buildSortableName(right));
        });
    }

    private int buildMatchScore(User user, String normalizedQuery) {
        String username = normalizeSearchText(user.getUsername());
        String email = normalizeSearchText(user.getEmail());
        String fullName = normalizeSearchText(user.getFullName());

        if (username.equals(normalizedQuery)) return 0;
        if (email.equals(normalizedQuery)) return 1;
        if (username.startsWith(normalizedQuery)) return 2;
        if (email.startsWith(normalizedQuery)) return 3;
        if (fullName.startsWith(normalizedQuery)) return 4;
        if (fullName.contains(normalizedQuery)) return 5;
        return 6;
    }

    private String buildSortableName(User user) {
        if (user == null) return "";
        String fullName = normalizeSearchText(user.getFullName());
        if (!fullName.isEmpty()) return fullName;
        String username = normalizeSearchText(user.getUsername());
        if (!username.isEmpty()) return username;
        return normalizeSearchText(user.getEmail());
    }

    private String buildUserDisplayName(User user) {
        if (user == null) return "Người dùng MomentShare";

        String fullName = user.getFullName();
        String username = user.getUsername();

        if (!isEmpty(fullName) && !isEmpty(username)) {
            return fullName.trim() + " (@" + username.trim() + ")";
        }
        if (!isEmpty(fullName)) return fullName.trim();
        if (!isEmpty(username)) return "@" + username.trim();
        return "Người dùng MomentShare";
    }

    private String normalizeSearchText(String value) {
        if (value == null) return "";

        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        normalized = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        normalized = normalized.replace('đ', 'd').replace('Đ', 'd');
        return normalized;
    }

    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() == null
                ? ""
                : FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String buildErrorMessage(Exception e) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return "Có lỗi xảy ra, vui lòng thử lại";
        }
        return e.getMessage();
    }
}
