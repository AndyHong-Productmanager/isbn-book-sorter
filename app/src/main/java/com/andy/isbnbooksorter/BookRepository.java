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
    private static final int DATABASE_VERSION = 3;

    BookRepository(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE books ("
                        + "isbn TEXT PRIMARY KEY,"
                        + "title TEXT NOT NULL,"
                        + "subtitle TEXT NOT NULL DEFAULT '',"
                        + "authors TEXT NOT NULL,"
                        + "translators TEXT NOT NULL DEFAULT '',"
                        + "publisher TEXT NOT NULL,"
                        + "published_date TEXT NOT NULL,"
                        + "category TEXT NOT NULL,"
                        + "source TEXT NOT NULL,"
                        + "description TEXT NOT NULL DEFAULT '',"
                        + "table_of_contents TEXT NOT NULL DEFAULT '',"
                        + "contents TEXT NOT NULL DEFAULT '',"
                        + "introduction TEXT NOT NULL DEFAULT '',"
                        + "page_count INTEGER NOT NULL DEFAULT 0,"
                        + "thumbnail_url TEXT NOT NULL DEFAULT '',"
                        + "language TEXT NOT NULL DEFAULT '',"
                        + "price TEXT NOT NULL DEFAULT '',"
                        + "saved_at INTEGER NOT NULL"
                        + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE books ADD COLUMN description TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN page_count INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE books ADD COLUMN thumbnail_url TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE books ADD COLUMN subtitle TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN translators TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN table_of_contents TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN contents TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN introduction TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN language TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN price TEXT NOT NULL DEFAULT ''");
        }
    }

    void save(Book book) {
        long savedAt = book.savedAt > 0L ? book.savedAt : System.currentTimeMillis();
        ContentValues values = new ContentValues();
        values.put("isbn", book.isbn);
        values.put("title", book.title);
        values.put("subtitle", book.subtitle);
        values.put("authors", book.authors);
        values.put("translators", book.translators);
        values.put("publisher", book.publisher);
        values.put("published_date", book.publishedDate);
        values.put("category", book.category);
        values.put("source", book.source);
        values.put("description", book.description);
        values.put("table_of_contents", book.tableOfContents);
        values.put("contents", book.contents);
        values.put("introduction", book.introduction);
        values.put("page_count", book.pageCount);
        values.put("thumbnail_url", book.thumbnailUrl);
        values.put("language", book.language);
        values.put("price", book.price);
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
                        cursor.getString(cursor.getColumnIndexOrThrow("subtitle")),
                        cursor.getString(cursor.getColumnIndexOrThrow("authors")),
                        cursor.getString(cursor.getColumnIndexOrThrow("translators")),
                        cursor.getString(cursor.getColumnIndexOrThrow("publisher")),
                        cursor.getString(cursor.getColumnIndexOrThrow("published_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        cursor.getString(cursor.getColumnIndexOrThrow("source")),
                        cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        cursor.getString(cursor.getColumnIndexOrThrow("table_of_contents")),
                        cursor.getString(cursor.getColumnIndexOrThrow("contents")),
                        cursor.getString(cursor.getColumnIndexOrThrow("introduction")),
                        cursor.getInt(cursor.getColumnIndexOrThrow("page_count")),
                        cursor.getString(cursor.getColumnIndexOrThrow("thumbnail_url")),
                        cursor.getString(cursor.getColumnIndexOrThrow("language")),
                        cursor.getString(cursor.getColumnIndexOrThrow("price")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("saved_at"))));
            }
        } finally {
            cursor.close();
        }
        return books;
    }
}
