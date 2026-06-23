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
    private static final int DATABASE_VERSION = 5;

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
                        + "book_size TEXT NOT NULL DEFAULT '',"
                        + "form TEXT NOT NULL DEFAULT '',"
                        + "form_detail TEXT NOT NULL DEFAULT '',"
                        + "series_title TEXT NOT NULL DEFAULT '',"
                        + "series_no TEXT NOT NULL DEFAULT '',"
                        + "related_isbn TEXT NOT NULL DEFAULT '',"
                        + "title_url TEXT NOT NULL DEFAULT '',"
                        + "ea_isbn TEXT NOT NULL DEFAULT '',"
                        + "ea_add_code TEXT NOT NULL DEFAULT '',"
                        + "input_date TEXT NOT NULL DEFAULT '',"
                        + "update_date TEXT NOT NULL DEFAULT '',"
                        + "bib_yn TEXT NOT NULL DEFAULT '',"
                        + "deposit_yn TEXT NOT NULL DEFAULT '',"
                        + "ebook_yn TEXT NOT NULL DEFAULT '',"
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
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE books ADD COLUMN book_size TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN form TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN form_detail TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN series_title TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN series_no TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN related_isbn TEXT NOT NULL DEFAULT ''");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE books ADD COLUMN title_url TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN ea_isbn TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN ea_add_code TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN input_date TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN update_date TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN bib_yn TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN deposit_yn TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE books ADD COLUMN ebook_yn TEXT NOT NULL DEFAULT ''");
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
        values.put("book_size", book.bookSize);
        values.put("form", book.form);
        values.put("form_detail", book.formDetail);
        values.put("series_title", book.seriesTitle);
        values.put("series_no", book.seriesNo);
        values.put("related_isbn", book.relatedIsbn);
        values.put("title_url", book.titleUrl);
        values.put("ea_isbn", book.eaIsbn);
        values.put("ea_add_code", book.eaAddCode);
        values.put("input_date", book.inputDate);
        values.put("update_date", book.updateDate);
        values.put("bib_yn", book.bibYn);
        values.put("deposit_yn", book.depositYn);
        values.put("ebook_yn", book.ebookYn);
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
                        cursor.getString(cursor.getColumnIndexOrThrow("book_size")),
                        cursor.getString(cursor.getColumnIndexOrThrow("form")),
                        cursor.getString(cursor.getColumnIndexOrThrow("form_detail")),
                        cursor.getString(cursor.getColumnIndexOrThrow("series_title")),
                        cursor.getString(cursor.getColumnIndexOrThrow("series_no")),
                        cursor.getString(cursor.getColumnIndexOrThrow("related_isbn")),
                        cursor.getString(cursor.getColumnIndexOrThrow("title_url")),
                        cursor.getString(cursor.getColumnIndexOrThrow("ea_isbn")),
                        cursor.getString(cursor.getColumnIndexOrThrow("ea_add_code")),
                        cursor.getString(cursor.getColumnIndexOrThrow("input_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("update_date")),
                        cursor.getString(cursor.getColumnIndexOrThrow("bib_yn")),
                        cursor.getString(cursor.getColumnIndexOrThrow("deposit_yn")),
                        cursor.getString(cursor.getColumnIndexOrThrow("ebook_yn")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("saved_at"))));
            }
        } finally {
            cursor.close();
        }
        return books;
    }
}
