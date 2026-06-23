package com.andy.isbnbooksorter;

final class Book {
    final String isbn;
    final String title;
    final String authors;
    final String publisher;
    final String publishedDate;
    final String category;
    final String source;
    final String description;
    final int pageCount;
    final String thumbnailUrl;
    final long savedAt;

    Book(
            String isbn,
            String title,
            String authors,
            String publisher,
            String publishedDate,
            String category,
            String source) {
        this(isbn, title, authors, publisher, publishedDate, category, source, "", 0, "", 0L);
    }

    Book(
            String isbn,
            String title,
            String authors,
            String publisher,
            String publishedDate,
            String category,
            String source,
            String description,
            int pageCount,
            String thumbnailUrl,
            long savedAt) {
        this.isbn = clean(isbn);
        this.title = defaultValue(title, "제목 없음");
        this.authors = clean(authors);
        this.publisher = clean(publisher);
        this.publishedDate = clean(publishedDate);
        this.category = defaultValue(category, "미분류");
        this.source = clean(source);
        this.description = clean(description);
        this.pageCount = Math.max(0, pageCount);
        this.thumbnailUrl = clean(thumbnailUrl);
        this.savedAt = Math.max(0L, savedAt);
    }

    Book withCategory(String category) {
        return new Book(
                isbn,
                title,
                authors,
                publisher,
                publishedDate,
                category,
                source,
                description,
                pageCount,
                thumbnailUrl,
                savedAt);
    }

    Book withSavedAt(long savedAt) {
        return new Book(
                isbn,
                title,
                authors,
                publisher,
                publishedDate,
                category,
                source,
                description,
                pageCount,
                thumbnailUrl,
                savedAt);
    }

    private static String defaultValue(String value, String fallback) {
        String cleaned = clean(value);
        if (cleaned.isEmpty()) {
            return fallback;
        }
        return cleaned;
    }

    private static String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }
}
