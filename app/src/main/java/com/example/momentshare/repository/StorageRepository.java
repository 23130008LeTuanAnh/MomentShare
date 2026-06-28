package com.example.momentshare.repository;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

/**
 * StorageRepository chịu trách nhiệm làm việc với Firebase Storage.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Nhiệm vụ chính:
 * - Upload ảnh đại diện của người dùng lên Firebase Storage.
 * - Lấy downloadUrl sau khi upload thành công.
 * - Trả downloadUrl để lưu vào Firestore field avatarUrl.
 */
public class StorageRepository {

    /**
     * Người 5 thực hiện: bật chế độ demo khi Firebase Storage chưa dùng được do yêu cầu nâng cấp billing.
     *
     * true  = không upload avatar lên Firebase Storage, dùng avatar mẫu URL để demo cập nhật hồ sơ.
     * false = upload avatar thật lên Firebase Storage khi project đã bật Storage/Blaze.
     */
    private static final boolean USE_DEMO_IMAGE_URL = true;

    /**
     * Người 5 thực hiện: URL avatar mẫu dùng khi chưa bật Firebase Storage.
     */
    private static final String DEMO_AVATAR_IMAGE_URL =
            "https://i.pravatar.cc/300?img=12";

    private final StorageReference storageReference;

    /**
     * Constructor khởi tạo Firebase Storage reference.
     */
    public StorageRepository() {
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    /**
     * Callback dùng cho thao tác upload avatar.
     */
    public interface UploadCallback {
        void onSuccess(String downloadUrl);

        void onFailure(String errorMessage);
    }

    /**
     * Upload avatar người dùng lên Firebase Storage.
     *
     * Đường dẫn lưu trên Firebase Storage:
     * avatars/{userId}_{timestamp}.jpg
     *
     * Quy trình:
     * 1. Tạo tên file avatar theo userId và thời gian hiện tại.
     * 2. Upload ảnh từ Uri lên Firebase Storage.
     * 3. Sau khi upload thành công, lấy downloadUrl.
     * 4. Trả downloadUrl về Activity để lưu vào Firestore.
     *
     * @param userId mã người dùng hiện tại
     * @param avatarUri Uri ảnh được chọn từ thư viện
     * @param callback callback trả về downloadUrl hoặc lỗi
     */
    public void uploadAvatar(@NonNull String userId,
                             @NonNull Uri avatarUri,
                             @NonNull UploadCallback callback) {

        /**
         * Người 5 thực hiện:
         * Nếu đang demo và chưa bật Storage/Blaze, không gọi putFile().
         * Hệ thống trả về avatar mẫu để EditProfileActivity vẫn cập nhật được avatarUrl trong Firestore.
         */
        if (USE_DEMO_IMAGE_URL) {
            callback.onSuccess(DEMO_AVATAR_IMAGE_URL);
            return;
        }

        String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";

        StorageReference avatarRef = storageReference
                .child("avatars")
                .child(fileName);

        avatarRef.putFile(avatarUri)
                .addOnSuccessListener(taskSnapshot ->
                        avatarRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e ->
                                        callback.onFailure("Không thể lấy link avatar: " + e.getMessage())))
                .addOnFailureListener(e ->
                        callback.onFailure("Upload avatar thất bại: " + e.getMessage()));
    }
}
