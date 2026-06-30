package com.example.momentshare.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadImageHelper {

    // Người 5 thực hiện: dùng API ImgBB để upload ảnh khoảnh khắc thay cho Firebase Storage.
    private static final String IMGBB_API_KEY = "53a62caae877be80edea319ea7c5f533";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface UploadCallback {
        void onSuccess(@NonNull String downloadUrl);
        void onFailure(@NonNull String errorMessage);
    }

    // Cần truyền Context vào để đọc được Uri ảnh từ thiết bị.
    public UploadImageHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void uploadMomentImage(@NonNull String senderId,
                                  @NonNull Uri imageUri,
                                  @NonNull UploadCallback callback) {

        String base64Image = getBase64FromUri(imageUri);
        if (base64Image == null || base64Image.trim().isEmpty()) {
            callback.onFailure("Không thể đọc được file ảnh từ thiết bị.");
            return;
        }

        // Người 5 thực hiện: gửi ảnh lên ImgBB bằng multipart/form-data.
        // API key đặt trên query parameter, ảnh base64 đặt trong field image.
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url(IMGBB_UPLOAD_URL + "?key=" + IMGBB_API_KEY)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnMainThread(() -> callback.onFailure("Lỗi mạng khi upload ImgBB: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    runOnMainThread(() -> callback.onFailure(
                            "Upload ImgBB thất bại, mã lỗi: " + response.code()
                                    + " | Response: " + responseData));
                    return;
                }

                try {
                    JSONObject jsonObject = new JSONObject(responseData);

                    if (!jsonObject.optBoolean("success", true)) {
                        runOnMainThread(() -> callback.onFailure("ImgBB từ chối ảnh: " + responseData));
                        return;
                    }

                    JSONObject dataObject = jsonObject.getJSONObject("data");
                    String imageUrl = dataObject.optString("display_url",
                            dataObject.optString("url", ""));

                    if (imageUrl.trim().isEmpty()) {
                        runOnMainThread(() -> callback.onFailure("ImgBB upload thành công nhưng không trả về URL ảnh."));
                        return;
                    }

                    runOnMainThread(() -> callback.onSuccess(imageUrl));
                } catch (Exception e) {
                    runOnMainThread(() -> callback.onFailure(
                            "Lỗi xử lý dữ liệu ImgBB: " + e.getMessage()
                                    + " | Response: " + responseData));
                }
            }
        });
    }

    /**
     * Người 5 thực hiện: chuyển Uri ảnh sang Base64 để gửi API ImgBB.
     * Dùng Base64.NO_WRAP để chuỗi không bị xuống dòng, tránh lỗi khi gửi qua API.
     */
    private String getBase64FromUri(Uri uri) {
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                return null;
            }

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
