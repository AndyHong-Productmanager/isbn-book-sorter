package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import com.google.mlkit.vision.barcode.common.Barcode;

import org.junit.Test;

public final class ScannerControllerTest {
    @Test
    public void isbnFromBarcodeValueAcceptsBooklandEan13() {
        assertEquals(
                "9788937460449",
                ScannerController.isbnFromBarcodeValue(Barcode.FORMAT_EAN_13, "978-89-374-6044-9"));
    }

    @Test
    public void isbnFromBarcodeValueRejectsNonIsbnBarcodes() {
        assertEquals("", ScannerController.isbnFromBarcodeValue(Barcode.FORMAT_EAN_8, "9788937460449"));
        assertEquals("", ScannerController.isbnFromBarcodeValue(Barcode.FORMAT_CODE_128, "9788937460449"));
        assertEquals("", ScannerController.isbnFromBarcodeValue(Barcode.FORMAT_EAN_13, "8801234567890"));
        assertEquals("", ScannerController.isbnFromBarcodeValue(Barcode.FORMAT_EAN_13, "9788937460440"));
        assertEquals("", ScannerController.isbnFromBarcodeValue(Barcode.FORMAT_EAN_13, null));
    }
}
