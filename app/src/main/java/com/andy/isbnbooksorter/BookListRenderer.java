package com.andy.isbnbooksorter;

import android.graphics.Typeface;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

final class BookListRenderer {
    private final UiKit ui;
    private final LinearLayout target;

    BookListRenderer(UiKit ui, LinearLayout target) {
        this.ui = ui;
        this.target = target;
    }

    void render(List<Book> books) {
        target.removeAllViews();
        if (books.isEmpty()) {
            target.addView(ui.text(
                    "아직 저장된 책이 없습니다. ISBN을 스캔하거나 직접 입력하세요.",
                    14,
                    UiKit.TEXT_SECONDARY,
                    Typeface.NORMAL));
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
        row.addView(ui.text(
                book.authors + " · " + book.publisher + " · " + book.publishedDate,
                13,
                UiKit.TEXT_SECONDARY,
                Typeface.NORMAL));
        TextView isbn = ui.text("ISBN " + book.isbn + " · " + book.source, 12, UiKit.TEXT_SECONDARY, Typeface.NORMAL);
        isbn.setTypeface(Typeface.MONOSPACE);
        row.addView(isbn);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, ui.dp(8));
        row.setLayoutParams(params);
        return row;
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
