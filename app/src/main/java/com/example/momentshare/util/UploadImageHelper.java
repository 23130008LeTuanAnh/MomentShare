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

        String fileName = senderId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = storageReference.child("moments").child(fileName);

        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl()
                                .addOnSuccessListener(uri ->
                                        callback.onSuccess(uri.toString()))
                                .addOnFailureListener(e ->
                                        callback.onFailure("Không lấy được link ảnh: " + e.getMessage())))
                .addOnFailureListener(e ->
                        callback.onFailure("Upload ảnh thất bại: " + e.getMessage()));
    }
}