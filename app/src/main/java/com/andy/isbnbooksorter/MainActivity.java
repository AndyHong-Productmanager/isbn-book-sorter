package com.andy.isbnbooksorter;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import java.io.OutputStream;
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

    private static final class PageState {
        final AppPage page;
        final Book book;

        PageState(AppPage page, Book book) {
            this.page = page;
            this.book = book;
        }
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
    private PopupWindow menuPopup;
    private TextView statusText;
    private View scannerPausedOverlay;
    private EditText isbnInput;
    private EditText categoryInput;
    private EditText savedSearchInput;
    private Spinner categoryFilterSpinner;
    private Spinner sortSpinner;
    private BookListQuery.Sort currentSort = BookListQuery.Sort.SAVED_NEWEST;
    private AppPage currentPage = AppPage.ISBN_SEARCH;
    private Book selectedBook;
    private final List<Book> currentVisibleBooks = new ArrayList<>();
    private final List<PageState> pageHistory = new ArrayList<>();

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
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed();
            }
        });
        navigateTo(AppPage.ISBN_SEARCH, null, false);
        if (hasCameraPermission()) {
            status("카메라 준비 완료. ISBN 바코드를 화면에 맞춰주세요.", UiKit.TEXT_SECONDARY);
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    protected void onDestroy() {
        dismissMenu();
        if (scanner != null) {
            scanner.shutdown();
        }
        client.shutdown();
        repository.close();
        super.onDestroy();
    }

    private View createContent() {
        resetPageViews();
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(UiKit.SURFACE_PRIMARY);
        pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageScroll.setBackgroundColor(UiKit.SURFACE_PRIMARY);
        screen.addView(pageScroll, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        LinearLayout root = ui.column(12);
        root.setPadding(ui.dp(16), headerTopPadding(), ui.dp(16), pageBottomPadding());
        pageScroll.addView(root);

        LinearLayout header = ui.row(12);
        Button menuButton = ui.iconButton("☰", CatalogUiContract.MENU_TOGGLE);
        menuButton.setOnClickListener(this::toggleMenu);
        TextView title = ui.text(CatalogUiContract.APP_TITLE, 24, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        header.addView(menuButton);
        header.addView(title);
        root.addView(header);
        if (currentPage == AppPage.ISBN_SEARCH) {
            root.addView(ui.text(CatalogUiContract.APP_SUBTITLE, 14, UiKit.TEXT_SECONDARY, Typeface.NORMAL));
        }

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
        screen.addView(createBottomMenu(), bottomMenuParams());
        return screen;
    }

    private int headerTopPadding() {
        int statusBarId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        int statusBarHeight = statusBarId > 0 ? getResources().getDimensionPixelSize(statusBarId) : 0;
        return statusBarHeight + ui.dp(12);
    }

    private int pageBottomPadding() {
        return bottomMenuHeight() + ui.dp(20);
    }

    private int navigationBarHeight() {
        int navigationBarId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        return navigationBarId > 0 ? getResources().getDimensionPixelSize(navigationBarId) : 0;
    }

    private int bottomMenuHeight() {
        return ui.dp(72) + navigationBarHeight();
    }

    private FrameLayout.LayoutParams bottomMenuParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                bottomMenuHeight());
        params.gravity = android.view.Gravity.BOTTOM;
        return params;
    }

    private View createBottomMenu() {
        LinearLayout menu = ui.row(8);
        menu.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), navigationBarHeight() + ui.dp(10));
        menu.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        Button searchButton = bottomMenuButton(CatalogUiContract.MENU_ISBN_SEARCH, currentPage == AppPage.ISBN_SEARCH);
        searchButton.setOnClickListener(view -> showSearchPage());
        Button libraryButton = bottomMenuButton(CatalogUiContract.MENU_LIBRARY, currentPage == AppPage.LIBRARY);
        libraryButton.setOnClickListener(view -> showLibraryPage());
        menu.addView(searchButton);
        menu.addView(libraryButton);
        return menu;
    }

    private Button bottomMenuButton(String label, boolean active) {
        Button button = active ? ui.button(label) : ui.secondaryButton(label);
        button.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
        return button;
    }

    private void renderSearchPage(LinearLayout root) {
        LinearLayout searchPage = ui.column(12);
        root.addView(searchPage);
        searchPage.addView(ui.text(CatalogUiContract.ISBN_SEARCH_TITLE, 18, UiKit.TEXT_PRIMARY, Typeface.BOLD));

        FrameLayout previewFrame = new FrameLayout(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                ui.dp(260));
        previewParams.setMargins(0, ui.dp(16), 0, ui.dp(12));
        previewFrame.setLayoutParams(previewParams);
        previewFrame.setBackgroundColor(android.graphics.Color.BLACK);

        PreviewView preview = new PreviewView(this);
        preview.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        preview.setBackgroundColor(UiKit.SCANNER_SURFACE);
        previewFrame.addView(preview);

        scannerPausedOverlay = new View(this);
        scannerPausedOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        scannerPausedOverlay.setBackgroundColor(android.graphics.Color.BLACK);
        scannerPausedOverlay.setVisibility(View.VISIBLE);
        previewFrame.addView(scannerPausedOverlay);
        searchPage.addView(previewFrame);

        scanner = new ScannerController(this, preview, new ScannerController.Listener() {
            @Override
            public void onIsbnDetected(String isbn) {
                runOnUiThread(() -> {
                    showScannerPausedFrame();
                    lookup(isbn, textFrom(categoryInput));
                });
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
            showScannerPausedFrame();
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
        LinearLayout libraryPage = ui.column(8);
        root.addView(libraryPage);
        TextView listTitle = ui.text(CatalogUiContract.LIBRARY_TITLE, 18, UiKit.TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, ui.dp(8), 0, ui.dp(2));
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

        root.addView(ui.text(CatalogUiContract.BOOK_DETAIL_TITLE, 20, UiKit.TEXT_PRIMARY, Typeface.BOLD));

        Button backButton = ui.secondaryButton(CatalogUiContract.BACK_TO_LIBRARY);
        backButton.setOnClickListener(view -> showLibraryPage());
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        backParams.setMargins(0, 0, 0, ui.dp(8));
        backButton.setLayoutParams(backParams);
        root.addView(backButton);

        LinearLayout content = ui.column(6);
        content.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(16));
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
        addDetail(content, "형태", book.form);
        addDetail(content, "상세 형태", book.formDetail);
        addDetail(content, "크기", book.bookSize);
        addDetail(content, "총서명", book.seriesTitle);
        addDetail(content, "총서 번호", book.seriesNo);
        addDetail(content, "관련 ISBN", book.relatedIsbn);
        addDetail(content, "서명 URL", book.titleUrl);
        addDetail(content, "발행자 ISBN", book.eaIsbn);
        addDetail(content, "부가기호", book.eaAddCode);
        addDetail(content, "입력일", book.inputDate);
        addDetail(content, "수정일", book.updateDate);
        addDetail(content, "서지 여부", book.bibYn);
        addDetail(content, "납본 여부", book.depositYn);
        addDetail(content, "전자책 여부", book.ebookYn);
        addDetail(content, "페이지", book.pageCount > 0 ? String.valueOf(book.pageCount) : "");
        addDetail(content, "표지 URL", book.thumbnailUrl);
        addDetail(content, "저장일", formatDetailSavedAt(book.savedAt));
        addDetail(content, "설명", book.description);
        addDetail(content, "소개", book.introduction);
        addDetail(content, "내용", book.contents);
        addDetail(content, "목차", book.tableOfContents);
    }

    private void resetPageViews() {
        dismissMenu();
        pageScroll = null;
        menuPanel = null;
        statusText = null;
        scannerPausedOverlay = null;
        isbnInput = null;
        categoryInput = null;
        savedSearchInput = null;
        categoryFilterSpinner = null;
        sortSpinner = null;
        bookListRenderer = null;
    }

    private LinearLayout createMenuPanel() {
        menuPanel = ui.column(8);
        menuPanel.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
        menuPanel.setBackgroundColor(UiKit.SURFACE_SECONDARY);

        Button isbnSearchButton = ui.button(CatalogUiContract.MENU_ISBN_SEARCH);
        isbnSearchButton.setOnClickListener(view -> {
            dismissMenu();
            showSearchPage();
        });
        Button libraryButton = ui.button(CatalogUiContract.MENU_LIBRARY);
        libraryButton.setOnClickListener(view -> {
            dismissMenu();
            showLibraryPage();
        });
        menuPanel.addView(isbnSearchButton);
        menuPanel.addView(libraryButton);
        return menuPanel;
    }

    private void toggleMenu(View anchor) {
        if (menuPopup != null && menuPopup.isShowing()) {
            dismissMenu();
            return;
        }
        LinearLayout panel = createMenuPanel();
        menuPopup = new PopupWindow(
                panel,
                ui.dp(196),
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        menuPopup.setOutsideTouchable(true);
        menuPopup.setBackgroundDrawable(new ColorDrawable(UiKit.SURFACE_SECONDARY));
        menuPopup.showAsDropDown(anchor, 0, ui.dp(4));
    }

    private void dismissMenu() {
        if (menuPopup != null) {
            menuPopup.dismiss();
            menuPopup = null;
        }
    }

    private void showSearchPage() {
        navigateTo(AppPage.ISBN_SEARCH, null, true);
    }

    private void showLibraryPage() {
        navigateTo(AppPage.LIBRARY, null, true);
    }

    private void showDetailPage(Book book) {
        navigateTo(AppPage.BOOK_DETAIL, book, true);
    }

    private void navigateTo(AppPage page, Book book, boolean recordHistory) {
        if (recordHistory && shouldRecordHistory(page, book)) {
            pageHistory.add(new PageState(currentPage, selectedBook));
        }
        shutdownScanner();
        currentPage = page;
        selectedBook = book;
        setContentView(createContent());
    }

    private boolean shouldRecordHistory(AppPage nextPage, Book nextBook) {
        if (currentPage != nextPage) {
            return true;
        }
        if (currentPage != AppPage.BOOK_DETAIL) {
            return false;
        }
        return selectedBook == null || nextBook == null || !selectedBook.isbn.equals(nextBook.isbn);
    }

    private void handleBackPressed() {
        if (menuPopup != null && menuPopup.isShowing()) {
            dismissMenu();
            return;
        }
        if (!pageHistory.isEmpty()) {
            PageState previous = pageHistory.remove(pageHistory.size() - 1);
            navigateTo(previous.page, previous.book, false);
            return;
        }
        if (currentPage != AppPage.ISBN_SEARCH) {
            navigateTo(AppPage.ISBN_SEARCH, null, false);
            return;
        }
        finish();
    }

    private void renderKeyStatus(LinearLayout root) {
        String message = client.hasDomesticKey()
                ? "국내 API 키가 설정되어 국내 서지 조회를 먼저 사용합니다."
                : "국내 API 키가 비어 있어 Google Books와 Open Library로 대체 조회합니다.";
        int color = client.hasDomesticKey() ? UiKit.ACCENT_PRIMARY : UiKit.STATUS_WARNING;
        root.addView(ui.text(message, 13, color, Typeface.BOLD));
    }

    private void renderManualInput(LinearLayout root) {
        LinearLayout panel = ui.column(6);
        panel.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
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
        categoryFilterSpinner = new Spinner(this);
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categoryFilterLabels(repository.listAll()));
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categoryFilterSpinner.setAdapter(categoryAdapter);
        categoryFilterSpinner.setMinimumHeight(ui.dp(48));
        sortSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CatalogUiContract.SORT_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);
        sortSpinner.setMinimumHeight(ui.dp(48));
        savedSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                renderBooks();
            }

            @Override
            public void afterTextChanged(Editable text) {
            }
        });
        AdapterView.OnItemSelectedListener immediateFilterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                renderBooks();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };
        categoryFilterSpinner.setOnItemSelectedListener(immediateFilterListener);
        sortSpinner.setOnItemSelectedListener(immediateFilterListener);

        LinearLayout actions = ui.row(8);
        Button clearButton = ui.button(CatalogUiContract.CLEAR_FILTERS);
        clearButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        clearButton.setOnClickListener(view -> {
            savedSearchInput.setText("");
            categoryFilterSpinner.setSelection(0);
            sortSpinner.setSelection(0);
            currentSort = BookListQuery.Sort.SAVED_NEWEST;
            renderBooks();
        });
        actions.addView(clearButton);

        Button exportButton = ui.button(CatalogUiContract.EXPORT_VISIBLE_CSV);
        exportButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        exportButton.setOnClickListener(view -> startCsvExport());

        panel.addView(savedSearchInput);
        panel.addView(categoryFilterSpinner);
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
            showScannerLiveFrame();
            status("스캔 중입니다. ISBN 바코드를 프레임 안에 맞춰주세요.", UiKit.ACCENT_PRIMARY);
        }
    }

    private void showScannerLiveFrame() {
        if (scannerPausedOverlay != null) {
            scannerPausedOverlay.setVisibility(View.GONE);
        }
    }

    private void showScannerPausedFrame() {
        if (scannerPausedOverlay != null) {
            scannerPausedOverlay.setVisibility(View.VISIBLE);
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
                selectedCategoryFilter(),
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

    private List<String> categoryFilterLabels(List<Book> books) {
        List<String> labels = new ArrayList<>();
        labels.add(CatalogUiContract.ALL_CATEGORIES);
        for (Book book : books) {
            if (book.category.isEmpty() || labels.contains(book.category)) {
                continue;
            }
            labels.add(book.category);
        }
        return labels;
    }

    private String selectedCategoryFilter() {
        if (categoryFilterSpinner == null || categoryFilterSpinner.getSelectedItemPosition() <= 0) {
            return "";
        }
        Object selected = categoryFilterSpinner.getSelectedItem();
        return selected == null ? "" : selected.toString().trim();
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
        try (OutputStream stream = getContentResolver().openOutputStream(uri)) {
            if (stream == null) {
                status("CSV 파일을 열 수 없습니다.", UiKit.STATUS_ERROR);
                return;
            }
            stream.write(CsvExporter.exportBooksAsUtf8BomBytes(currentVisibleBooks));
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
