package com.example.momentshare.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.example.momentshare.model.FriendUser;
import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class SelectFriendToSendActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CAPTION = "extra_caption";

    private ListView listFriends;
    private TextView txtEmpty;
    private Button btnContinue;
    private TextView txtDebugError;
    private ProgressDialog progressDialog;

    private final ArrayList<FriendUser> friendCandidates = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private MomentRepository momentRepository;
    private Uri imageUri;
    private String caption;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend_send);

        momentRepository = new MomentRepository(this);

        initViews();
        readIntentData();
        loadFriends();
    }

    private void initViews() {
        listFriends = findViewById(R.id.listFriendsToSend);
        txtEmpty = findViewById(R.id.txtEmptyFriends);
        btnContinue = findViewById(R.id.btnContinueSend);
        txtDebugError = findViewById(R.id.txtDebugError);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarSelectFriend);
        toolbar.setNavigationIcon(android.R.drawable.ic_media_previous);
        toolbar.setNavigationOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> handleContinue());

        // Lắng nghe sự kiện tích chọn trên ListView để cập nhật chữ hiển thị trên nút bấm
        listFriends.setOnItemClickListener((parent, view, position, id) -> updateButtonText());
    }

    private void readIntentData() {
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        caption = getIntent().getStringExtra(EXTRA_CAPTION);

        if (uriString == null || uriString.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu ảnh", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageUri = Uri.parse(uriString);
        if (caption == null) caption = "";
    }

    private void loadFriends() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            Toast.makeText(this, "Bạn cần đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        momentRepository.loadSelectableFriends(currentUserId, new MomentRepository.FriendsCallback() {
            @Override
            public void onSuccess(@NonNull List<FriendUser> friends) {
                friendCandidates.clear();
                friendCandidates.addAll(friends);

                // NẾU CHƯA CÓ BẠN BÈ: Vẫn bật nút gửi để họ dùng tính năng ĐĂNG CÔNG KHAI
                if (friendCandidates.isEmpty()) {
                    txtEmpty.setVisibility(View.VISIBLE);
                    listFriends.setVisibility(View.GONE);
                    btnContinue.setEnabled(true);
                    btnContinue.setText("Đăng công khai (Mọi người)");
                    return;
                }

                txtEmpty.setVisibility(View.GONE);
                listFriends.setVisibility(View.VISIBLE);
                btnContinue.setEnabled(true);

                ArrayList<String> labels = new ArrayList<>();
                for (FriendUser friend : friendCandidates) {
                    labels.add(formatFriendLabel(friend));
                }

                adapter = new ArrayAdapter<>(
                        SelectFriendToSendActivity.this,
                        android.R.layout.simple_list_item_multiple_choice,
                        labels
                );

                listFriends.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
                listFriends.setAdapter(adapter);

                // Cập nhật trạng thái chữ mặc định ban đầu cho nút bấm
                updateButtonText();
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                Toast.makeText(SelectFriendToSendActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Hàm tự động tính toán số người được chọn để đổi tên hiển thị trên nút bấm
     */
    private void updateButtonText() {
        int checkedCount = 0;
        for (int i = 0; i < listFriends.getCount(); i++) {
            if (listFriends.isItemChecked(i)) {
                checkedCount++;
            }
        }

        if (checkedCount == 0) {
            btnContinue.setText("Đăng công khai (Mọi người)");
        } else {
            btnContinue.setText("Gửi khoảnh khắc (" + checkedCount + " bạn bè)");
        }
    }

    private String formatFriendLabel(FriendUser friend) {
        String fullName = friend.getFullName() == null ? "" : friend.getFullName();
        String username = friend.getUsername() == null ? "" : friend.getUsername();

        if (username.isEmpty()) return fullName;
        if (fullName.isEmpty()) return "@" + username;
        return fullName + "  @" + username;
    }

    private void handleContinue() {
        txtDebugError.setVisibility(View.GONE);
        ArrayList<String> receiverIds = new ArrayList<>();

        for (int i = 0; i < listFriends.getCount(); i++) {
            if (listFriends.isItemChecked(i)) {
                receiverIds.add(friendCandidates.get(i).getUserId());
            }
        }
        
        // Nếu receiverIds rỗng -> Firebase sẽ nhận một danh sách trống và hiểu đây là bài đăng công khai công cộng.

        if (imageUri == null) {
            showError("Lỗi dữ liệu ảnh");
            return;
        }

        long fileSizeInBytes = getFileSize(imageUri);
        long maxFileSize = 5 * 1024 * 1024; // 5MB tính bằng bytes

        if (fileSizeInBytes > maxFileSize) {
            float fileSizeInMB = (float) fileSizeInBytes / (1024 * 1024);
            showError("Ảnh quá lớn! Vui lòng chọn ảnh dưới 5MB (Ảnh của bạn: " + String.format("%.2f", fileSizeInMB) + "MB)");
            return; // Chặn lại luôn không cho gửi lên Firebase nữa
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        showProgress();

        // Gửi trực tiếp lên cơ sở dữ liệu Firebase
        momentRepository.sendMoment(currentUserId, imageUri, caption, receiverIds,
                new MomentRepository.SendMomentCallback() {
                    @Override
                    public void onSuccess(@NonNull String momentId) {
                        hideProgress();

                        // Đưa ra thông báo tương ứng với phương thức gửi
                        String message = receiverIds.isEmpty() ? "Đã đăng khoảnh khắc công khai!" : "Đã gửi khoảnh khắc đến bạn bè!";
                        Toast.makeText(SelectFriendToSendActivity.this, message, Toast.LENGTH_SHORT).show();

                        // Điều hướng mượt mà về thẳng HomeFeedActivity
                        Intent intent = new Intent(SelectFriendToSendActivity.this, HomeFeedActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull String errorMessage) {
                        hideProgress();
                        showError("Lỗi Firebase: " + errorMessage);
                    }
                });
    }

    private void showError(String message) {
        txtDebugError.setVisibility(View.VISIBLE);
        txtDebugError.setText("❌ " + message);
    }

    private void showProgress() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang tải khoảnh khắc lên...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void hideProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        hideProgress();
        super.onDestroy();
    }

    /**
     * Hàm phụ dùng để tính toán chính xác dung lượng (bytes) của bức ảnh từ Uri
     */
    private long getFileSize(Uri uri) {
        if (uri == null) return 0;
        try {
            if ("content".equals(uri.getScheme())) {
                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    cursor.moveToFirst();
                    long size = cursor.getLong(sizeIndex);
                    cursor.close();
                    return size;
                }
            } else if ("file".equals(uri.getScheme())) {
                java.io.File file = new java.io.File(uri.getPath());
                return file.length();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
