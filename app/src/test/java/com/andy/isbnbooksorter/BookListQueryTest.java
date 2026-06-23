package com.andy.isbnbooksorter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public final class BookListQueryTest {
    @Test
    public void filtersSearchAcrossStoredFieldsAndCategory() {
        List<Book> books = Arrays.asList(
                book("978-1", "Effective Java", "Joshua Bloch", "Addison", "프로그래밍", "Google Books", 30L),
                book("978-2", "토지", "박경리", "마로니에북스", "문학", "Open Library", 20L),
                book("979-3", "Clean Code", "Robert Martin", "Prentice Hall", "프로그래밍", "알라딘", 10L));

        List<Book> result = BookListQuery.apply(
                books,
                new BookListQuery.Options("open 978-2", "", BookListQuery.Sort.SAVED_NEWEST));

        assertEquals(1, result.size());
        assertEquals("토지", result.get(0).title);
    }

    @Test
    public void appliesCategoryFilterAndTitleSort() {
        List<Book> books = Arrays.asList(
                book("1", "Clean Code", "Robert Martin", "Prentice Hall", "프로그래밍", "Google Books", 10L),
                book("2", "Effective Java", "Joshua Bloch", "Addison", "프로그래밍", "Google Books", 30L),
                book("3", "토지", "박경리", "마로니에북스", "문학", "Open Library", 20L));

        List<Book> result = BookListQuery.apply(
                books,
                new BookListQuery.Options("", "프로그래밍", BookListQuery.Sort.TITLE));

        assertEquals(2, result.size());
        assertEquals("Clean Code", result.get(0).title);
        assertEquals("Effective Java", result.get(1).title);
    }

    @Test
    public void sortsByAuthorCategoryAndSavedNewest() {
        List<Book> books = Arrays.asList(
                book("1", "B", "Charlie", "P", "Z", "S", 10L),
                book("2", "C", "Alice", "P", "A", "S", 30L),
                book("3", "A", "Bob", "P", "M", "S", 20L));

        assertEquals("Alice", BookListQuery.apply(books, new BookListQuery.Options("", "", BookListQuery.Sort.AUTHOR)).get(0).authors);
        assertEquals("A", BookListQuery.apply(books, new BookListQuery.Options("", "", BookListQuery.Sort.CATEGORY)).get(0).category);
        assertEquals(30L, BookListQuery.apply(books, new BookListQuery.Options("", "", BookListQuery.Sort.SAVED_NEWEST)).get(0).savedAt);
    }

    private static Book book(
            String isbn,
            String title,
            String authors,
            String publisher,
            String category,
            String source,
            long savedAt) {
        return new Book(isbn, title, authors, publisher, "2024", category, source, "", 0, "", savedAt);
    }
}
