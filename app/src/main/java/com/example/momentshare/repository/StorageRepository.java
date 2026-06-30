package com.example.momentshare.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * StorageRepository chịu trách nhiệm làm việc với dịch vụ lưu trữ ảnh.
 *
 * File này thuộc phần Người 1 - Tài khoản, hồ sơ cá nhân.
 *
 * Nhiệm vụ chính:
 * - Upload ảnh đại diện của người dùng lên hệ thống ImgBB (Thay thế cho Firebase Storage bị giới hạn gói).
 * - Lấy downloadUrl sau khi upload thành công.
 * - Trả downloadUrl để lưu vào Firestore field avatarUrl.
 */
public class StorageRepository {

    public interface UploadCallback {
        void onSuccess(@NonNull String downloadUrl);
        void onFailure(@NonNull String errorMessage);
    }

    /**
     * Người 5 thực hiện: bật chế độ demo khi Firebase Storage chưa dùng được do yêu cầu nâng cấp billing.
     * Hiện tại đã chuyển sang dùng API ImgBB nên đặt false để hệ thống luôn upload ảnh thật của người dùng.
     */
    private static final boolean USE_DEMO_IMAGE_URL = false;

    /**
     * Người 5 thực hiện: URL avatar mẫu dùng khi chưa bật Firebase Storage.
     */
    private static final String DEMO_AVATAR_IMAGE_URL =
            "https://i.pravatar.cc/300?img=12";

    // Cấu hình API ImgBB cho mục Update Avatar
    private static final String IMGBB_API_KEY = "53a62caae877be80edea319ea7c5f533";
    private static final String IMGBB_UPLOAD_URL = "https://api.imgbb.com/1/upload";

    private final Context context;
    private final OkHttpClient client;
    private final Handler mainHandler;

    /**
     * Constructor khởi tạo context và cấu hình mạng HTTP Client.
     */
    public StorageRepository(Context context) {
        this.context = context;
        this.client = new OkHttpClient();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Tải ảnh đại diện lên máy chủ ImgBB và trả về URL ảnh thật.
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

        // 1. Chuyển đổi file ảnh được chọn (Uri) sang chuỗi văn bản Base64
        String base64Image = getBase64FromUri(avatarUri);
        if (base64Image == null) {
            callback.onFailure("Không thể đọc được file ảnh đại diện từ thiết bị.");
            return;
        }

        // Người 5 thực hiện: dùng multipart/form-data để upload ảnh lên ImgBB ổn định hơn.
        // API key đặt ở query parameter, ảnh base64 đặt trong field image.
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url(IMGBB_UPLOAD_URL + "?key=" + IMGBB_API_KEY)
                .post(requestBody)
                .build();

        // 3. Thực thi tiến trình gửi dữ liệu bất đồng bộ qua mạng
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnMainThread(() -> callback.onFailure("Lỗi kết nối mạng: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseData = response.body() != null ? response.body().string() : "";

                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonObject = new JSONObject(responseData);

                        if (!jsonObject.optBoolean("success", true)) {
                            runOnMainThread(() -> callback.onFailure("ImgBB từ chối avatar: " + responseData));
                            return;
                        }

                        JSONObject dataObject = jsonObject.getJSONObject("data");
                        String imageUrl = dataObject.optString("display_url",
                                dataObject.optString("url", ""));

                        if (imageUrl.trim().isEmpty()) {
                            runOnMainThread(() -> callback.onFailure("ImgBB không trả về URL avatar."));
                            return;
                        }

                        runOnMainThread(() -> callback.onSuccess(imageUrl));
                    } catch (Exception e) {
                        runOnMainThread(() -> callback.onFailure("Lỗi cấu trúc dữ liệu ảnh: "
                                + e.getMessage() + " | Response: " + responseData));
                    }
                } else {
                    runOnMainThread(() -> callback.onFailure("Tải ảnh lên thất bại, mã lỗi: "
                            + response.code() + " | Response: " + responseData));
                }
            }
        });
    }

    /**
     * Hàm hỗ trợ chuyển Uri thành chuỗi Base64 để gửi qua kết nối API
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

            // Người 5 thực hiện: dùng NO_WRAP để base64 không bị xuống dòng khi gửi lên ImgBB.
            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Đảm bảo luồng callback luôn trả kết quả về Main Thread an toàn cho UI
     */
    private void runOnMainThread(Runnable runnable) {
        mainHandler.post(runnable);
    }
}
