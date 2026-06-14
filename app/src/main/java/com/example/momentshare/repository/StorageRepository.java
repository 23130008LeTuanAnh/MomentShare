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