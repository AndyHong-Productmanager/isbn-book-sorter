package com.andy.isbnbooksorter;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;

import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class ScannerController {
    interface Listener {
        void onIsbnDetected(String isbn);

        void onScannerError(String message);
    }

    private final Activity activity;
    private final PreviewView previewView;
    private final Listener listener;
    private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private final BarcodeScanner scanner = BarcodeScanning.getClient();
    private boolean paused = true;

    ScannerController(Activity activity, PreviewView previewView, Listener listener) {
        this.activity = activity;
        this.previewView = previewView;
        this.listener = listener;
    }

    boolean start() {
        if (!hasAvailableCamera()) {
            listener.onScannerError("사용 가능한 카메라를 찾을 수 없습니다. 수동 ISBN 입력을 사용하세요.");
            return false;
        }
        paused = false;
        ListenableFuture<ProcessCameraProvider> providerFuture = ProcessCameraProvider.getInstance(activity);
        providerFuture.addListener(() -> bindProvider(providerFuture), ContextCompat.getMainExecutor(activity));
        return true;
    }

    private boolean hasAvailableCamera() {
        if (!activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return false;
        }
        CameraManager cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (cameraManager == null) {
            return false;
        }
        try {
            return cameraManager.getCameraIdList().length > 0;
        } catch (CameraAccessException exception) {
            return false;
        }
    }

    void pause() {
        paused = true;
    }

    void shutdown() {
        paused = true;
        scanner.close();
        cameraExecutor.shutdownNow();
    }

    private void bindProvider(ListenableFuture<ProcessCameraProvider> providerFuture) {
        try {
            ProcessCameraProvider provider = providerFuture.get();
            Preview preview = new Preview.Builder().build();
            ImageAnalysis analysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build();
            preview.setSurfaceProvider(previewView.getSurfaceProvider());
            analysis.setAnalyzer(cameraExecutor, this::analyze);
            provider.unbindAll();
            provider.bindToLifecycle(
                    (LifecycleOwner) activity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis);
        } catch (Exception exception) {
            listener.onScannerError("카메라 시작 실패: " + exception.getMessage());
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyze(ImageProxy imageProxy) {
        if (paused) {
            imageProxy.close();
            return;
        }
        Image image = imageProxy.getImage();
        if (image == null) {
            imageProxy.close();
            return;
        }
        InputImage inputImage = InputImage.fromMediaImage(
                image,
                imageProxy.getImageInfo().getRotationDegrees());
        scanner.process(inputImage)
                .addOnSuccessListener(this::handleBarcodes)
                .addOnFailureListener(error -> listener.onScannerError("ISBN 인식 실패"))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        for (Barcode barcode : barcodes) {
            int format = barcode.getFormat();
            if (format != Barcode.FORMAT_EAN_13 && format != Barcode.FORMAT_EAN_8) {
                continue;
            }
            String rawValue = barcode.getRawValue();
            String isbn = isbnFromBarcodeValue(format, rawValue);
            if (!isbn.isEmpty()) {
                paused = true;
                listener.onIsbnDetected(isbn);
                return;
            }
        }
    }

    static String isbnFromBarcodeValue(int format, String rawValue) {
        if (format != Barcode.FORMAT_EAN_13 || rawValue == null) {
            return "";
        }
        String isbn = IsbnUtils.normalize(rawValue);
        if (!IsbnUtils.isBooklandIsbn13(isbn)) {
            return "";
        }
        return isbn;
    }
}
