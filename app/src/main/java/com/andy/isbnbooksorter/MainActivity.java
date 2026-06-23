package com.andy.isbnbooksorter;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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
    private static final SimpleDateFormat DETAIL_SAVED_AT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);

    private enum AppPage {
        ISBN_SEARCH,
        LIBRARY,
        BOOK_DETAIL
    }

    private BookRepository repository;
    private BibliographyClient client;
    private ScannerController scanner;
    private UiKit ui;
    private BookListRenderer bookListRenderer;
    private ActivityResultLauncher<String> cameraPermissionLauncher;
    private ActivityResultLauncher<String> csvExportLauncher;
    private ScrollView pageScroll;
    private LinearLayout menuPanel;
    private TextView statusText;
    private EditText isbnInput;
    private EditText categoryInput;
    private EditText savedSearchInput;
    private EditText savedCategoryFilterInput;
    private Spinner sortSpinner;
    private BookListQuery.Sort currentSort = BookListQuery.Sort.SAVED_NEWEST;
    private AppPage currentPage = AppPage.ISBN_SEARCH;
    private Book selectedBook;
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
        showSearchPage();
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
        resetPageViews();
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

        switch (currentPage) {
            case LIBRARY:
                renderLibraryPage(root);
                break;
            case BOOK_DETAIL:
                renderDetailPage(root);
                break;
            case ISBN_SEARCH:
            default:
                renderSearchPage(root);
                break;
        }
        return pageScroll;
    }

    private void renderSearchPage(LinearLayout root) {
        LinearLayout searchPage = ui.column(12);
        root.addView(searchPage);
        searchPage.addView(ui.text(CatalogUiContract.ISBN_SEARCH_TITLE, 18, UiKit.TEXT_PRIMARY, Typeface.BOLD));

        PreviewView preview = new PreviewView(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ui.dp(260));
        previewParams.setMargins(0, ui.dp(16), 0, ui.dp(12));
        preview.setLayoutParams(previewParams);
        preview.setBackgroundColor(UiKit.SCANNER_SURFACE);
        searchPage.addView(preview);

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
        searchPage.addView(scanActions);

        statusText = ui.text("", 13, UiKit.TEXT_SECONDARY, Typeface.NORMAL);
        statusText.setMinHeight(ui.dp(40));
        statusText.setPadding(ui.dp(12), ui.dp(8), ui.dp(12), ui.dp(8));
        statusText.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        searchPage.addView(statusText);
        renderKeyStatus(searchPage);
        renderManualInput(searchPage);
    }

    private void renderLibraryPage(LinearLayout root) {
        LinearLayout libraryPage = ui.column(12);
        root.addView(libraryPage);
        TextView listTitle = ui.text(CatalogUiContract.LIBRARY_TITLE, 18, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, ui.dp(18), 0, ui.dp(6));
        listTitle.setLayoutParams(titleParams);
        libraryPage.addView(listTitle);
        libraryPage.addView(ui.text(CatalogUiContract.SAVED_BOOKS_TITLE, 13, UiKit.TEXT_SECONDARY, Typeface.BOLD));
        renderSavedBookControls(libraryPage);
        LinearLayout bookList = ui.column(8);
        libraryPage.addView(bookList);
        bookListRenderer = new BookListRenderer(ui, bookList, this::showBookDetails);
        renderBooks();
    }

    private void renderDetailPage(LinearLayout root) {
        Book book = selectedBook;
        if (book == null) {
            renderLibraryPage(root);
            return;
        }

        Button backButton = ui.button(CatalogUiContract.BACK_TO_LIBRARY);
        backButton.setOnClickListener(view -> showLibraryPage());
        root.addView(backButton);
        root.addView(ui.text(CatalogUiContract.BOOK_DETAIL_TITLE, 20, UiKit.TEXT_PRIMARY, Typeface.BOLD));

        LinearLayout content = ui.column(8);
        content.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(24));
        content.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        root.addView(content);
        addDetail(content, "제목", book.title);
        addDetail(content, "부제", book.subtitle);
        addDetail(content, "저자", book.authors);
        addDetail(content, "번역", book.translators);
        addDetail(content, "출판사", book.publisher);
        addDetail(content, "출판일", book.publishedDate);
        addDetail(content, "카테고리", book.category);
        addDetail(content, "ISBN", book.isbn);
        addDetail(content, "출처", book.source);
        addDetail(content, "언어", book.language);
        addDetail(content, "가격", book.price);
        addDetail(content, "페이지", book.pageCount > 0 ? String.valueOf(book.pageCount) : "");
        addDetail(content, "표지 URL", book.thumbnailUrl);
        addDetail(content, "저장일", formatDetailSavedAt(book.savedAt));
        addDetail(content, "설명", book.description);
        addDetail(content, "소개", book.introduction);
        addDetail(content, "내용", book.contents);
        addDetail(content, "목차", book.tableOfContents);

        Button bottomBackButton = ui.button(CatalogUiContract.BACK_TO_LIBRARY);
        bottomBackButton.setOnClickListener(view -> showLibraryPage());
        root.addView(bottomBackButton);
    }

    private void resetPageViews() {
        pageScroll = null;
        menuPanel = null;
        statusText = null;
        isbnInput = null;
        categoryInput = null;
        savedSearchInput = null;
        savedCategoryFilterInput = null;
        sortSpinner = null;
        bookListRenderer = null;
    }

    private void renderMenu(LinearLayout root) {
        menuPanel = ui.column(8);
        menuPanel.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        menuPanel.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        menuPanel.setVisibility(View.GONE);

        Button isbnSearchButton = ui.button(CatalogUiContract.MENU_ISBN_SEARCH);
        isbnSearchButton.setOnClickListener(view -> {
            toggleMenu();
            showSearchPage();
        });
        Button libraryButton = ui.button(CatalogUiContract.MENU_LIBRARY);
        libraryButton.setOnClickListener(view -> {
            toggleMenu();
            showLibraryPage();
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

    private void showSearchPage() {
        shutdownScanner();
        currentPage = AppPage.ISBN_SEARCH;
        selectedBook = null;
        setContentView(createContent());
    }

    private void showLibraryPage() {
        shutdownScanner();
        currentPage = AppPage.LIBRARY;
        selectedBook = null;
        setContentView(createContent());
    }

    private void showDetailPage(Book book) {
        shutdownScanner();
        currentPage = AppPage.BOOK_DETAIL;
        selectedBook = book;
        setContentView(createContent());
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
        isbnInput.clearFocus();
        categoryInput.clearFocus();
        hideKeyboard();
        showLibraryPage();
        String fallbackNote = result.fallbackUsed ? " (" + book.source + " 대체 조회)" : "";
        status("저장 완료: " + book.title + fallbackNote, UiKit.ACCENT_PRIMARY);
    }

    private void showBookDetails(Book book) {
        showDetailPage(book);
    }

    private void addDetail(LinearLayout content, String label, String value) {
        TextView labelView = ui.text(label, 12, UiKit.TEXT_SECONDARY, Typeface.BOLD);
        TextView valueView = ui.text(display(value), 15, UiKit.TEXT_PRIMARY, Typeface.NORMAL);
        valueView.setLineSpacing(ui.dp(2), 1.0f);
        if ("ISBN".equals(label)) {
            valueView.setTypeface(Typeface.MONOSPACE);
        }
        content.addView(labelView);
        content.addView(valueView);
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
        if (statusText != null) {
            statusText.setText(message);
            statusText.setTextColor(color);
        }
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

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused == null) {
            return;
        }
        InputMethodManager manager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void shutdownScanner() {
        if (scanner == null) {
            return;
        }
        scanner.shutdown();
        scanner = null;
    }

    private static String display(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "정보 없음";
        }
        return value.trim();
    }

    private static String formatDetailSavedAt(long savedAt) {
        if (savedAt <= 0L) {
            return "";
        }
        return DETAIL_SAVED_AT_FORMAT.format(new Date(savedAt));
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
