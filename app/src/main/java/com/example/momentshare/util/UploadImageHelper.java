package com.example.momentshare.util;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UploadImageHelper {

    public interface UploadCallback {
        void onSuccess(@NonNull String downloadUrl);
        void onFailure(@NonNull String errorMessage);
    }

    private final StorageReference storageReference;

    public UploadImageHelper() {
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    public void uploadMomentImage(@NonNull String senderId,
                                  @NonNull Uri imageUri,
                                  @NonNull UploadCallback callback) {

        // Tạo tên file duy nhất dựa trên thời gian
        String fileName = senderId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageReference.child("moments").child(fileName);

        // Bỏ qua nén ảnh bị lỗi trên Android mới, upload thẳng file gốc bằng Uri
        uploadOriginalFile(imageRef, imageUri, callback);
    }

    private void uploadOriginalFile(StorageReference imageRef, Uri imageUri, UploadCallback callback) {
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e ->
                                        callback.onFailure("Không lấy được link: " + e.toString())))
                .addOnFailureListener(e -> {
                    // In ra CHI TIẾT lỗi (e.toString()) để biết chính xác do Firebase chặn hay do điện thoại
                    callback.onFailure("Upload ảnh thất bại: " + e.toString());
                });
    }
}
