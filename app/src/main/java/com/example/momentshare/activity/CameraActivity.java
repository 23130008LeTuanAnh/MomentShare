package com.example.momentshare.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCapture;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.momentshare.R;
import com.example.momentshare.util.CameraHelper;

public class CameraActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    public static final String EXTRA_IMAGE_URI = "extra_image_uri";

    private PreviewView previewView;
    private Button btnCapture;
    private ImageButton btnClose;

    private CameraHelper cameraHelper;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        cameraHelper = new CameraHelper();

        initViews();
        setupEvents();
        checkCameraPermissionAndStart();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewViewCamera);
        btnCapture = findViewById(R.id.btnCapture);
        btnClose = findViewById(R.id.btnCloseCamera);
    }

    private void setupEvents() {
        btnCapture.setOnClickListener(v -> capturePhoto());

        btnClose.setOnClickListener(v -> finish());
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION
            );
        }
    }

    private void startCamera() {
        cameraHelper.bindCamera(this, previewView, new CameraHelper.CameraReadyCallback() {
            @Override
            public void onReady(@NonNull ImageCapture capture) {
                imageCapture = capture;
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void capturePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "Camera chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        cameraHelper.takePhoto(this, imageCapture, new CameraHelper.CaptureCallback() {
            @Override
            public void onSuccess(@NonNull Uri imageUri) {
                Intent intent = new Intent(CameraActivity.this, PreviewMomentActivity.class);
                intent.putExtra(EXTRA_IMAGE_URI, imageUri.toString());
                startActivity(intent);
            }

            @Override
            public void onFailure(@NonNull String errorMessage) {
                Toast.makeText(CameraActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Cần quyền camera để chụp ảnh", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraHelper != null) {
            cameraHelper.release();
        }
    }
}
