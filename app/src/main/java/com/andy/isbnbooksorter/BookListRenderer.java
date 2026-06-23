package com.andy.isbnbooksorter;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class BookListRenderer {
    private static final SimpleDateFormat SAVED_AT_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA);

    private final UiKit ui;
    private final LinearLayout target;

    BookListRenderer(UiKit ui, LinearLayout target) {
        this.ui = ui;
        this.target = target;
    }

    void render(List<Book> books) {
        render(books, "아직 저장된 책이 없습니다. ISBN을 스캔하거나 직접 입력하세요.", true);
    }

    void render(List<Book> books, String emptyMessage, boolean groupByCategory) {
        target.removeAllViews();
        if (books.isEmpty()) {
            target.addView(ui.text(
                    emptyMessage,
                    14,
                    UiKit.TEXT_SECONDARY,
                    Typeface.NORMAL));
            return;
        }
        if (!groupByCategory) {
            for (Book book : books) {
                target.addView(bookRow(book));
            }
            return;
        }
        String currentCategory = "";
        LinearLayout currentGroup = null;
        for (Book book : books) {
            if (!book.category.equals(currentCategory)) {
                currentCategory = book.category;
                target.addView(ui.text(
                        currentCategory + " · " + countBooksInCategory(books, currentCategory) + "권",
                        18,
                        UiKit.TEXT_PRIMARY,
                        Typeface.BOLD));
                currentGroup = ui.column(0);
                target.addView(currentGroup);
            }
            currentGroup.addView(bookRow(book));
        }
    }

    private View bookRow(Book book) {
        LinearLayout row = ui.column(4);
        row.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
        row.setBackgroundColor(UiKit.SURFACE_SECONDARY);
        row.addView(ui.text(book.title, 16, UiKit.TEXT_PRIMARY, Typeface.BOLD));
        row.addView(ui.text("저자: " + display(book.authors), 13, UiKit.TEXT_SECONDARY, Typeface.NORMAL));
        row.addView(ui.text(
                "출판: " + display(book.publisher) + " · " + display(book.publishedDate),
                13,
                UiKit.TEXT_SECONDARY,
                Typeface.NORMAL));
        row.addView(ui.text("분류: " + display(book.category), 13, UiKit.TEXT_SECONDARY, Typeface.NORMAL));
        if (!book.description.isEmpty()) {
            row.addView(ui.text("설명: " + compact(book.description), 13, UiKit.TEXT_SECONDARY, Typeface.NORMAL));
        }
        if (book.pageCount > 0 || !book.thumbnailUrl.isEmpty()) {
            row.addView(ui.text(
                    "페이지: " + (book.pageCount > 0 ? book.pageCount : "정보 없음")
                            + " · 표지: " + display(book.thumbnailUrl),
                    12,
                    UiKit.TEXT_SECONDARY,
                    Typeface.NORMAL));
        }
        TextView isbn = ui.text(
                "ISBN " + book.isbn + " · 출처 " + display(book.source) + " · 저장 " + formatSavedAt(book.savedAt),
                12,
                UiKit.TEXT_SECONDARY,
                Typeface.NORMAL);
        isbn.setTypeface(Typeface.MONOSPACE);
        row.addView(isbn);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, ui.dp(8));
        row.setLayoutParams(params);
        return row;
    }

    private static String display(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "정보 없음";
        }
        return value.trim();
    }

    private static String compact(String value) {
        String compacted = value.replace('\r', ' ').replace('\n', ' ').trim();
        if (compacted.length() <= 160) {
            return compacted;
        }
        return compacted.substring(0, 157) + "...";
    }

    private static String formatSavedAt(long savedAt) {
        if (savedAt <= 0L) {
            return "정보 없음";
        }
        return SAVED_AT_FORMAT.format(new Date(savedAt));
    }

    private static int countBooksInCategory(List<Book> books, String category) {
        int count = 0;
        for (Book book : books) {
            if (book.category.equals(category)) {
                count += 1;
            }
        }
        return count;
    }
}
