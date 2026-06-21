package com.andy.isbnbooksorter;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

public final class MainActivity extends ComponentActivity {
    private BookRepository repository;
    private BibliographyClient client;
    private ScannerController scanner;
    private UiKit ui;
    private BookListRenderer bookListRenderer;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private TextView statusText;
    private EditText isbnInput;
    private EditText categoryInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ui = new UiKit(this);
        repository = new BookRepository(this);
        client = new BibliographyClient(this);
        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        status("카메라 권한 승인됨. 스캔 시작을 누르세요.", UiKit.TEXT_SECONDARY);
                    } else {
                        status("카메라 권한이 없어 수동 ISBN 입력만 사용할 수 있습니다.", UiKit.STATUS_WARNING);
                    }
                });
        setContentView(createContent());
        renderBooks();
        if (hasCameraPermission()) {
            status("카메라 준비 완료. ISBN 바코드를 화면에 맞춰주세요.", UiKit.TEXT_SECONDARY);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        if (scanner != null) {
            scanner.shutdown();
        }
        client.shutdown();
        repository.close();
        super.onDestroy();
    }

    private View createContent() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(UiKit.SURFACE_PRIMARY);
        LinearLayout root = ui.column(20);
        root.setPadding(ui.dp(16), ui.dp(18), ui.dp(16), ui.dp(24));
        scroll.addView(root);

        TextView title = ui.text("ISBN Book Sorter", 24, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        root.addView(title);
        root.addView(ui.text("ISBN을 스캔하거나 직접 입력하면 카테고리별로 저장됩니다.", 14, UiKit.TEXT_SECONDARY, Typeface.NORMAL));

        PreviewView preview = new PreviewView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ui.dp(260));
        previewParams.setMargins(0, ui.dp(16), 0, ui.dp(12));
        preview.setLayoutParams(previewParams);
        preview.setBackgroundColor(UiKit.SCANNER_SURFACE);
        root.addView(preview);

        scanner = new ScannerController(this, preview, new ScannerController.Listener() {
            @Override
            public void onIsbnDetected(String isbn) {
                runOnUiThread(() -> lookup(isbn));
            }

            @Override
            public void onScannerError(String message) {
                runOnUiThread(() -> status(message, UiKit.STATUS_ERROR));
            }
        });

        LinearLayout scanActions = ui.row(8);
        Button scanButton = ui.button("스캔 시작");
        scanButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        scanButton.setOnClickListener(view -> startScanner());
        Button pauseButton = ui.button("일시정지");
        pauseButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        pauseButton.setOnClickListener(view -> {
            scanner.pause();
            status("스캔을 일시정지했습니다.", UiKit.TEXT_SECONDARY);
        });
        scanActions.addView(scanButton);
        scanActions.addView(pauseButton);
        root.addView(scanActions);

        statusText = ui.text("", 13, UiKit.TEXT_SECONDARY, Typeface.NORMAL);
        root.addView(statusText);
        renderKeyStatus(root);
        renderManualInput(root);

        TextView listTitle = ui.text("저장된 책", 18, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, ui.dp(18), 0, ui.dp(6));
        listTitle.setLayoutParams(titleParams);
        root.addView(listTitle);
        LinearLayout bookList = ui.column(8);
        root.addView(bookList);
        bookListRenderer = new BookListRenderer(ui, bookList);
        return scroll;
    }

    private void renderKeyStatus(LinearLayout root) {
        String message = client.hasDomesticKey()
                ? "국내 API 키가 설정되어 국내 서지 조회를 먼저 사용합니다."
                : "국내 API 키가 비어 있어 Google Books fallback으로 조회합니다.";
        int color = client.hasDomesticKey() ? UiKit.ACCENT_PRIMARY : UiKit.STATUS_WARNING;
        root.addView(ui.text(message, 13, color, Typeface.BOLD));
    }

    private void renderManualInput(LinearLayout root) {
        LinearLayout panel = ui.column(8);
        panel.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        panel.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, ui.dp(14), 0, 0);
        panel.setLayoutParams(params);

        isbnInput = ui.input("ISBN 직접 입력");
        categoryInput = ui.input("카테고리 직접 지정(선택)");
        Button lookupButton = ui.button("조회 후 저장");
        lookupButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        lookupButton.setOnClickListener(view -> lookup(IsbnUtils.normalize(isbnInput.getText().toString())));
        panel.addView(isbnInput);
        panel.addView(categoryInput);
        panel.addView(lookupButton);
        root.addView(panel);
    }

    private void startScanner() {
        if (!hasCameraPermission()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        scanner.start();
        status("스캔 중입니다. ISBN 바코드를 프레임 안에 맞춰주세요.", UiKit.ACCENT_PRIMARY);
    }

    private void lookup(String isbn) {
        if (!IsbnUtils.isValid(isbn)) {
            status("ISBN 10자리 또는 13자리를 입력하세요.", UiKit.STATUS_ERROR);
            return;
        }
        status("서지정보 조회 중: " + isbn, UiKit.TEXT_SECONDARY);
        client.lookup(isbn, new BibliographyClient.Callback() {
            @Override
            public void onFound(BookLookupResult result) {
                runOnUiThread(() -> saveResult(result));
            }

            @Override
            public void onMissing(String message) {
                runOnUiThread(() -> status(message, UiKit.STATUS_ERROR));
            }
        });
    }

    private void saveResult(BookLookupResult result) {
        String manualCategory = categoryInput.getText().toString().trim();
        Book book = result.book;
        if (!manualCategory.isEmpty()) {
            book = new Book(
                    book.isbn,
                    book.title,
                    book.authors,
                    book.publisher,
                    book.publishedDate,
                    manualCategory,
                    book.source);
        }
        repository.save(book);
        isbnInput.setText("");
        categoryInput.setText("");
        renderBooks();
        String fallbackNote = result.fallbackUsed ? " Google Books fallback 사용." : "";
        status("저장 완료: " + book.title + "." + fallbackNote, UiKit.ACCENT_PRIMARY);
    }

    private void renderBooks() {
        if (bookListRenderer == null) {
            return;
        }
        bookListRenderer.render(repository.listAll());
    }

    private void status(String message, int color) {
        if (statusText == null) {
            return;
        }
        statusText.setText(message);
        statusText.setTextColor(color);
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }
}
