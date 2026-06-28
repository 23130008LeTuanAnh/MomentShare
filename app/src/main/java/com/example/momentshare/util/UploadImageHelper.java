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

    /**
     * Người 5 thực hiện: bật chế độ demo khi Firebase Storage chưa dùng được do yêu cầu nâng cấp billing.
     *
     * true  = không upload lên Firebase Storage, dùng ảnh mẫu URL để demo luồng gửi/xem/reaction/report.
     * false = upload ảnh thật lên Firebase Storage khi project đã bật Storage/Blaze.
     */
    private static final boolean USE_DEMO_IMAGE_URL = true;

    /**
     * Người 5 thực hiện: URL ảnh mẫu dùng cho demo khi chưa bật Firebase Storage.
     */
    private static final String DEMO_MOMENT_IMAGE_URL =
            "https://picsum.photos/seed/momentshare-moment/800/1200";

    private final StorageReference storageReference;

    public UploadImageHelper() {
        storageReference = FirebaseStorage.getInstance().getReference();
    }

    public void uploadMomentImage(@NonNull String senderId,
                                  @NonNull Uri imageUri,
                                  @NonNull UploadCallback callback) {

        /**
         * Người 5 thực hiện:
         * Nếu đang demo và chưa bật Storage/Blaze, không gọi putFile().
         * Hệ thống trả về URL ảnh mẫu để MomentRepository vẫn tạo được moment,
         * moment_receivers và notifications trong Firestore.
         */
        if (USE_DEMO_IMAGE_URL) {
            callback.onSuccess(DEMO_MOMENT_IMAGE_URL);
            return;
        }

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
