package com.andy.isbnbooksorter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class BookListQuery {
    enum Sort {
        SAVED_NEWEST,
        TITLE,
        AUTHOR,
        CATEGORY
    }

    static final class Options {
        final String search;
        final String categoryFilter;
        final Sort sort;

        Options(String search, String categoryFilter, Sort sort) {
            this.search = search == null ? "" : search.trim();
            this.categoryFilter = categoryFilter == null ? "" : categoryFilter.trim();
            this.sort = sort == null ? Sort.SAVED_NEWEST : sort;
        }
    }

    private static final Collator COLLATOR = Collator.getInstance(Locale.KOREAN);

    private BookListQuery() {}

    static List<Book> apply(List<Book> books, Options options) {
        Options safeOptions = options == null ? new Options("", "", Sort.SAVED_NEWEST) : options;
        String[] tokens = lower(safeOptions.search).split("\\s+");
        String categoryFilter = lower(safeOptions.categoryFilter);
        List<Book> result = new ArrayList<>();
        for (Book book : books) {
            if (!matchesCategory(book, categoryFilter)) {
                continue;
            }
            if (!matchesSearch(book, tokens)) {
                continue;
            }
            result.add(book);
        }
        result.sort(comparatorFor(safeOptions.sort));
        return result;
    }

    private static boolean matchesCategory(Book book, String categoryFilter) {
        return categoryFilter.isEmpty() || lower(book.category).contains(categoryFilter);
    }

    private static boolean matchesSearch(Book book, String[] tokens) {
        if (tokens.length == 0 || tokens[0].isEmpty()) {
            return true;
        }
        String searchable = lower(String.join(
                " ",
                book.title,
                book.subtitle,
                book.authors,
                book.translators,
                book.publisher,
                book.isbn,
                book.category,
                book.source,
                book.description,
                book.tableOfContents,
                book.contents,
                book.introduction,
                book.language,
                book.price,
                book.bookSize,
                book.form,
                book.formDetail,
                book.seriesTitle,
                book.seriesNo,
                book.relatedIsbn,
                book.titleUrl,
                book.eaIsbn,
                book.eaAddCode,
                book.inputDate,
                book.updateDate,
                book.bibYn,
                book.depositYn,
                book.ebookYn));
        for (String token : tokens) {
            if (!token.isEmpty() && !searchable.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private static Comparator<Book> comparatorFor(Sort sort) {
        switch (sort) {
            case TITLE:
                return compareText(book -> book.title)
                        .thenComparing(compareText(book -> book.authors))
                        .thenComparing(compareText(book -> book.isbn));
            case AUTHOR:
                return compareText(book -> book.authors)
                        .thenComparing(compareText(book -> book.title))
                        .thenComparing(compareText(book -> book.isbn));
            case CATEGORY:
                return compareText(book -> book.category)
                        .thenComparing(compareText(book -> book.title))
                        .thenComparing(compareText(book -> book.isbn));
            case SAVED_NEWEST:
            default:
                return Comparator.comparingLong((Book book) -> book.savedAt)
                        .reversed()
                        .thenComparing(compareText(book -> book.title));
        }
    }

    private static Comparator<Book> compareText(ValueReader reader) {
        return (left, right) -> COLLATOR.compare(reader.read(left), reader.read(right));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private interface ValueReader {
        String read(Book book);
    }
}
