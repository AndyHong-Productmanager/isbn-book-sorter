package com.andy.isbnbooksorter;

final class Book {
    final String isbn;
    final String title;
    final String authors;
    final String publisher;
    final String publishedDate;
    final String category;
    final String source;

    Book(
            String isbn,
            String title,
            String authors,
            String publisher,
            String publishedDate,
            String category,
            String source) {
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.category = category;
        this.source = source;
    }
}
