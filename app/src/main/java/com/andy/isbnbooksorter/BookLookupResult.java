package com.andy.isbnbooksorter;

final class BookLookupResult {
    final Book book;
    final boolean fallbackUsed;

    BookLookupResult(Book book, boolean fallbackUsed) {
        this.book = book;
        this.fallbackUsed = fallbackUsed;
    }
}
