package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public final class MlKitBarcodeScanInstrumentedTest {
    @Test
    public void mlKitDecodesGeneratedIsbnEan13Image() throws Exception {
        Bitmap bitmap = barcodeBitmap("9788937460449");
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        BarcodeScanner scanner = BarcodeScanning.getClient();
        try {
            List<Barcode> barcodes = Tasks.await(scanner.process(image), 15, TimeUnit.SECONDS);
            String isbn = "";
            for (Barcode barcode : barcodes) {
                isbn = ScannerController.isbnFromBarcodeValue(barcode.getFormat(), barcode.getRawValue());
                if (!isbn.isEmpty()) {
                    break;
                }
            }
            assertEquals("9788937460449", isbn);
        } finally {
            scanner.close();
        }
    }

    private static Bitmap barcodeBitmap(String value) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.EAN_13, 900, 360);
        Bitmap bitmap = Bitmap.createBitmap(matrix.getWidth(), matrix.getHeight(), Bitmap.Config.ARGB_8888);
        for (int y = 0; y < matrix.getHeight(); y += 1) {
            for (int x = 0; x < matrix.getWidth(); x += 1) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }
}
