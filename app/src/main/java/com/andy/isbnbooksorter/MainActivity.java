package com.andy.isbnbooksorter;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends ComponentActivity {
    private BookRepository repository;
    private BibliographyClient client;
    private ScannerController scanner;
    private UiKit ui;
    private BookListRenderer bookListRenderer;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> csvExportLauncher;
    private ScrollView pageScroll;
    private LinearLayout menuPanel;
    private View isbnSearchSection;
    private View librarySection;
    private TextView statusText;
    private EditText isbnInput;
    private EditText categoryInput;
    private EditText savedSearchInput;
    private EditText savedCategoryFilterInput;
    private Spinner sortSpinner;
    private BookListQuery.Sort currentSort = BookListQuery.Sort.SAVED_NEWEST;
    private final List<Book> currentVisibleBooks = new ArrayList<>();

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
        csvExportLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/csv"),
                uri -> {
                    if (uri != null) {
                        exportVisibleBooks(uri);
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
        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setBackgroundColor(UiKit.SURFACE_PRIMARY);
        LinearLayout root = ui.column(20);
        root.setPadding(ui.dp(16), ui.dp(18), ui.dp(16), ui.dp(24));
        pageScroll.addView(root);

        LinearLayout header = ui.row(12);
        TextView title = ui.text(CatalogUiContract.APP_TITLE, 24, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button menuButton = ui.button(CatalogUiContract.MENU_TOGGLE);
        menuButton.setMinWidth(ui.dp(104));
        menuButton.setOnClickListener(view -> toggleMenu());
        header.addView(title);
        header.addView(menuButton);
        root.addView(header);
        root.addView(ui.text(CatalogUiContract.APP_SUBTITLE, 14, UiKit.TEXT_SECONDARY, Typeface.NORMAL));
        renderMenu(root);

        isbnSearchSection = ui.column(12);
        root.addView(isbnSearchSection);
        ((LinearLayout) isbnSearchSection).addView(
                ui.text(CatalogUiContract.ISBN_SEARCH_TITLE, 18, UiKit.TEXT_PRIMARY, Typeface.BOLD));

        PreviewView preview = new PreviewView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ui.dp(260));
        previewParams.setMargins(0, ui.dp(16), 0, ui.dp(12));
        preview.setLayoutParams(previewParams);
        preview.setBackgroundColor(UiKit.SCANNER_SURFACE);
        ((LinearLayout) isbnSearchSection).addView(preview);

        scanner = new ScannerController(this, preview, new ScannerController.Listener() {
            @Override
            public void onIsbnDetected(String isbn) {
                runOnUiThread(() -> lookup(isbn, textFrom(categoryInput)));
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
        ((LinearLayout) isbnSearchSection).addView(scanActions);

        statusText = ui.text("", 13, UiKit.TEXT_SECONDARY, Typeface.NORMAL);
        statusText.setMinHeight(ui.dp(40));
        statusText.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8));
        statusText.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        ((LinearLayout) isbnSearchSection).addView(statusText);
        renderKeyStatus((LinearLayout) isbnSearchSection);
        renderManualInput((LinearLayout) isbnSearchSection);

        librarySection = ui.column(12);
        root.addView(librarySection);
        TextView listTitle = ui.text(CatalogUiContract.LIBRARY_TITLE, 18, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, ui.dp(18), 0, ui.dp(6));
        listTitle.setLayoutParams(titleParams);
        ((LinearLayout) librarySection).addView(listTitle);
        ((LinearLayout) librarySection).addView(
                ui.text(CatalogUiContract.SAVED_BOOKS_TITLE, 13, UiKit.TEXT_SECONDARY, Typeface.BOLD));
        renderSavedBookControls((LinearLayout) librarySection);
        LinearLayout bookList = ui.column(8);
        ((LinearLayout) librarySection).addView(bookList);
        bookListRenderer = new BookListRenderer(ui, bookList);
        return pageScroll;
    }

    private void renderMenu(LinearLayout root) {
        menuPanel = ui.column(8);
        menuPanel.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        menuPanel.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        menuPanel.setVisibility(View.GONE);

        Button isbnSearchButton = ui.button(CatalogUiContract.MENU_ISBN_SEARCH);
        isbnSearchButton.setOnClickListener(view -> {
            toggleMenu();
            scrollToSection(isbnSearchSection);
            if (isbnInput != null) {
                isbnInput.requestFocus();
            }
        });
        Button libraryButton = ui.button(CatalogUiContract.MENU_LIBRARY);
        libraryButton.setOnClickListener(view -> {
            toggleMenu();
            scrollToSection(librarySection);
            if (savedSearchInput != null) {
                savedSearchInput.requestFocus();
            }
        });
        menuPanel.addView(isbnSearchButton);
        menuPanel.addView(libraryButton);
        root.addView(menuPanel);
    }

    private void toggleMenu() {
        if (menuPanel == null) {
            return;
        }
        int nextVisibility = menuPanel.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
        menuPanel.setVisibility(nextVisibility);
    }

    private void scrollToSection(View section) {
        if (pageScroll == null || section == null) {
            return;
        }
        pageScroll.post(() -> pageScroll.smoothScrollTo(0, section.getTop()));
    }

    private void renderKeyStatus(LinearLayout root) {
        String message = client.hasDomesticKey()
                ? "국내 API 키가 설정되어 국내 서지 조회를 먼저 사용합니다."
                : "국내 API 키가 비어 있어 Google Books와 Open Library로 대체 조회합니다.";
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

        isbnInput = ui.input(CatalogUiContract.ISBN_INPUT_HINT);
        categoryInput = ui.input(CatalogUiContract.CATEGORY_INPUT_HINT);
        Button lookupButton = ui.button(CatalogUiContract.LOOKUP_AND_SAVE);
        lookupButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        lookupButton.setOnClickListener(view -> lookupManualInput());
        panel.addView(isbnInput);
        panel.addView(categoryInput);
        panel.addView(lookupButton);
        root.addView(panel);
    }

    private void renderSavedBookControls(LinearLayout root) {
        LinearLayout panel = ui.column(8);
        panel.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        panel.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, ui.dp(12));
        panel.setLayoutParams(params);

        savedSearchInput = ui.input(CatalogUiContract.SAVED_SEARCH_HINT);
        savedCategoryFilterInput = ui.input(CatalogUiContract.SAVED_CATEGORY_FILTER_HINT);
        sortSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CatalogUiContract.SORT_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setMinimumHeight(ui.dp(48));

        LinearLayout actions = ui.row(8);
        Button applyButton = ui.button(CatalogUiContract.APPLY_FILTERS);
        applyButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        applyButton.setOnClickListener(view -> {
            currentSort = sortFromPosition(sortSpinner.getSelectedItemPosition());
            renderBooks();
        });
        Button clearButton = ui.button(CatalogUiContract.CLEAR_FILTERS);
        clearButton.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        clearButton.setOnClickListener(view -> {
            savedSearchInput.setText("");
            savedCategoryFilterInput.setText("");
            sortSpinner.setSelection(0);
            currentSort = BookListQuery.Sort.SAVED_NEWEST;
            renderBooks();
        });
        actions.addView(applyButton);
        actions.addView(clearButton);

        Button exportButton = ui.button(CatalogUiContract.EXPORT_VISIBLE_CSV);
        exportButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        exportButton.setOnClickListener(view -> startCsvExport());

        panel.addView(savedSearchInput);
        panel.addView(savedCategoryFilterInput);
        panel.addView(sortSpinner);
        panel.addView(actions);
        panel.addView(exportButton);
        root.addView(panel);
    }

    private void startScanner() {
        if (!hasCameraPermission()) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            return;
        }
        if (scanner.start()) {
            status("스캔 중입니다. ISBN 바코드를 프레임 안에 맞춰주세요.", UiKit.ACCENT_PRIMARY);
        }
    }

    private void lookup(String isbn, String categoryOverride) {
        if (!IsbnUtils.isValid(isbn)) {
            status("ISBN 형식에 맞지 않습니다. 숫자 10자리 또는 13자리를 입력하세요.", UiKit.STATUS_ERROR);
            return;
        }
        status("찾고 있습니다: " + isbn, UiKit.TEXT_SECONDARY);
        client.lookup(isbn, new BibliographyClient.Callback() {
            @Override
            public void onFound(BookLookupResult result) {
                runOnUiThread(() -> saveResult(result, categoryOverride));
            }

            @Override
            public void onMissing(String message) {
                runOnUiThread(() -> status(message, UiKit.STATUS_ERROR));
            }
        });
    }

    private void lookupManualInput() {
        String rawValue = isbnInput.getText().toString().trim();
        if (rawValue.isEmpty()) {
            status("ISBN을 입력하세요.", UiKit.STATUS_ERROR);
            return;
        }
        String isbn = IsbnUtils.normalize(rawValue);
        if (!IsbnUtils.isValid(isbn)) {
            status("ISBN 형식에 맞지 않습니다. 숫자 10자리 또는 13자리를 입력하세요.", UiKit.STATUS_ERROR);
            return;
        }
        lookup(isbn, textFrom(categoryInput));
    }

    private void saveResult(BookLookupResult result, String categoryOverride) {
        String manualCategory = categoryOverride.trim();
        Book book = result.book;
        if (!manualCategory.isEmpty()) {
            book = book.withCategory(manualCategory);
        }
        repository.save(book);
        isbnInput.setText("");
        categoryInput.setText("");
        renderBooks();
        String fallbackNote = result.fallbackUsed ? " (" + book.source + " 대체 조회)" : "";
        status("저장 완료: " + book.title + fallbackNote, UiKit.ACCENT_PRIMARY);
    }

    private void renderBooks() {
        if (bookListRenderer == null) {
            return;
        }
        currentSort = sortFromPosition(sortSpinner == null ? 0 : sortSpinner.getSelectedItemPosition());
        List<Book> allBooks = repository.listAll();
        BookListQuery.Options options = new BookListQuery.Options(
                textFrom(savedSearchInput),
                textFrom(savedCategoryFilterInput),
                currentSort);
        List<Book> visibleBooks = BookListQuery.apply(allBooks, options);
        currentVisibleBooks.clear();
        currentVisibleBooks.addAll(visibleBooks);
        boolean hasActiveFilter = !options.search.isEmpty() || !options.categoryFilter.isEmpty();
        String emptyMessage = allBooks.isEmpty()
                ? CatalogUiContract.EMPTY_LIBRARY_MESSAGE
                : CatalogUiContract.EMPTY_FILTERED_MESSAGE;
        bookListRenderer.render(visibleBooks, emptyMessage, currentSort == BookListQuery.Sort.CATEGORY && !hasActiveFilter);
    }

    private void status(String message, int color) {
        if (statusText == null) {
            return;
        }
        statusText.setText(message);
        statusText.setTextColor(color);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCsvExport() {
        if (currentVisibleBooks.isEmpty()) {
            status("내보낼 현재 목록이 없습니다.", UiKit.STATUS_WARNING);
            return;
        }
        String fileName = "isbn-books-" + timestampForFileName() + ".csv";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportVisibleBooksToDownloads(fileName);
            return;
        }
        csvExportLauncher.launch(fileName);
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private void exportVisibleBooksToDownloads(String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            status("CSV 파일을 만들 수 없습니다.", UiKit.STATUS_ERROR);
            return;
        }
        exportVisibleBooks(uri, "Downloads/" + fileName);
    }

    private void exportVisibleBooks(Uri uri) {
        exportVisibleBooks(uri, null);
    }

    private void exportVisibleBooks(Uri uri, String savedLocation) {
        String csv = CsvExporter.exportBooks(currentVisibleBooks);
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) {
                status("CSV 파일을 열 수 없습니다.", UiKit.STATUS_ERROR);
                return;
            }
            stream.write(csv.getBytes(StandardCharsets.UTF_8));
            String locationNote = savedLocation == null ? "" : " (" + savedLocation + ")";
            status("현재 목록 " + currentVisibleBooks.size() + "권을 CSV로 내보냈습니다." + locationNote, UiKit.ACCENT_PRIMARY);
        } catch (Exception exception) {
            status("CSV 내보내기에 실패했습니다.", UiKit.STATUS_ERROR);
        }
    }

    private static String textFrom(EditText input) {
        if (input == null) {
            return "";
        }
        return input.getText().toString().trim();
    }

    private static BookListQuery.Sort sortFromPosition(int position) {
        switch (position) {
            case 1:
                return BookListQuery.Sort.TITLE;
            case 2:
                return BookListQuery.Sort.AUTHOR;
            case 3:
                return BookListQuery.Sort.CATEGORY;
            case 0:
            default:
                return BookListQuery.Sort.SAVED_NEWEST;
        }
    }

    private static String timestampForFileName() {
        return new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date());
    }
}
