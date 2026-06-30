package com.example.momentshare.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.momentshare.model.ChatMessage;
import com.example.momentshare.model.ChatRoom;
import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ChatRepository xử lý nhắn tin 1-1 giữa hai tài khoản đã là bạn bè.
 *
 * Cấu trúc Firestore:
 * chat_rooms/{roomId}
 * chat_rooms/{roomId}/messages/{messageId}
 */
public class ChatRepository {

    private final FirebaseFirestore db;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    public ChatRepository() {
        db = FirebaseFirestore.getInstance();
        userRepository = new UserRepository();
        notificationRepository = new NotificationRepository();
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface ChatRoomCallback {
        void onSuccess(ChatRoom room);
        void onFailure(String errorMessage);
    }

    public interface ChatRoomListCallback {
        void onSuccess(List<ChatRoom> rooms);
        void onFailure(String errorMessage);
    }

    public interface MessageListCallback {
        void onSuccess(List<ChatMessage> messages);
        void onFailure(String errorMessage);
    }

    public void openOrCreateRoom(@NonNull String currentUserId,
                                 @NonNull String friendId,
                                 @NonNull ChatRoomCallback callback) {
        if (!isValidPair(currentUserId, friendId)) {
            callback.onFailure("Không xác định được bạn bè để nhắn tin");
            return;
        }

        ensureFriendship(currentUserId, friendId, new ActionCallback() {
            @Override
            public void onSuccess() {
                String roomId = buildRoomId(currentUserId, friendId);
                DocumentReference roomRef = db.collection(Constants.COLLECTION_CHAT_ROOMS).document(roomId);

                roomRef.get()
                        .addOnSuccessListener(snapshot -> {
                            if (snapshot.exists()) {
                                ChatRoom room = snapshot.toObject(ChatRoom.class);
                                if (room == null) room = buildEmptyRoom(roomId, currentUserId, friendId);
                                if (room.getRoomId() == null || room.getRoomId().trim().isEmpty()) {
                                    room.setRoomId(snapshot.getId());
                                }
                                callback.onSuccess(room);
                                return;
                            }

                            ChatRoom newRoom = buildEmptyRoom(roomId, currentUserId, friendId);
                            roomRef.set(newRoom)
                                    .addOnSuccessListener(unused -> callback.onSuccess(newRoom))
                                    .addOnFailureListener(e -> callback.onFailure("Không tạo được phòng chat: " + e.getMessage()));
                        })
                        .addOnFailureListener(e -> callback.onFailure("Không mở được phòng chat: " + e.getMessage()));
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    public void sendTextMessage(@NonNull String currentUserId,
                                @NonNull String friendId,
                                @Nullable String rawText,
                                @NonNull ActionCallback callback) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            callback.onFailure("Vui lòng nhập nội dung tin nhắn");
            return;
        }

        if (text.length() > 1000) {
            callback.onFailure("Tin nhắn tối đa 1000 ký tự");
            return;
        }

        if (!isValidPair(currentUserId, friendId)) {
            callback.onFailure("Không xác định được người nhận tin nhắn");
            return;
        }

        ensureFriendship(currentUserId, friendId, new ActionCallback() {
            @Override
            public void onSuccess() {
                String roomId = buildRoomId(currentUserId, friendId);
                Timestamp now = Timestamp.now();

                DocumentReference roomRef = db.collection(Constants.COLLECTION_CHAT_ROOMS).document(roomId);
                DocumentReference messageRef = roomRef.collection(Constants.COLLECTION_MESSAGES).document();

                ChatMessage message = new ChatMessage(
                        messageRef.getId(),
                        roomId,
                        currentUserId,
                        friendId,
                        text,
                        false,
                        now
                );

                Map<String, Object> roomUpdates = new HashMap<>();
                roomUpdates.put("roomId", roomId);
                roomUpdates.put("memberIds", buildMemberIds(currentUserId, friendId));
                roomUpdates.put("userAId", getUserAId(currentUserId, friendId));
                roomUpdates.put("userBId", getUserBId(currentUserId, friendId));
                roomUpdates.put("lastMessage", text);
                roomUpdates.put("lastMessageSenderId", currentUserId);
                roomUpdates.put("lastMessageAt", now);
                roomUpdates.put("updatedAt", now);
                roomUpdates.put("createdAt", now);

                WriteBatch batch = db.batch();
                batch.set(roomRef, roomUpdates, SetOptions.merge());
                batch.set(messageRef, message);

                batch.commit()
                        .addOnSuccessListener(unused -> {
                            sendMessageNotification(currentUserId, friendId, text);
                            callback.onSuccess();
                        })
                        .addOnFailureListener(e -> callback.onFailure("Không gửi được tin nhắn: " + e.getMessage()));
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onFailure(errorMessage);
            }
        });
    }

    public ListenerRegistration listenMessages(@NonNull String roomId,
                                               @NonNull MessageListCallback callback) {
        return db.collection(Constants.COLLECTION_CHAT_ROOMS)
                .document(roomId)
                .collection(Constants.COLLECTION_MESSAGES)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        callback.onFailure("Không tải được tin nhắn: " + error.getMessage());
                        return;
                    }

                    List<ChatMessage> messages = new ArrayList<>();
                    if (snapshot != null) {
                        for (QueryDocumentSnapshot document : snapshot) {
                            ChatMessage message = document.toObject(ChatMessage.class);
                            if (message == null) continue;
                            if (message.getMessageId() == null || message.getMessageId().trim().isEmpty()) {
                                message.setMessageId(document.getId());
                            }
                            messages.add(message);
                        }
                    }

                    sortMessagesOldestFirst(messages);
                    callback.onSuccess(messages);
                });
    }

    public void loadChatRooms(@NonNull String currentUserId,
                              @NonNull ChatRoomListCallback callback) {
        if (currentUserId.trim().isEmpty()) {
            callback.onFailure("Không xác định được tài khoản hiện tại");
            return;
        }

        db.collection(Constants.COLLECTION_CHAT_ROOMS)
                .whereArrayContains("memberIds", currentUserId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<ChatRoom> rooms = new ArrayList<>();
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        ChatRoom room = document.toObject(ChatRoom.class);
                        if (room == null) continue;
                        if (room.getRoomId() == null || room.getRoomId().trim().isEmpty()) {
                            room.setRoomId(document.getId());
                        }
                        rooms.add(room);
                    }

                    sortRoomsNewestFirst(rooms);
                    callback.onSuccess(rooms);
                })
                .addOnFailureListener(e -> callback.onFailure("Không tải được danh sách tin nhắn: " + e.getMessage()));
    }

    public String buildRoomId(@NonNull String userId1, @NonNull String userId2) {
        String first = userId1.trim();
        String second = userId2.trim();
        if (first.compareTo(second) <= 0) {
            return first + "_" + second;
        }
        return second + "_" + first;
    }

    private void ensureFriendship(@NonNull String currentUserId,
                                  @NonNull String friendId,
                                  @NonNull ActionCallback callback) {
        String relationId = currentUserId.trim() + "_" + friendId.trim();
        db.collection(Constants.COLLECTION_FRIENDS)
                .document(relationId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure("Chỉ có thể nhắn tin với người đã kết bạn");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Không kiểm tra được quan hệ bạn bè: " + e.getMessage()));
    }

    private void sendMessageNotification(@NonNull String senderId,
                                         @NonNull String receiverId,
                                         @NonNull String messageText) {
        userRepository.getUserById(senderId, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User sender) {
                String senderName = buildDisplayName(sender);
                notificationRepository.createNotification(
                        receiverId,
                        Constants.NOTIFICATION_TYPE_MESSAGE,
                        "Tin nhắn mới",
                        senderName + ": " + buildMessagePreview(messageText),
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

    private ChatRoom buildEmptyRoom(String roomId, String currentUserId, String friendId) {
        ChatRoom room = new ChatRoom();
        room.setRoomId(roomId);
        room.setMemberIds(buildMemberIds(currentUserId, friendId));
        room.setUserAId(getUserAId(currentUserId, friendId));
        room.setUserBId(getUserBId(currentUserId, friendId));
        room.setLastMessage("");
        room.setLastMessageSenderId("");
        room.setCreatedAt(Timestamp.now());
        room.setUpdatedAt(Timestamp.now());
        return room;
    }

    private List<String> buildMemberIds(String currentUserId, String friendId) {
        return Arrays.asList(getUserAId(currentUserId, friendId), getUserBId(currentUserId, friendId));
    }

    private String getUserAId(String currentUserId, String friendId) {
        String first = currentUserId.trim();
        String second = friendId.trim();
        return first.compareTo(second) <= 0 ? first : second;
    }

    private String getUserBId(String currentUserId, String friendId) {
        String first = currentUserId.trim();
        String second = friendId.trim();
        return first.compareTo(second) <= 0 ? second : first;
    }

    private boolean isValidPair(String currentUserId, String friendId) {
        return currentUserId != null
                && friendId != null
                && !currentUserId.trim().isEmpty()
                && !friendId.trim().isEmpty()
                && !currentUserId.trim().equals(friendId.trim());
    }

    private void sortMessagesOldestFirst(List<ChatMessage> messages) {
        Collections.sort(messages, (left, right) -> {
            Timestamp leftTime = left.getCreatedAt();
            Timestamp rightTime = right.getCreatedAt();

            if (leftTime == null && rightTime == null) return 0;
            if (leftTime == null) return -1;
            if (rightTime == null) return 1;
            return leftTime.compareTo(rightTime);
        });
    }

    private void sortRoomsNewestFirst(List<ChatRoom> rooms) {
        Collections.sort(rooms, (left, right) -> {
            Timestamp leftTime = left.getLastMessageAt() != null ? left.getLastMessageAt() : left.getUpdatedAt();
            Timestamp rightTime = right.getLastMessageAt() != null ? right.getLastMessageAt() : right.getUpdatedAt();

            if (leftTime == null && rightTime == null) return 0;
            if (leftTime == null) return 1;
            if (rightTime == null) return -1;
            return rightTime.compareTo(leftTime);
        });
    }

    private String buildDisplayName(User user) {
        if (user == null) return "Một người bạn";
        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) return "@" + user.getUsername().trim();
        return "Một người bạn";
    }

    private String buildMessagePreview(String text) {
        String safeText = text == null ? "" : text.trim().replace('\n', ' ');
        if (safeText.length() <= 60) return safeText;
        return safeText.substring(0, 60) + "...";
    }
}
