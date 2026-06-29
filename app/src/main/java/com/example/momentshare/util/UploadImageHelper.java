package com.example.momentshare.util;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.io.IOException;

public class UploadImageHelper {

    // Điền API Key của ImgBB vào đây
    private static final String IMGBB_API_KEY = "53a62caae877be80edea319ea7c5f533";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface UploadCallback {
        void onSuccess(@NonNull String downloadUrl);
        void onFailure(@NonNull String errorMessage);
    }

    // Cần truyền Context vào để đọc được Uri ảnh từ thiết bị
    public UploadImageHelper(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        // Handler để đẩy kết quả về Main Thread (UI Thread)
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void uploadMomentImage(@NonNull String senderId,
                                  @NonNull Uri imageUri,
                                  @NonNull UploadCallback callback) {

        // 1. Chuyển Uri thành chuỗi Base64
        String base64Image = getBase64FromUri(imageUri);
        if (base64Image == null) {
            callback.onFailure("Không thể đọc được file ảnh từ thiết bị.");
            return;
        }

        // 2. Tạo RequestBody gửi lên ImgBB
        RequestBody formBody = new FormBody.Builder()
                .add("key", IMGBB_API_KEY)
                .add("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url(IMGBB_UPLOAD_URL)
                .post(formBody)
                .build();

        // 3. Gọi API bất đồng bộ
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnMainThread(() -> callback.onFailure("Lỗi mạng khi upload: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONObject jsonObject = new JSONObject(responseData);

                        // Lấy URL ảnh trực tiếp từ JSON ImgBB trả về
                        JSONObject dataObject = jsonObject.getJSONObject("data");
                        String imageUrl = dataObject.getString("display_url");

                        runOnMainThread(() -> callback.onSuccess(imageUrl));
                    } catch (Exception e) {
                        runOnMainThread(() -> callback.onFailure("Lỗi xử lý dữ liệu từ ImgBB: " + e.getMessage()));
                    }
                } else {
                    runOnMainThread(() -> callback.onFailure("Upload thất bại, mã lỗi: " + response.code()));
                }
            }
        });
    }

    /**
     * Hàm hỗ trợ chuyển Uri thành chuỗi Base64
     */
    private String getBase64FromUri(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            byte[] imageBytes = outputStream.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Đảm bảo callback luôn được chạy trên Main Thread để không crash UI
     */
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
