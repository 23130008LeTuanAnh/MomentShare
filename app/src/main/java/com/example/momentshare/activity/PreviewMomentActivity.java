package com.example.momentshare.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.momentshare.R;
import com.bumptech.glide.Glide;

public class PreviewMomentActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";
    public static final String EXTRA_CAPTION = "extra_caption";

    private ImageView imgPreview;
    private EditText edtCaption;
    private Button btnRetake;
    private Button btnNext;

    private Uri imageUri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview_moment);

        initViews();
        readIntentData();
        setupEvents();
    }

    private void initViews() {
        imgPreview = findViewById(R.id.imgMomentPreview);
        edtCaption = findViewById(R.id.edtCaptionMoment);
        btnRetake = findViewById(R.id.btnRetakeMoment);
        btnNext = findViewById(R.id.btnNextMoment);
    }

    private void readIntentData() {
        String uriString = getIntent().getStringExtra(EXTRA_IMAGE_URI);

        if (uriString == null || uriString.trim().isEmpty()) {
            Toast.makeText(this, "Không có ảnh để xem trước", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        imageUri = Uri.parse(uriString);
        Glide.with(this).load(imageUri).into(imgPreview);
    }

    private void setupEvents() {
        btnRetake.setOnClickListener(v -> finish());

        btnNext.setOnClickListener(v -> {
            if (imageUri == null) {
                Toast.makeText(this, "Ảnh không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(PreviewMomentActivity.this, SelectFriendToSendActivity.class);
            intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
            intent.putExtra(EXTRA_CAPTION, edtCaption.getText().toString().trim());
            startActivity(intent);
        });
    }
}