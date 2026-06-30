package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.example.momentshare.util.ValidationUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * UserRepository chịu trách nhiệm làm việc với collection users trên Firestore.
 *
 * Đã chỉnh:
 * - getUserById() fallback thêm query whereEqualTo("userId", userId).
 *   Nhờ vậy Home/Moment detail vẫn hiện tên người đăng nếu document ID không trùng UID nhưng field userId có tồn tại.
 */
public class UserRepository {

    private final FirebaseFirestore db;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface BooleanCallback {
        void onSuccess(boolean result);
        void onFailure(String errorMessage);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public void checkUsernameExists(@NonNull String username, @NonNull BooleanCallback callback) {
        String normalizedUsername = ValidationUtils.normalizeUsername(username);

        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", normalizedUsername)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onSuccess(!querySnapshot.isEmpty()))
                .addOnFailureListener(e -> callback.onFailure("Không thể kiểm tra username: " + e.getMessage()));
    }

    public void createUser(@NonNull User user, @NonNull ActionCallback callback) {
        user.setUsername(ValidationUtils.normalizeUsername(user.getUsername()));
        user.setEmail(ValidationUtils.normalizeEmail(user.getEmail()));

        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể lưu thông tin người dùng: " + e.getMessage()));
    }

    public void getUserById(@NonNull String userId, @NonNull UserCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        handleUserDocument(documentSnapshot, callback);
                        return;
                    }

                    db.collection(Constants.COLLECTION_USERS)
                            .whereEqualTo("userId", userId)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (querySnapshot.isEmpty()) {
                                    callback.onFailure("Không tìm thấy hồ sơ người dùng");
                                    return;
                                }
                                handleUserDocument(querySnapshot.getDocuments().get(0), callback);
                            })
                            .addOnFailureListener(e -> callback.onFailure("Không thể tải hồ sơ người dùng: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Không thể tải hồ sơ người dùng: " + e.getMessage()));
    }

    public void getUserByUsername(@NonNull String username, @NonNull UserCallback callback) {
        String normalizedUsername = ValidationUtils.normalizeUsername(username);

        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", normalizedUsername)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure("Tài khoản không tồn tại");
                        return;
                    }
                    handleUserDocument(querySnapshot.getDocuments().get(0), callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Không thể tìm tài khoản: " + e.getMessage()));
    }

    public void updateProfile(@NonNull String userId,
                              @NonNull String fullName,
                              @NonNull String bio,
                              @NonNull ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("bio", bio);

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể cập nhật hồ sơ: " + e.getMessage()));
    }

    public void updateProfileWithAvatar(@NonNull String userId,
                                        @NonNull String fullName,
                                        @NonNull String bio,
                                        @NonNull String avatarUrl,
                                        @NonNull ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("bio", bio);
        updates.put("avatarUrl", avatarUrl);

        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure("Không thể cập nhật avatar: " + e.getMessage()));
    }

    private void handleUserDocument(@NonNull DocumentSnapshot documentSnapshot,
                                    @NonNull UserCallback callback) {
        User user = documentSnapshot.toObject(User.class);
        if (user == null) {
            callback.onFailure("Dữ liệu hồ sơ không hợp lệ");
            return;
        }
        callback.onSuccess(user);
    }
}
