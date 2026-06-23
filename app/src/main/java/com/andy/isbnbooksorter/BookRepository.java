package com.andy.isbnbooksorter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

final class BookRepository extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "isbn_book_sorter.db";
    private static final int DATABASE_VERSION = 2;

    BookRepository(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE books ("
                        + "isbn TEXT PRIMARY KEY,"
                        + "title TEXT NOT NULL,"
                        + "authors TEXT NOT NULL,"
                        + "publisher TEXT NOT NULL,"
                        + "published_date TEXT NOT NULL,"
                        + "category TEXT NOT NULL,"
                        + "source TEXT NOT NULL,"
                        + "description TEXT NOT NULL DEFAULT '',"
                        + "page_count INTEGER NOT NULL DEFAULT 0,"
                        + "thumbnail_url TEXT NOT NULL DEFAULT '',"
                        + "saved_at INTEGER NOT NULL"
                        + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE books ADD COLUMN description TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN page_count INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE books ADD COLUMN thumbnail_url TEXT NOT NULL DEFAULT ''");
            return;
        }
    }

    void save(Book book) {
        long savedAt = book.savedAt > 0L ? book.savedAt : System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("isbn", book.isbn);
        values.put("title", book.title);
        values.put("authors", book.authors);
        values.put("publisher", book.publisher);
        values.put("published_date", book.publishedDate);
        values.put("category", book.category);
        values.put("source", book.source);
        values.put("description", book.description);
        values.put("page_count", book.pageCount);
        values.put("thumbnail_url", book.thumbnailUrl);
        values.put("saved_at", savedAt);
        getWritableDatabase().insertWithOnConflict("books", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    List<Book> listAll() {
        List<Book> books = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                "books",
                null,
                null,
                null,
                null,
                null,
                "category COLLATE LOCALIZED ASC, saved_at DESC");
        try {
            while (cursor.moveToNext()) {
                books.add(new Book(
                        cursor.getString(cursor.getColumnIndexOrThrow("isbn")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("authors")),
                        cursor.getString(cursor.getColumnIndexOrThrow("publisher")),
                        cursor.getString(cursor.getColumnIndexOrThrow("published_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        cursor.getString(cursor.getColumnIndexOrThrow("source")),
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("page_count")),
                        cursor.getString(cursor.getColumnIndexOrThrow("thumbnail_url")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("saved_at"))));
            }
        } finally {
            cursor.close();
        }
        return books;
    }
}
