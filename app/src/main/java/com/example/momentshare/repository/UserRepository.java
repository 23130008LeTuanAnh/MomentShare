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
 * - Lấy thông tin hồ sơ người dùng theo userId.
 * - Tìm người dùng theo username để hỗ trợ đăng nhập bằng username.
 * - Cập nhật họ tên và bio trong hồ sơ cá nhân.
 * - Cập nhật avatarUrl khi người dùng đổi ảnh đại diện.
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
     * Hàm này dùng trong màn hình đăng ký để đảm bảo username không bị trùng.
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
     * Hàm này được gọi sau khi Firebase Authentication tạo tài khoản thành công.
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
     * Hàm này dùng trong ProfileActivity, EditProfileActivity và SplashActivity.
     *
     * @param userId mã người dùng lấy từ Firebase Authentication
     * @param callback callback trả về User nếu tìm thấy
     */
    public void getUserById(@NonNull String userId, @NonNull UserCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot ->
                        handleUserDocument(documentSnapshot, callback))
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể tải hồ sơ người dùng: " + e.getMessage()));
    }

    /**
     * Tìm thông tin người dùng theo username.
     *
     * Hàm này phục vụ chức năng đăng nhập bằng username.
     *
     * Quy trình:
     * - Tìm document trong collection users có username trùng với dữ liệu người dùng nhập.
     * - Nếu tìm thấy, trả về User để AuthManager lấy email đăng nhập FirebaseAuth.
     * - Nếu không tìm thấy, trả về lỗi "Tài khoản không tồn tại".
     *
     * @param username username đã được chuẩn hóa
     * @param callback callback trả về User hoặc thông báo lỗi
     */
    public void getUserByUsername(@NonNull String username, @NonNull UserCallback callback) {
        db.collection(Constants.COLLECTION_USERS)
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        callback.onFailure("Tài khoản không tồn tại");
                        return;
                    }

                    DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                    handleUserDocument(documentSnapshot, callback);
                })
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể tìm tài khoản: " + e.getMessage()));
    }

    /**
     * Cập nhật họ tên và bio trong hồ sơ cá nhân.
     *
     * Hàm này dùng trong EditProfileActivity khi người dùng không chọn avatar mới.
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
     * Cập nhật họ tên, bio và avatarUrl trong hồ sơ cá nhân.
     *
     * Hàm này dùng khi người dùng chọn avatar mới trong EditProfileActivity.
     *
     * Quy trình:
     * 1. Người dùng chọn avatar mới.
     * 2. StorageRepository upload ảnh lên Firebase Storage.
     * 3. Sau khi upload thành công, nhận downloadUrl.
     * 4. Hàm này lưu fullName, bio và avatarUrl mới vào Firestore.
     *
     * @param userId mã người dùng hiện tại
     * @param fullName họ tên mới
     * @param bio mô tả cá nhân mới
     * @param avatarUrl link avatar mới sau khi upload lên Firebase Storage
     * @param callback callback báo thành công/thất bại
     */
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
                .addOnFailureListener(e ->
                        callback.onFailure("Không thể cập nhật avatar: " + e.getMessage()));
    }

    /**
     * Chuyển DocumentSnapshot thành User object.
     *
     * Hàm này dùng chung cho getUserById() và getUserByUsername()
     * để tránh lặp code xử lý document Firestore.
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
