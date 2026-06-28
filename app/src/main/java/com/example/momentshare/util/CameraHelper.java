package com.example.momentshare.util;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraHelper {

    public interface CameraReadyCallback {
        void onReady(@NonNull ImageCapture imageCapture);
        void onFailure(@NonNull String errorMessage);
    }

    public interface CaptureCallback {
        void onSuccess(@NonNull Uri imageUri);
        void onFailure(@NonNull String errorMessage);
    }

    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private ImageCapture currentImageCapture;

    public void bindCamera(@NonNull AppCompatActivity activity,
                           @NonNull PreviewView previewView,
                           @NonNull CameraReadyCallback callback) {

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(activity);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                currentImageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        activity,
                        cameraSelector,
                        preview,
                        currentImageCapture
                );

                callback.onReady(currentImageCapture);
            } catch (Exception e) {
                callback.onFailure("Không thể mở camera: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    public void takePhoto(@NonNull AppCompatActivity activity,
                          @NonNull ImageCapture imageCapture,
                          @NonNull CaptureCallback callback) {

        File photoFile;
        try {
            File outputDir = activity.getCacheDir();
            photoFile = File.createTempFile("moment_", ".jpg", outputDir);
        } catch (Exception e) {
            callback.onFailure("Không thể tạo file ảnh tạm: " + e.getMessage());
            return;
        }

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(activity),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        callback.onSuccess(Uri.fromFile(photoFile));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        callback.onFailure("Chụp ảnh thất bại: " + exception.getMessage());
                    }
                }
        );
    }

    public void release() {
        cameraExecutor.shutdown();
    }

    public ImageCapture getCurrentImageCapture() {
        return currentImageCapture;
    }
}
