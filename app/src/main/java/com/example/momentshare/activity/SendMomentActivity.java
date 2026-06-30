package com.example.momentshare.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.repository.MomentRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class SendMomentActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CAPTION = "extra_caption";
    public static final String EXTRA_RECEIVER_IDS = "extra_receiver_ids";

    private ProgressDialog progressDialog;
    private MomentRepository momentRepository;

    private Uri imageUri;
    private String caption;
    private ArrayList<String> receiverIds;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        momentRepository = new MomentRepository(this);

        readIntentData();
        if (imageUri == null || receiverIds == null || receiverIds.isEmpty()) {
            Toast.makeText(this, "Dữ liệu gửi không hợp lệ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showProgress();
        sendMoment();
    }

    private void readIntentData() {
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);
        caption = getIntent().getStringExtra(EXTRA_CAPTION);
        receiverIds = getIntent().getStringArrayListExtra(EXTRA_RECEIVER_IDS);

        if (uriString != null && !uriString.trim().isEmpty()) {
            imageUri = Uri.parse(uriString);
        }

        if (caption == null) {
            caption = "";
        }
    }

    private void sendMoment() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (currentUserId == null) {
            hideProgress();
            Toast.makeText(this, "Bạn cần đăng nhập lại", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        momentRepository.sendMoment(currentUserId, imageUri, caption, receiverIds,
                new MomentRepository.SendMomentCallback() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull String momentId) {
                        hideProgress();
                        Toast.makeText(SendMomentActivity.this,
                                "Gửi khoảnh khắc thành công",
                                Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(SendMomentActivity.this, ProfileActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(@androidx.annotation.NonNull String errorMessage) {
                        hideProgress();
                        Toast.makeText(SendMomentActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void showProgress() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang gửi khoảnh khắc...");
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
}
