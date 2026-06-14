package com.example.momentshare.repository;

import androidx.annotation.NonNull;

import com.example.momentshare.model.User;
import com.example.momentshare.util.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * UserRepository chịu trách nhiệm làm việc với collection users trên Firestore.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Nhiệm vụ chính:
 * - Kiểm tra username đã tồn tại chưa.
 * - Tạo hồ sơ người dùng sau khi đăng ký Firebase Authentication.
 * - Lấy thông tin hồ sơ người dùng.
 * - Cập nhật họ tên và bio trong hồ sơ cá nhân.
 */
public class UserRepository {

    private final FirebaseFirestore db;

    /**
     * Constructor khởi tạo Firestore instance.
     */
    public UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Callback dùng cho các thao tác trả về true/false.
     */
    public interface BooleanCallback {
        void onSuccess(boolean result);

        void onFailure(String errorMessage);
    }

    /**
     * Callback dùng khi lấy dữ liệu User từ Firestore.
     */
    public interface UserCallback {
        void onSuccess(User user);

        void onFailure(String errorMessage);
    }

    /**
     * Callback dùng cho thao tác không cần trả dữ liệu.
     */
    public interface ActionCallback {
        void onSuccess();

        void onFailure(String errorMessage);
    }

    /**
     * Kiểm tra username đã tồn tại trong collection users hay chưa.
     *
     * @param username username cần kiểm tra
     * @param callback callback trả về true nếu username đã tồn tại
     */
    public void checkUsernameExists(@NonNull String username, @NonNull BooleanCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        callback.onSuccess(!querySnapshot.isEmpty()))
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể kiểm tra username: " + e.getMessage()));
    }

    /**
     * Tạo document người dùng mới trong Firestore.
     *
     * @param user object User chứa thông tin hồ sơ
     * @param callback callback báo thành công/thất bại
     */
    public void createUser(@NonNull User user, @NonNull ActionCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(user.getUserId())
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể lưu thông tin người dùng: " + e.getMessage()));
    }

    /**
     * Lấy thông tin người dùng theo userId.
     *
     * @param userId mã người dùng lấy từ Firebase Authentication
     * @param callback callback trả về User nếu tìm thấy
     */
    public void getUserById(@NonNull String userId, @NonNull UserCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> handleUserDocument(documentSnapshot, callback))
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể tải hồ sơ người dùng: " + e.getMessage()));
    }

    /**
     * Cập nhật họ tên và bio trong hồ sơ cá nhân.
     *
     * @param userId mã người dùng hiện tại
     * @param fullName họ tên mới
     * @param bio mô tả cá nhân mới
     * @param callback callback báo thành công/thất bại
     */
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
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể cập nhật hồ sơ: " + e.getMessage()));
    }

    /**
     * Chuyển DocumentSnapshot thành User object.
     *
     * @param documentSnapshot dữ liệu document từ Firestore
     * @param callback callback trả kết quả
     */
    private void handleUserDocument(@NonNull DocumentSnapshot documentSnapshot,
                                    @NonNull UserCallback callback) {
        if (!documentSnapshot.exists()) {
            callback.onFailure("Không tìm thấy hồ sơ người dùng");
            return;
        }

        User user = documentSnapshot.toObject(User.class);

        if (user == null) {
            callback.onFailure("Dữ liệu hồ sơ không hợp lệ");
            return;
        }

        callback.onSuccess(user);
    }
}